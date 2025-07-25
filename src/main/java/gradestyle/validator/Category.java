package gradestyle.validator;

import com.github.javaparser.ParseResult;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import gradestyle.Repo;
import gradestyle.config.CategoryConfig;
import gradestyle.util.FileUtils;
import gradestyle.util.JavaParser;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public enum Category {
  Formatting,
  ClassNames,
  MethodNames,
  VariableNames,
  PackageNames,
  Commenting,
  JavadocClass,
  JavadocMethod,
  JavadocConstructor,
  JavadocField,
  Javadoc,
  PrivateInstances,
  Ordering,
  Useless,
  StringConcatenation,
  Clones,
  JavaFX,
  MissingOverride,
  FinalizeOverride,
  UnqualifiedStaticAccess,
  EmptyCatchBlock;

  public static Map<Category, Integer> getCategoryScores(
      ValidationResult result, List<CategoryConfig> configs) throws IOException {
    Map<Category, Integer> categoryScores = new HashMap<>();

    for (CategoryConfig config : configs) {
      int score;

      switch (config.getMode()) {
        case ABSOLUTE:
          score = getCategoryAbsoluteScore(result, config);
          break;
        case RELATIVE:
          score = getCategoryRelativeScore(result, config);
          break;
        default:
          throw new IllegalArgumentException("Unknown category config mode: " + config.getMode());
      }

      categoryScores.put(config.getCategory(), score);
    }

    return categoryScores;
  }

  private static int getCategoryAbsoluteScore(ValidationResult result, CategoryConfig config) {
    int count = config.getCategory().getViolationTotal(result.getViolations());
    return getScore(count, config.getScores());
  }

  private static int getCategoryRelativeScore(ValidationResult result, CategoryConfig config)
      throws IOException {
    int count = config.getCategory().getViolationTotal(result.getViolations());
    long normalisation = config.getCategory().getNormalisation(result.getRepo());
    float percentage = normalisation != 0 ? ((float) count / normalisation * 100) : 0;

    return getScore((int) percentage, config.getScores());
  }

  private static int getScore(int score, List<Integer> scores) {
    int index = Collections.binarySearch(scores, score);

    if (index < 0) {
      index = -index - 1;
    }

    return scores.size() - index;
  }

  private int getViolationTotal(Violations violations) {
    return getTypes().stream()
        .map(type -> violations.filterByType(type))
        .map(Violations::getViolations)
        .mapToInt(List::size)
        .sum();
  }

  private long getNormalisation(Repo repo) throws IOException {
    if (this == JavaFX) {
      return FileUtils.getFxmlFiles(repo.getDir()).count();
    }

    long normalisation = 0;

    com.github.javaparser.JavaParser parser = JavaParser.get(repo);

    for (Path file : FileUtils.getJavaSrcFiles(repo.getDir()).toList()) {
      ParseResult<CompilationUnit> result = parser.parse(file);

      if (!result.isSuccessful()) {
        throw new IOException();
      }

      CompilationUnit cu = result.getResult().get();

      switch (this) {
        case Formatting:
        case Commenting:
        case Useless:
          normalisation += cu.getRange().get().getLineCount();
          break;
        case ClassNames:
          normalisation += classCounter(cu);
          break;
        case MethodNames:
          normalisation += methodCounter(cu);
          break;
        case VariableNames:
          normalisation += variableCounter(cu);
          break;
        case PackageNames:
          normalisation += packageCounter(cu);
          break;
        case PrivateInstances:
          normalisation += instanceFieldCounter(cu);
          break;
        case Ordering:
          normalisation +=
              staticFieldCounter(cu)
                  + instanceFieldCounter(cu)
                  + constructorCounter(cu)
                  + methodCounter(cu);
          break;
        case JavadocClass:
          normalisation += cu.findAll(ClassOrInterfaceDeclaration.class).stream().count();
          break;

        case JavadocMethod:
          normalisation += cu.findAll(MethodDeclaration.class).stream().count();
          break;

        case JavadocField:
          normalisation += cu.findAll(FieldDeclaration.class).stream().count();
          break;

        case JavadocConstructor:
          normalisation += cu.findAll(ConstructorDeclaration.class).stream().count();
          break;

        case Javadoc:
          normalisation +=
              cu.getAllComments().stream()
                  .filter(Comment::isJavadocComment)
                  .map(Comment::getRange)
                  .map(Optional::get)
                  .mapToInt(Range::getLineCount)
                  .sum();
          break;

        case MissingOverride:
        case FinalizeOverride:
          normalisation += methodCounter(cu);
          break;

        case UnqualifiedStaticAccess:
          normalisation += staticMemberAccessCount(cu);

          break;
        case EmptyCatchBlock:
          normalisation += tryCatchCounter(cu);
          break;

        case StringConcatenation:
          normalisation += loopCounter(cu);
          break;

        default:
          throw new IllegalArgumentException("Unknown category: " + this);
      }
    }

    return normalisation;
  }

  private int classCounter(CompilationUnit cu) {
    return cu.findAll(ClassOrInterfaceDeclaration.class).size()
        + cu.findAll(EnumDeclaration.class).size();
  }

  private int methodCounter(CompilationUnit cu) {
    return cu.findAll(MethodDeclaration.class).size();
  }

  private int variableCounter(CompilationUnit cu) {
    return cu.findAll(VariableDeclarator.class).size() + cu.findAll(Parameter.class).size();
  }

  private int packageCounter(CompilationUnit cu) {
    return cu.findAll(PackageDeclaration.class).size();
  }

  private int instanceFieldCounter(CompilationUnit cu) {
    return cu.findAll(FieldDeclaration.class, Predicate.not(FieldDeclaration::isStatic)).size();
  }

  private int staticFieldCounter(CompilationUnit cu) {
    return cu.findAll(FieldDeclaration.class, FieldDeclaration::isStatic).size();
  }

  private int constructorCounter(CompilationUnit cu) {
    return cu.findAll(ConstructorDeclaration.class).size();
  }

  private int loopCounter(CompilationUnit cu) {
    return cu.findAll(com.github.javaparser.ast.stmt.ForStmt.class).size()
        + cu.findAll(com.github.javaparser.ast.stmt.WhileStmt.class).size()
        + cu.findAll(com.github.javaparser.ast.stmt.DoStmt.class).size()
        + cu.findAll(com.github.javaparser.ast.stmt.ForEachStmt.class).size();
  }

  public int tryCatchCounter(CompilationUnit cu) {
    return cu.findAll(CatchClause.class).size();
  }

  private long staticMemberAccessCount(CompilationUnit cu) {
    long staticMethodCount = 0;
    staticMethodCount =
        cu.findAll(MethodCallExpr.class).stream()
            .filter(
                methodCall -> {
                  try {
                    ResolvedMethodDeclaration resolvedMethod = methodCall.resolve();
                    return resolvedMethod.isStatic();
                  } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
                    return false;
                  } catch (RuntimeException e) {
                    return false;
                  }
                })
            .count();

    long staticFieldCount =
        cu.findAll(FieldAccessExpr.class).stream()
            .filter(
                fieldAccess -> {
                  try {
                    ResolvedValueDeclaration v = fieldAccess.resolve();

                    if (v.isField()) {
                      return v.asField().isStatic();
                    }

                    return false;

                  } catch (Exception e) {
                    return false;
                  }
                })
            .count();

    return staticFieldCount + staticMethodCount;
  }

  public List<Type> getTypes() {
    return Arrays.stream(Type.values()).filter(type -> type.getCategory() == this).toList();
  }

  @Override
  public String toString() {
    if (this == Javadoc) {
      return "Javadoc Formatting";
    }
    return name().replaceAll("(.)([A-Z])", "$1 $2");
  }
}
