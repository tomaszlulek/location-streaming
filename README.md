# location-streaming <a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

The project provides a framework to fetch, load and analyse location data in a stream.

## Core technologies
- Scala
- Cats Effect
- fs2
- http4s
- AWS Kinesis
- AWS S3
- AWS Athena
- Docker

## Applications
### SrcFetcher
The application creates fs2 stream and sends it to Amazon Kinesis. Example implementation sources the data from the API with real-time positions of all buses and trams in the city of Warsaw (https://api.um.warszawa.pl).
### Kinesis2S3
The application consumes the location data from Amazon Kinesis and stores it in Amazon S3. Then, the data can be easily used for analytics.
### Kinesis2DDB
The application stores the location stream in Amazon DynamoDB to enable quick access for the current and historical locations of all objects.

## Analytics
The data stored in S3 can be easily analysed using Amazon Athena. How to create the table with the location data:

```sql
-- create database if necessary
CREATE DATABASE IF NOT EXISTS locations;

-- create external table on S3 data imported by Kinesis2S3 application
CREATE EXTERNAL TABLE IF NOT EXISTS `locations`.`history` (
  `unique_id` string,
  `business_id` string,
  `lat` double,
  `lon` double,
  `ts` timestamp,
  `params` map < string,
  string >,
  `fetch_id` string,
  `fetch_ts` timestamp
)
PARTITIONED BY (`load_dt` date)
ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe'
WITH SERDEPROPERTIES (
  'ignore.malformed.json' = 'FALSE',
  'dots.in.keys' = 'FALSE',
  'case.insensitive' = 'TRUE',
  'mapping' = 'TRUE'
)
STORED AS INPUTFORMAT 'org.apache.hadoop.mapred.TextInputFormat' OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION 's3://{YOUR_BUCKET}/{TABLE_DIRECTORY}/'
TBLPROPERTIES ('classification' = 'json');

-- add newly created partitions
MSCK REPAIR TABLE locations.history;

```
