# GradeStyle

## Cite

If you use GradeStyle in your research, please cite the following paper:

```bibtex
@inproceedings{iddon-icse-2023,
  author = {Iddon, Callum and Giacaman, Nasser and Terragni, Valerio},
  title = {{GradeStyle: GitHub-Integrated and Automated Assessment of Java Code Style}},
  booktitle = {{IEEE/ACM} International Conference on Software Engineering, SEET track},
  year = {2023},
  doi = {10.1109/ICSE-SEET58685.2023.00024},
  pages = {192--197}
}
```

You can find a copy of the paper in the [publications](publications) directory.

## Build

```bash
> ./gradlew build
```

## Run

```bash
> java -jar build/libs/GradeStyle-all.jar <properties-file>
```

## Run Existing Jar
There are 3 jars in the /jars folder, each for a different purpose. you can run any of these jars if you are interested in replication or ease of use. See the README file in the /jars folder for more info.

## Configuration

```properties
# The directory where repos are stored.
# If $github is true, repos will be cloned into this directory,
# otherwise each subdirectory will be considered a repo.
# Required: Yes.
repos=/path/to/repos

# The common package name accross all repos.
# Required: Yes.
package=com.example.package

# The name of the template repository.
# Required: No, Default: Last directory name in $repos.
template=template-repo

# If a style violation should be ignored if
# it is also found in the template repo.
# Required: No, Default: false.
template.ignoreViolations=true

# The output CSV report file.
# Required: No.
reports.csv=/path/to/report.csv

# The output markdown report directory.
# Required: No.
reports.md=/path/to/md

# The title of the feedback report.
# Required: No.
feedback.title=Feedback Report

# The message on the feedback report.
# Required: No.
feedback.message=Code style report

# The message to show if a report could not be generated.
# Required: No.
feedback.error=It broke!

# If GitHub should be used to get repos.
# Required: No, Default: false.
github=true

# The GitHub personal access token to use.
# Required: If $github is true.
github.token=ghp_...

# The GitHub organisation of the GitHub Classroom.
# Required: If $github is true, OR if markdown reports should have links.
github.classroom=My-GitHub-Organisation

# The GitHub Classroom assignment repo prefix.
# Required: If $github is true.
github.assignment=my-github-classroom-assignment

# If feedback should be provided on the GitHub Classroom feedback pull request.
# Required: If $github is true, Default: false.
github.feedback=true

# Style category configuration.
# Valid <category>: Formatting, ClassNames, MethodNames, VariableNames,
#                   PackageNames, Commenting, JavaDoc, PrivateMembers,
#                   Ordering, Useless, StringConcatenation, Clones, JavaFX,
#                   EarlyReturn.

# If scoring for <category> is enabled.
# Required: No, Default: false.
<category>=true

# How the comparison score is calculated.
# Valid values: ABSOLUTE, RELATIVE.
# Required: If ${<category>} is true.
<category>.mode=RELATIVE

# How scores are calculated from the comparison score.
# The maximum score is the length of the list.
# Required: If ${<category>} is true.
<category>.scores=10,15,25

# The maximum number of examples to be provided in feedback.
# Required: No, Default: Integer.MAX_VALUE.
<category>.examples=3

# Extra commenting category configuration.

# Minimum number of lines required in method to check.
# Required: If Commenting is true.
Commenting.minLines=5

# Minimum percentage of commented lines required in each method.
# Required: If Commenting is true.
Commenting.minFrequency=10

# Maximum percentage of commented lines allowed in each method.
# Required: If Commenting is true.
Commenting.maxFrequency=50

# Maximum allowed levenshtein distance of comments.
# Required: If Commenting is true.
Commenting.levenshteinDistance=10

# Minimum number of words in the JavaDoc description.
# Required: If JavaDoc is true.
JavaDoc.minWords=10

# Number of tokens required for duplicated code.
# Required: If Clones is true.
Clones.tokens=100
```
