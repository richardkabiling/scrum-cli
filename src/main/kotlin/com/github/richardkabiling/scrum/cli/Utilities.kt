package com.github.richardkabiling.scrum.cli

import java.math.BigDecimal
import java.time.LocalDate

const val NON_TRAILING_NON_WORD_CHARACTERS = "[^\\w\\-]+(?![^\\w\\-]*\$)"
const val TRAILING_NON_WORD_CHARACTERS = "[^\\w\\-]+(?=\$)"

fun String.sanitized(): String {
    return this.toLowerCase()
        .replace(NON_TRAILING_NON_WORD_CHARACTERS.toRegex(), "-")
        .replace(TRAILING_NON_WORD_CHARACTERS.toRegex(), "")
}

infix fun Map<LocalDate, BigDecimal>.addKeyWise(that: Map<LocalDate, BigDecimal>) =
    keys.associateWith {
        (this[it] ?: BigDecimal.ZERO) + (that[it] ?: BigDecimal.ZERO)
    }

fun List<Task>.filterSum(
    predicate: (Task) -> Boolean = { true },
    selector: (Task) -> BigDecimal
) = filter(predicate)
    .map(selector)
    .fold(BigDecimal.ZERO, BigDecimal::plus)

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

infix fun <A, B, C> Pair<A, B>.to(third: C): Triple<A, B, C> = Triple(first, second, third)
infix fun <A, B, C, D> Triple<A, B, C>.to(fourth: D): Quadruple<A, B, C, D> = Quadruple(first, second, third, fourth)
