package com.github.richardkabiling.scrum.cli

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import javax.inject.Singleton

@Factory
class ObjectMapperFactory {

    @Replaces(ObjectMapper::class)
    @Singleton
    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper(YAMLFactory())
            .registerModule(JavaTimeModule())
            .registerKotlinModule()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
}