from app.categorize import build_prompt, parse_response

CATEGORIES = ["ALIMENTATION", "TRANSPORT", "AUTRES"]
TXS = [{"id": 1, "label": "CB SARL DUPONT", "amount": -12.5},
       {"id": 2, "label": "CB TAXI G7", "amount": -30.0}]


def test_prompt_lists_categories_and_operations():
    prompt = build_prompt(TXS, CATEGORIES)
    assert "ALIMENTATION, TRANSPORT, AUTRES" in prompt
    assert '"label": "CB TAXI G7"' in prompt


def test_valid_model_output_is_kept():
    raw = {"results": [{"id": 1, "category": "alimentation"}, {"id": 2, "category": "TRANSPORT"}]}
    assert parse_response(raw, TXS, CATEGORIES) == [{"id": 1, "category": "ALIMENTATION"},
                                                    {"id": 2, "category": "TRANSPORT"}]


def test_unknown_categories_and_missing_ids_fall_back_to_autres():
    raw = {"results": [{"id": 1, "category": "CRYPTO"}, {"id": "abc", "category": "TRANSPORT"}]}
    assert parse_response(raw, TXS, CATEGORIES) == [{"id": 1, "category": "AUTRES"},
                                                    {"id": 2, "category": "AUTRES"}]


def test_malformed_output_never_breaks_the_import():
    assert parse_response("n'importe quoi", TXS, CATEGORIES) == [{"id": 1, "category": "AUTRES"},
                                                                 {"id": 2, "category": "AUTRES"}]
