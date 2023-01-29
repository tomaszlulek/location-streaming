select
    count(1) as records_cnt,
    count(distinct fetch_id) as batches_cnt,
    count(distinct params['vehicle_number']) as vehicles_cnt,
    count(distinct business_id) as lines_cnt
from locations.history
;
