with added_prev as
(
select
    business_id, ts,
    lat, lon,
    lag(lat, 1) over(partition by unique_id order by ts) as prev_lat,
    lag(lon, 1) over(partition by unique_id order by ts) as prev_lon,
    lag(ts, 1) over(partition by unique_id order by ts) as prev_ts
from locations.history
where 1=1
    and lat between 51 and 53
    and lon between 20 and 22
),
deltas as
(
select
    business_id, ts,
    great_circle_distance(lat, lon, prev_lat, prev_lon) as distance,
    date_diff('second', prev_ts, ts) as time_diff
from added_prev
),
speeds as
(
select
    business_id, ts,
    time_diff,
    distance / time_diff * 3600 as speed_kph
from deltas
),
aggregates as
(
select
    business_id,
    extract(hour from AT_TIMEZONE(ts, 'EUROPE/WARSAW')) as day_hour,
    count(*) as cnt,
    avg(speed_kph) as avg_speed_kph,
    approx_percentile(speed_kph,0.5) as median_speed_kph
from speeds
where 1=1
    and time_diff between 10 and 120
    and speed_kph between 5 and 60
group by 1,2
)
select
    a.business_id,
    b.avg_speed_kph - a.avg_speed_kph as traffic_impact
from aggregates a
join aggregates b
    on a.business_id = b.business_id
where a.day_hour = 9
    and b.day_hour = 16
order by 2
;
