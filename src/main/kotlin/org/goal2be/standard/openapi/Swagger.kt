@file:Suppress("UNUSED")
package org.goal2be.standard.openapi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import java.io.File
import java.io.FileNotFoundException


class SwaggerConfig {
    var version: String = "5.17.14"
    var packageLocation: String = "https://unpkg.com/swagger-ui-dist"
    var hostPrefix: String = ""

    private var apiContent: String = ""
    val content get() = apiContent
    private var fileName: String = ""
    val apiUrl get() = fileName

    fun file(path: String) {
        val apiFile = File(path)
        if (!apiFile.exists()) {
            throw FileNotFoundException("Swagger file not found: ${apiFile.absolutePath}")
        }
        apiContent = apiFile.readText()
        fileName = apiFile.name
    }

}

fun Application.swaggerUI(
    path: String,
    file: String,
    block: (SwaggerConfig.() -> Unit)? = {}
) {
    val config = SwaggerConfig().apply {
        this.file(file)
        block?.invoke(this)
    }

    routing {
        route(path) {
            get(config.apiUrl) {
                call.respondText(config.content, ContentType.fromFilePath(config.apiUrl).firstOrNull())
            }
            get {
                call.respondHtml {
                    head {
                        title { +"Swagger UI" }
                        link(
                            href = "${config.packageLocation}@${config.version}/swagger-ui.css",
                            rel = "stylesheet"
                        )
                    }
                    body {
                        div { id = "swagger-ui" }
                        script(src = "${config.packageLocation}@${config.version}/swagger-ui-bundle.js") {
                            attributes["crossorigin"] = "anonymous"
                        }

                        val src = "${config.packageLocation}@${config.version}/swagger-ui-standalone-preset.js"
                        script(src = src) {
                            attributes["crossorigin"] = "anonymous"
                        }

                        script {
                            unsafe {
                                +"""
window.onload = function() {
    window.ui = SwaggerUIBundle({
        url: '${config.hostPrefix}${call.request.path()}/${config.apiUrl}',
        dom_id: '#swagger-ui',
        presets: [
            SwaggerUIBundle.presets.apis,
            SwaggerUIBundle.SwaggerUIStandalonePreset
        ],
        layout: 'BaseLayout',
        deepLinking: true,
        showExtensions: true,
        showCommonExtensions: true
    });
}
                            """.trimIndent()
                            }
                        }
                    }
                }
            }
        }
    }
}
