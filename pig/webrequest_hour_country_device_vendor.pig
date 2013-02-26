-- Counts webrequests by hour, country code, devive and vendor.

REGISTER 'kraken-generic-0.0.1-SNAPSHOT-jar-with-dependencies.jar'
REGISTER 'kraken-dclass-0.0.1-SNAPSHOT.jar'
REGISTER 'kraken-pig-0.0.1-SNAPSHOT.jar'
REGISTER 'piggybank.jar'
REGISTER 'geoip-1.2.5.jar'

-- import LOAD_WEBREQUEST macro to load in webrequest log fields.
IMPORT 'include/load_webrequest.pig';

DEFINE EXTRACT             org.apache.pig.piggybank.evaluation.string.RegexExtract();
DEFINE GEOCODE_COUNTRY     org.wikimedia.analytics.kraken.pig.GeoIpLookup('countryCode', 'GeoIPCity');
DEFINE DCLASS              org.wikimedia.analytics.kraken.pig.UserAgentClassifier(''); 
DEFINE TO_HOUR             org.wikimedia.analytics.kraken.pig.ConvertDateFormat('yyyy-MM-dd\'T\'HH:mm:ss', 'yyyy-MM-dd_HH');


-- Set this as a regular expression to filter for desired timestamps.
-- E.g. if you want to only compute stats for a given hour, then
--   -p hour_regex='2013-01-02_15'.
-- This will make your output only contain values for 15:00 on Jan 2.
-- If you want hourly output for an entire day, then:
--   -p hour_regex='2013-01-02_\d\d'
-- should do just fine.
-- The default is to match all hour timestamps.
%default hour_regex '.*';

LOG_FIELDS     = LOAD_WEBREQUEST('$input');

LOG_FIELDS     = FILTER LOG_FIELDS BY (request_method MATCHES '(GET|get)');
LOG_FIELDS     = FILTER LOG_FIELDS BY (content_type MATCHES '.*(text\\/html).*' OR (content_type == '-'));

LOG_FIELDS     = FOREACH LOG_FIELDS GENERATE 
                    TO_HOUR(timestamp) AS day_hour,
                    FLATTEN(GEOCODE_COUNTRY(remote_addr)) AS country,
                    FLATTEN(DCLASS(user_agent)) AS 
                       (device:chararray,
                        vendor:chararray,
                        model:chararray,
                        is_wireless:chararray,
                        is_tablet:chararray);

-- only compute stats for hours that match $hour_regex;
LOG_FIELDS    = FILTER LOG_FIELDS BY day_hour MATCHES '$hour_regex';

COUNT          = FOREACH (GROUP LOG_FIELDS BY (day_hour, country, device, vendor) PARALLEL 10) GENERATE FLATTEN(group), COUNT(LOG_FIELDS.country) as num;
COUNT          = ORDER COUNT BY day_hour, country, device, vendor;

STORE COUNT into '$output';