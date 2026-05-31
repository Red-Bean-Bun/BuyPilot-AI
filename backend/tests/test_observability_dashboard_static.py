from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[2]


def test_observability_dashboard_request_list_uses_dom_text_nodes():
    source = (PROJECT_ROOT / "backend" / "static" / "observability_dashboard.html").read_text(
        encoding="utf-8"
    )

    assert "function escapeHtml(value)" in source
    assert "item.addEventListener('click', () => loadTurn(turnId));" in source
    assert "path.textContent" in source
    assert "id.textContent = turnId" in source
    assert "onclick=\"loadTurn('${turnId}')\"" not in source
    assert 'data-turn="${turnId}"' not in source
    assert "list.innerHTML = reqs.map" not in source
