package com.louiscad.playground.compose.videogen.core

import kotlin.test.assertEquals

infix fun Any?.shouldBe(expected: Any?) = assertEquals(expected = expected, actual = this)
