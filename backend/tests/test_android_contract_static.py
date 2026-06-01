from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[2]


def test_android_convergence_uses_backend_stream_not_local_fallback():
    view_model_path = (
        PROJECT_ROOT
        / "android"
        / "feature"
        / "chat"
        / "src"
        / "main"
        / "java"
        / "com"
        / "buypilot"
        / "feature"
        / "chat"
        / "ChatViewModel.kt"
    )
    source = view_model_path.read_text(encoding="utf-8")
    converge_body = source[
        source.index("fun convergeProductDeck") : source.index("private fun addConvergenceUserMessage")
    ]

    # convergeProductDeck delegates to continueConvergingProductDeck which calls startRealStream
    assert "continueConvergingProductDeck(" in converge_body
    assert "startRealStream(" in converge_body
    assert "userMessage.ifBlank" in converge_body
    # Fallback convergence must not be called in the real convergence path
    assert "sendFallbackConvergence(deckId, userMessage)" not in converge_body
    assert "ChatReducer.convergeDeck" in converge_body
    assert "deckId = deckId" in source


def test_android_product_feedback_sends_deck_id():
    request_path = (
        PROJECT_ROOT
        / "android"
        / "core"
        / "model"
        / "src"
        / "main"
        / "java"
        / "com"
        / "buypilot"
        / "core"
        / "model"
        / "requests"
        / "ChatRequests.kt"
    )
    repository_path = (
        PROJECT_ROOT
        / "android"
        / "core"
        / "data"
        / "src"
        / "main"
        / "java"
        / "com"
        / "buypilot"
        / "core"
        / "data"
        / "ChatRepository.kt"
    )

    assert '@SerialName("deck_id") val deckId' in request_path.read_text(encoding="utf-8")
    assert "deckId = deckId" in repository_path.read_text(encoding="utf-8")


def test_android_gradle_config_does_not_pin_machine_local_paths():
    gradle_properties = (PROJECT_ROOT / "android" / "gradle.properties").read_text(encoding="utf-8")
    gradlew = (PROJECT_ROOT / "android" / "gradlew").read_text(encoding="utf-8")

    assert "org.gradle.java.home" not in gradle_properties
    assert "/mnt/disk1/" not in gradle_properties
    assert "/mnt/disk1/" not in gradlew
