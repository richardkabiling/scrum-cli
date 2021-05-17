package com.github.richardkabiling.scrum.cli

class SprintAlreadyPlannedException(message: String? = null, cause: Throwable? = null) :
    Throwable(message = message, cause = cause)

class SprintNotFoundException(message: String? = null, cause: Throwable? = null) :
    Throwable(message = message, cause = cause)