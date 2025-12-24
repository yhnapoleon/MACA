package com.team06.maca

import java.util.UUID

data class DisplayImage(val path: String, val id: String = UUID.randomUUID().toString())
