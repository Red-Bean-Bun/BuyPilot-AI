package com.buypilot.core.common.sse

class EventDeduplicator {
    private val seenEventIds = linkedSetOf<String>()
    private val seenNodes = mutableSetOf<String>()
    private val seenDecks = mutableSetOf<String>()

    fun markEvent(eventId: String): Boolean = seenEventIds.add(eventId)

    fun isKnownNode(nodeId: String): Boolean = !seenNodes.add(nodeId)

    fun isKnownDeck(deckId: String): Boolean = !seenDecks.add(deckId)

    fun clear() {
        seenEventIds.clear()
        seenNodes.clear()
        seenDecks.clear()
    }
}
