SELECT
    COUNT(1) AS RECORDS_CNT,
    AT_TIMEZONE(MIN(FETCH_TS), 'EUROPE/WARSAW') AS FIRST_BATCH_TS,
    AT_TIMEZONE(MAX(FETCH_TS), 'EUROPE/WARSAW') AS LAST_BATCH_TS,
    COUNT(DISTINCT FETCH_ID) AS BATCHES_CNT,
    COUNT(DISTINCT PARAMS['VEHICLE_NUMBER']) AS VEHICLES_CNT,
    COUNT(DISTINCT BUSINESS_ID) AS LINES_CNT
FROM LOCATIONS.HISTORY
;
