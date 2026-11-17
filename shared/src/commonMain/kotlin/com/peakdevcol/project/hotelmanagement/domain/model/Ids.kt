package com.peakdevcol.project.hotelmanagement.domain.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** A new random id for a document or for a line of an order. */
@OptIn(ExperimentalUuidApi::class)
fun newId(): String = Uuid.random().toString()
