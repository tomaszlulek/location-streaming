# location-streaming <a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

The project provides a framework to fetch, load and analyze location data in a stream.

## Core technologies
Programming:
- [Scala](https://www.scala-lang.org/)
- [Cats Effect](https://typelevel.org/cats-effect/)
- [fs2](https://fs2.io/)
- [fs2-aws](https://github.com/laserdisc-io/fs2-aws)
- [http4s](https://http4s.org/)

Building:
- [sbt](https://www.scala-sbt.org/)
- [Docker](https://www.docker.com/)

Infrastructure:
- [AWS Kinesis, S3, Athena, DynamoDB](https://aws.amazon.com/)


## Building

The project uses sbt as the main build tool. You can run sbt directly or use the provided `Dockerfile` to build the project in the first stage and then the Docker image in the second.

Building Docker image:
```bash
docker build -t location-streaming:latest .
```

## Running applications

All provided applications can be run inside Docker containers in any execution environment (local, k8s, etc).

How to run an application:
```bash
docker run \
  --env-file ./{configFile} \
  location-streaming:latest \
  app-runner \
  {appName}
```

- `appName` - the name of the application to be run (listed below) - you can use any object from pl.edu.geolocation package implementing main method
- `configFile` - a file with necessary settings for the specific application, configuration templates are available in the [examples](examples/config)

### SrcFetcher
The application creates fs2 stream and sends it to Amazon Kinesis. Example implementation sources the data from the API with real-time positions of all buses and trams in the city of Warsaw (https://api.um.warszawa.pl). SrcFetcher must transform the source data to the common model. It means providing the following fields:

- business_id - the main identifier used to query for the locations (for example bus or tram line number, customer id)
- unique_id - unique entity identifier (at any given time) - for example line number with vehicle id, customer id with device_id
- lat - latitude
- long - longtitude
- ts - observation timestamp

This model is then used by applications writing data in S3 and DynamoDB.

### Kinesis2S3
The application consumes the location data from Amazon Kinesis and stores it in Amazon S3. Then, the data can be easily used for analytics.
### Kinesis2DDB
The application stores the location stream in Amazon DynamoDB to enable quick access for the current and historical locations of all objects.

## Analytics in Athena
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
## Queries in DynamoDB
The target DynamoDB table must include the following fields:

- business_id (PARTITION KEY - STRING)
- ts_with_unique_id (SORT KEY - STRING) - first part includes timestamp formatted as` yyyyMMddHHmmss`

Latitude and longtitude are loaded as the standard fields.

This data model enables efficient querying for the location data of the specific business_id in a given period. For example:

1. get the business_id latest location
2. get all locations of the business_id in a given time unit (year, month, day, hour)
