// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-License-Identifier: LGPL-2.1

package dev.kokoroidkt.httpDriver.rule

import dev.kokoroidkt.httpDriver.http.HttpRequest

typealias ServerRule = suspend (HttpRequest) -> Boolean
