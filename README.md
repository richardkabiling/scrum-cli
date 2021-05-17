# scrum-cli

**Scrum** is a tool that organizes scraped data into a directory structure to generate sprint statistics.

# Features
* plan - declare new sprint and register information including roles, members, schedule and capacity
* stories
    * baseline stories - register starting stories of a sprint
    * put stories - register story information for today (or a specified day)
* tasks
    * baseline tasks - register starting tasks of a sprint
    * put tasks - register task information for today (or a specified day)
* stats - generate stats based on current sprint
    * task progress
    * task progress by task type
    * points statistics
    * story completion progress
    * story estimate progress
    * capacity utilization
    * etc
* namespaces - all commands can receive the option `-n, --namespace` that allows namespacing sprints. Default namespace is `default`

# Notes on Stories and Tasks
* Stories
    * Top level items
    * Optionally estimated in points and earned on completion
    * No restriction on types
* Tasks
    * Under stories
    * Estimated in hours and counts against capacity
    * Types must correspond to registered member roles during planning
* Stories and tasks must have status "To Do", "In Progress" and "Done"

# Recommended Workflow
* During planning, `plan` sprint and `baseline` stories and tasks
* At least daily, `put` stories and tasks information-- possibly via CI
* At least daily, generate `stats` -- possibly via CI
* Output stats file can be used to template a report

# Assembly
```
$ ./gradlew clean assemble
```

# Usage
```
$ java - jar build/libs/scrum.jar --help
```