from src.scripts.audit_domain_terms import DEFAULT_DATASET_DIR, build_audit


def test_domain_terms_cover_all_dataset_sub_categories():
    report = build_audit(DEFAULT_DATASET_DIR)

    missing = [row["sub_category"] for row in report["uncovered_sub_categories"]]

    assert missing == []


def test_domain_terms_audit_preserves_raw_food_category_as_source_alias():
    report = build_audit(DEFAULT_DATASET_DIR)

    food_mapping = next(row for row in report["category_mappings"] if row["raw_category"] == "食品饮料")

    assert food_mapping["canonical_category"] == "食品生活"
    assert report["product_count"] == 100
    assert report["brand_count"] > 0
    assert any(row["key"] == "口味" for row in report["sku_property_keys"])
