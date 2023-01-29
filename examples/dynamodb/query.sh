aws dynamodb query \
    --table-name locations \
    --key-condition-expression "business_id = :line and begins_with(ts_with_unique_id, :ts)" \
    --expression-attribute-values  '{":line":{"S":"17"},":ts":{"S":"2023012016"}}' \
    --no-scan-index-forward \
    --limit 15
