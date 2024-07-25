package org.goal2be.standard.validator.plugin

import io.ktor.server.application.*
import io.ktor.util.pipeline.*

object RequestValidationHook : Hook<suspend (ApplicationCall) -> Unit> {

    private val RequestValidationPhase = PipelinePhase("RequestValidation")
    override fun install(pipeline: ApplicationCallPipeline, handler: suspend (ApplicationCall) -> Unit) {
        pipeline.insertPhaseAfter(ApplicationCallPipeline.Plugins, RequestValidationPhase)
        pipeline.intercept(RequestValidationPhase) { handler(call) }
    }
}
