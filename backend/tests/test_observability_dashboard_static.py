from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[2]


def test_observability_dashboard_request_list_uses_dom_text_nodes():
    source = (PROJECT_ROOT / "backend" / "static" / "observability_dashboard.html").read_text(encoding="utf-8")

    assert "function escapeHtml(value)" in source
    assert "if(targetKind==='turn')item.addEventListener('click', () => loadTurn(targetId));" in source
    assert "else if(targetKind==='session')item.addEventListener('click', () => loadSession(targetId));" in source
    assert "item.dataset.key=targetId" in source
    assert "el.dataset.key===id" in source
    assert "path.textContent" in source
    assert "id.textContent = targetKind ? targetKind+': '+displayId : displayId" in source
    assert "onclick=\"loadTurn('${turnId}')\"" not in source
    assert 'data-turn="${turnId}"' not in source
    assert "list.innerHTML = reqs.map" not in source


def test_observability_dashboard_forwards_admin_token_to_api_requests():
    source = (PROJECT_ROOT / "backend" / "static" / "observability_dashboard.html").read_text(encoding="utf-8")

    assert "const adminToken = new URLSearchParams(window.location.search).get('token') || '';" in source
    assert "Authorization:'Bearer '+adminToken" in source
    assert "const r=await fetch(url);" not in source


def test_observability_dashboard_render_helpers_are_defined():
    source = (PROJECT_ROOT / "backend" / "static" / "observability_dashboard.html").read_text(encoding="utf-8")

    assert "e.product_ids.map(escapeHtml)" in source
    assert "e.product_ids.map(esc)" not in source
    assert 'onclick="togglePanel(this)"><span class="collapse-arrow">' in source
    assert "togglePanel(this.parentElement)" not in source
