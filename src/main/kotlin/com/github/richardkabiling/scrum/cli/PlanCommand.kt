package com.github.richardkabiling.scrum.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import picocli.CommandLine.*
import java.io.File
import java.math.BigDecimal
import java.nio.file.Paths
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private val logger = KotlinLogging.logger { }

private val ABSOLUTE_HOURS_MAX: BigDecimal = 24.toBigDecimal()
private val ABSOLUTE_HOURS_MIN: BigDecimal = 0.toBigDecimal()

@Command(
    name = "plan",
    mixinStandardHelpOptions = true
)
class PlanCommand : Runnable {

    @Option(
        names = ["-f", "--force"],
        description = ["overrides existing plan"],
        defaultValue = "false",
    )
    var force: Boolean = false

    @Parameters(
        index = "0",
        description = ["plan file in yml format"]
    )
    lateinit var planFile: File

    @ParentCommand
    lateinit var parent: ScrumCommand

    @Inject
    lateinit var mapper: ObjectMapper

    override fun run() {
        if (!planFile.isFile)
            throw PlanFileReadException("Plan file is not file")
        if (!planFile.exists())
            throw PlanFileReadException("Plan file does not exist")

        val sprintDetails: SprintDetails = mapper.readValue(planFile)
        val sprint = sprintDetails.run {
            val ms = members.map { (k, v) -> Sprint.Member(name = k, role = v) }

            Sprint(
                namespace = parent.namespace,
                project = project,
                name = name,
                members = ms,
                schedule = schedule.toSchedule(),
                effectiveCapacity = capacity.toCapacity(ms.map { it.name }, schedule.dates)
            )
        }

        val sprintFile = Paths.get(".", parent.namespace, "sprint-${sprint.name.sanitized()}.yml")
            .toAbsolutePath()
            .toFile()
        if (sprintFile.exists() && !force)
            throw SprintAlreadyPlannedException()

        FileUtils.forceMkdirParent(sprintFile)
        mapper.writeValue(sprintFile, sprint)

        val indexFile = Paths.get(".", parent.namespace, "sprints.yml")
            .toAbsolutePath()
            .toFile()
        if (!indexFile.exists())
            mapper.writeValue(indexFile, LinkedHashSet<String>())

        val index: LinkedHashSet<String> = mapper.readValue(indexFile)
        mapper.writeValue(indexFile, index + sprint.name)
    }

    private fun SprintDetails.Schedule.toSchedule() = Sprint.Schedule(this.dates, this.offset)

    private fun SprintDetails.Capacity.toCapacity(
        xs: List<String>,
        ys: List<LocalDate>,
    ): Sprint.EffectiveCapacity {
        val base = base.filled(xs, ys)
        val time = time.filled(xs, ys)
        val manpower = manpower.filled(xs, ys)
        val timeOff = training.filled(xs, ys)
        val overhead = overhead.filled(xs, ys)
        val training = training.filled(xs, ys)
        val overtime = overtime.filled(xs, ys)
        val efficiency = efficiency.filled(xs, ys)

        val detailed = xs.associateWith { x ->
            ys.associateWith { y ->
                val b = base[x]!![y]!!
                val m = manpower[x]!![y]!!.coerceAtLeast(BigDecimal.ZERO)
                val t = time[x]!![y]!!.coerceAtLeast(BigDecimal.ZERO)
                val ec = (b * m * t).coerceIn(ABSOLUTE_HOURS_MIN, ABSOLUTE_HOURS_MAX)
                val to = timeOff[x]!![y]!!.coerceIn(ABSOLUTE_HOURS_MIN, ec)
                val oh = overhead[x]!![y]!!.coerceIn(ABSOLUTE_HOURS_MIN, ec)
                val tr = training[x]!![y]!!.coerceIn(ABSOLUTE_HOURS_MIN, ec)
                val ot = overtime[x]!![y]!!.coerceIn(ABSOLUTE_HOURS_MIN, ABSOLUTE_HOURS_MAX - ec)
                val e = efficiency[x]!![y]!!.coerceAtLeast(BigDecimal.ZERO)

                ((ec - to - oh - tr + ot) * e)
                    .coerceIn(ABSOLUTE_HOURS_MIN, ABSOLUTE_HOURS_MAX)
            }
        }

        return Sprint.EffectiveCapacity(
            base = base,
            time = time,
            manpower = manpower,
            timeOff = timeOff,
            overhead = overhead,
            training = training,
            overtime = overtime,
            efficiency = efficiency,
            detailed = detailed
        )
    }

    private fun <T : Number> SprintDetails.CapacityComponent<T>.filled(
        xs: List<String>,
        ys: List<LocalDate>
    ): Map<String, Map<LocalDate, T>> {
        return xs.associateWith { x ->
            ys.mapIndexed { i, y ->
                val value = detailed?.get(x)?.get(i)
                    ?: member?.get(x)
                    ?: daily?.get(i)
                    ?: default
                y to value
            }.toMap()
        }
    }

    class PlanFileReadException(message: String? = null, cause: Throwable? = null) :
        Throwable(message = message, cause = cause)

    class PlanFileInvalidException(message: String? = null, cause: Throwable? = null) :
        Throwable(message = message, cause = cause)

    data class SprintDetails(
        val name: String,
        val project: String,
        val members: Map<String, String>,
        val schedule: Schedule,
        val capacity: Capacity,
    ) {
        data class Schedule(
            val start: LocalDate,
            val end: LocalDate,
            val offset: ZoneOffset
        ) {
            val dates: List<LocalDate> = Period.between(start, end.plus(1, ChronoUnit.DAYS))
                .run {
                    List(days) { start + Period.ofDays(it) }
                        .filter { it.dayOfWeek < DayOfWeek.SATURDAY }
                }

            val durationInDays: Int = dates.size
        }

        data class Capacity(
            val base: CapacityComponent<BigDecimal>,
            val efficiency: CapacityComponent<BigDecimal>,
            val time: CapacityComponent<BigDecimal>,
            val manpower: CapacityComponent<BigDecimal>,
            val timeOff: CapacityComponent<BigDecimal>,
            val overhead: CapacityComponent<BigDecimal>,
            val training: CapacityComponent<BigDecimal>,
            val overtime: CapacityComponent<BigDecimal>
        )

        data class CapacityComponent<T : Number>(
            val default: T,
            val member: Map<String, T>?,
            val daily: Map<Int, T>?,
            val detailed: Map<String, Map<Int, T>>?
        )
    }

}
