from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[2]


def test_observability_dashboard_request_list_uses_dom_text_nodes():
    source = (PROJECT_ROOT / "backend" / "static" / "observability_dashboard.html").read_text(encoding="utf-8")

    assert "function escapeHtml(value)" in source
    assert "if(targetKind==='turn')item.addEventListener('click', () => loadTurn(targetId));" in source
    assert "else if(targetKind==='request')item.addEventListener('click', () => loadRequest(targetId));" in source
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

    assert "e.product_ids.map(productButton)" in source
    assert "e.product_ids.map(esc)" not in source
    assert 'onclick="togglePanel(this)"><span class="collapse-arrow">' in source
    assert "togglePanel(this.parentElement)" not in source


def test_observability_dashboard_supports_all_endpoint_filters_and_new_tabs():
    source = (PROJECT_ROOT / "backend" / "static" / "observability_dashboard.html").read_text(encoding="utf-8")

    assert 'id="kindFilter"' in source
    assert '<option value="">全部接口</option>' in source
    assert '<option value="upload">Upload</option>' in source
    assert '<option value="feedback">Feedback</option>' in source
    assert '<option value="cart">Cart</option>' in source
    assert '<option value="product">Product</option>' in source
    assert '<option value="cancel">Cancel</option>' in source
    assert 'data-tab="trace"' in source
    assert 'data-tab="evidence"' in source
    assert 'data-tab="eval"' in source
    assert "async function loadRequest(id)" in source
    assert "async function loadProductDetail(id)" in source
    assert "function renderTrace(b)" in source
    assert "function renderEvidence(b)" in source
    assert "function renderEval()" in source


def test_observability_dashboard_tabs_are_contextual():
    source = (PROJECT_ROOT / "backend" / "static" / "observability_dashboard.html").read_text(encoding="utf-8")

    assert "function visibleTabsForBundle(b)" in source
    assert "function bundleKind(b)" in source
    assert "function applyTabVisibility()" in source
    assert "if(kind==='chat'||kind==='session')" in source
    assert "if(kind==='feedback'||kind==='upload'||kind==='cancel'||kind==='cart')" in source
    assert "return auditCount?['overview','audit']:['overview'];" in source
    assert "if(t.style.display==='none')return;" in source
    assert "const visibleTabs=new Set(visibleTabsForBundle(b));" in source
    assert "statCard('LLM',llmC)" in source
    assert "applyTabVisibility();" in source


def test_observability_dashboard_tabs_show_signal_summaries():
    source = (PROJECT_ROOT / "backend" / "static" / "observability_dashboard.html").read_text(encoding="utf-8")

    assert "function updateTabSummaries()" in source
    assert "function tabInfo(tabId,b)" in source
    assert "function shortPath(path)" in source
    assert "function kindLabel(kind)" in source
    assert "failed?failed+' failed'" in source
    assert "fallback?fallback+' fallback'" in source
    assert "done?'done #'+lastSeq" in source
    assert "hits+' hits · '+selected.size+' selected'" in source
    assert "products.size+' products'" in source
    assert "effects+' sidefx · '+last" in source
    assert "samples.length+' samples'" in source
    assert "updateTabSummaries();" in source


def test_observability_dashboard_tab_pages_have_summary_sections():
    source = (PROJECT_ROOT / "backend" / "static" / "observability_dashboard.html").read_text(encoding="utf-8")

    assert "function countBy(items,fn)" in source
    assert "任务分布" in source
    assert "事件分布" in source
    assert "最终选中商品" in source
    assert "商品证据覆盖" in source
    assert "诊断代码分布" in source
    assert "Action 分布" in source
    assert "Missing seq:" in source
    assert "Slow >=3s" in source
    assert "Vector Candidates" in source
