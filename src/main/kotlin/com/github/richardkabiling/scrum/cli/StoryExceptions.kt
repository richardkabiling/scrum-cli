package com.github.richardkabiling.scrum.cli

class StoriesFileReadException(message: String? = null, cause: Throwable? = null) :
    Throwable(message = message, cause = cause)

class StoriesAlreadyExistsException : Throwable()