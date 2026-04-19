package dev.kokoroidkt.httpDriver.rule

import dev.kokoroidkt.httpDriver.http.HttpRequest

typealias ServerRule = suspend (HttpRequest) -> Boolean
