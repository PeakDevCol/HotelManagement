package com.peakdevcol.project.hotelmanagement.domain

import com.peakdevcol.project.hotelmanagement.domain.model.DestinationDraft
import com.peakdevcol.project.hotelmanagement.domain.model.DestinationKind
import com.peakdevcol.project.hotelmanagement.domain.model.OrderDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** RF-ORD-03: a room or a table with its number, the pool or the bar with an optional one, or "Otro". */
class DestinationDraftTest {
    @Test
    fun aRoomAndATableNeedTheirNumber() {
        assertNull(DestinationDraft(DestinationKind.ROOM).destination)
        assertNull(DestinationDraft(DestinationKind.TABLE, "0").destination)
        assertEquals(OrderDestination.Room(204), DestinationDraft(DestinationKind.ROOM, "204").destination)
        assertEquals(OrderDestination.Table(4), DestinationDraft(DestinationKind.TABLE, "4").destination)
    }

    @Test
    fun thePoolAndTheBarTakeAnOptionalNumberButNeverDropAWrongOne() {
        assertEquals(OrderDestination.Pool(), DestinationDraft(DestinationKind.POOL).destination)
        assertEquals(OrderDestination.Bar(7), DestinationDraft(DestinationKind.BAR, "7").destination)
        assertNull(DestinationDraft(DestinationKind.POOL, "0").destination)
    }

    @Test
    fun otherHasNoNumberAndNothingIsChosenWithoutAKind() {
        assertEquals(OrderDestination.Other, DestinationDraft(DestinationKind.OTHER, "12").destination)
        assertNull(DestinationDraft().destination)
    }

    @Test
    fun startsFromTheDestinationAlreadyChosen() {
        assertEquals(DestinationDraft(DestinationKind.ROOM, "1204"), DestinationDraft.of(OrderDestination.Room(1204)))
        assertEquals(DestinationDraft(DestinationKind.POOL, ""), DestinationDraft.of(OrderDestination.Pool()))
        assertEquals(DestinationDraft(), DestinationDraft.of(null))
    }
}
