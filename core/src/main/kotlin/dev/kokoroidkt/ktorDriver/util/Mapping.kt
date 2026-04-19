package dev.kokoroidkt.ktorDriver.util

import dev.kokoroidkt.httpDriver.http.HttpRequest
import io.ktor.server.request.*
import io.ktor.util.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import dev.kokoroidkt.httpDriver.http.HttpMethod as kokoroidHttpMethod
import io.ktor.http.HttpMethod as KtorHttpMethod

fun kokoroidHttpMethod.toKtorHttpMethod() =
    when (this) {
        kokoroidHttpMethod.GET -> KtorHttpMethod.Get
        kokoroidHttpMethod.POST -> KtorHttpMethod.Post
        kokoroidHttpMethod.PUT -> KtorHttpMethod.Put
        kokoroidHttpMethod.DELETE -> KtorHttpMethod.Delete
        kokoroidHttpMethod.PATCH -> KtorHttpMethod.Patch
        kokoroidHttpMethod.HEAD -> KtorHttpMethod.Head
        kokoroidHttpMethod.OPTIONS -> KtorHttpMethod.Options
        kokoroidHttpMethod.TRACE -> KtorHttpMethod.Trace
    }

fun KtorHttpMethod.toKokoroidHttpMethod() =
    when (this) {
        KtorHttpMethod.Get -> {
            kokoroidHttpMethod.GET
        }

        KtorHttpMethod.Post -> {
            kokoroidHttpMethod.POST
        }

        KtorHttpMethod.Put -> {
            kokoroidHttpMethod.PUT
        }

        KtorHttpMethod.Delete -> {
            kokoroidHttpMethod.DELETE
        }

        KtorHttpMethod.Patch -> {
            kokoroidHttpMethod.PATCH
        }

        KtorHttpMethod.Head -> {
            kokoroidHttpMethod.HEAD
        }

        KtorHttpMethod.Options -> {
            kokoroidHttpMethod.OPTIONS
        }

        KtorHttpMethod.Trace -> {
            kokoroidHttpMethod.TRACE
        }

        else -> {
            throw IllegalArgumentException("Unsupported HTTP method: $this")
        }
    }

suspend fun ApplicationRequest.toKokoroidRequest() =
    HttpRequest(
        method = this.httpMethod.toKokoroidHttpMethod(),
        path = this.path(),
        params = this.queryParameters.toMap(),
        headers = this.headers.toMap(),
        json =
            try {
                Json.parseToJsonElement(this.call.receiveText())
            } catch (_: SerializationException) {
                null
            },
        bodyText = this.call.receiveText(),
    )
