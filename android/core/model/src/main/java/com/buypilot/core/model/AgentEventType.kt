package com.buypilot.core.model

enum class AgentEventType(val wireValue: String) {
    Thinking("thinking"),
    Clarification("clarification"),
    CriteriaCard("criteria_card"),
    TextDelta("text_delta"),
    ProductCard("product_card"),
    CartAction("cart_action"),
    FinalDecision("final_decision"),
    Done("done"),
    Error("error"),
    Unknown("unknown");

    companion object {
        fun fromWireValue(value: String): AgentEventType =
            entries.firstOrNull { it.wireValue == value } ?: Unknown
    }
}
