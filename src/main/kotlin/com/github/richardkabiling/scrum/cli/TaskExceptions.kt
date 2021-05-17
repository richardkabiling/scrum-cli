package com.github.richardkabiling.scrum.cli

class TasksFileReadException(message: String? = null, cause: Throwable? = null) :
    Throwable(message = message, cause = cause)

class TasksAlreadyExistsException(message: String? = null, cause: Throwable? = null) :
    Throwable(message = message, cause = cause)