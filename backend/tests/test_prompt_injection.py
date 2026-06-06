"""Tests for _sanitize_user_message input sanitization (P0-2)."""

from __future__ import annotations

from src.services.llm_task_payloads import _sanitize_user_message, intent_messages


class TestSanitizeUserMessage:
    def test_strips_null_bytes(self):
        assert _sanitize_user_message("hello\x00world") == "helloworld"

    def test_strips_control_characters(self):
        msg = "test\x01\x02\x08\x0b\x0c\x0e\x1ftext"
        assert _sanitize_user_message(msg) == "testtext"

    def test_preserves_normal_text(self):
        original = "推荐适合油皮的洗面奶，200元以内"
        assert _sanitize_user_message(original) == original

    def test_preserves_tabs_newlines(self):
        original = "line1\nline2\tindented"
        assert _sanitize_user_message(original) == original

    def test_does_not_truncate(self):
        # Truncation is Pydantic's job (max_length=2000 on ChatStreamRequest).
        # VL analysis appends to the message, so it can legitimately exceed 2000 chars.
        msg = "x" * 2500
        result = _sanitize_user_message(msg)
        assert len(result) == 2500

    def test_empty_string_passes_through(self):
        assert _sanitize_user_message("") == ""


class TestIntentMessagesSanitization:
    def test_sanitized_message_appears_in_system_prompt(self):
        messages = intent_messages(
            message="hello\x00world",
            history=None,
            image_url=None,
        )
        system_content = messages[0]["content"]
        user_content = messages[1]["content"]
        assert "\x00" not in system_content
        assert "helloworld" in system_content
        assert "\x00" not in user_content
        assert "helloworld" in user_content
