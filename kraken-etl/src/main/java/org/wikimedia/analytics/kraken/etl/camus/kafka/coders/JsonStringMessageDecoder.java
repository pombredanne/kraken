// Copyright (C) 2013 Wikimedia Foundation
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

package org.wikimedia.analytics.kraken.etl.camus.kafka.coders;

import java.util.Properties;
import java.text.SimpleDateFormat;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

import com.linkedin.camus.coders.CamusWrapper;
import com.linkedin.camus.coders.MessageDecoder;

import org.apache.log4j.Logger;


/**
 * MessageDecoder class that will convert the payload into a JSON object,
 * look for a field named 'timestamp', and then set the CamusWrapper's
 * timestamp property to the record's timestamp.  If the JSON does not have
 * a timestamp, then System.currentTimeMillis() will be used.
 * This MessageDecoder returns a CamusWrapper that works with Strings.
 */
public class JsonStringMessageDecoder extends MessageDecoder<byte[], String> {
    private static org.apache.log4j.Logger log = Logger.getLogger(JsonStringMessageDecoder.class);

    public  static final String CAMUS_MESSAGE_TIMESTAMP_FORMAT = "camus.message.timestamp.format";
    public  static final String DEFAULT_TIMESTAMP_FORMAT       = "[dd/MMM/yyyy:HH:mm:ss Z]";

    public  static final String CAMUS_MESSAGE_TIMESTAMP_FIELD  = "camus.message.timestamp.field";
    public  static final String DEFAULT_TIMESTAMP_FIELD        = "timestamp";

    private String timestampFormat;
    private String timestampField;

    @Override
    public void init(Properties props, String topicName) {
        this.props     = props;
        this.topicName = topicName;

        timestampFormat = props.getProperty(CAMUS_MESSAGE_TIMESTAMP_FORMAT, DEFAULT_TIMESTAMP_FORMAT);
        timestampField  = props.getProperty(CAMUS_MESSAGE_TIMESTAMP_FIELD,  DEFAULT_TIMESTAMP_FIELD);
    }

    @Override
    public CamusWrapper<String> decode(byte[] payload) {
        long       timestamp = 0;
        String     payloadString;
        JsonObject jsonObject;

        payloadString =  new String(payload);

        // Parse the payload into a JsonObject.
        try {
            jsonObject = new JsonParser().parse(payloadString).getAsJsonObject();
        } catch (RuntimeException e) {
            log.error("Caught exception while parsing JSON string '" + payloadString + "'.");
            throw new RuntimeException(e);
        }

        // Attempt to read and parse the timestamp element into a long.
        if (jsonObject.has(timestampField)) {
            String timestampString = jsonObject.get(timestampField).getAsString();
            try {
                timestamp = new SimpleDateFormat(timestampFormat).parse(timestampString).getTime();
            } catch (Exception e) {
                    log.error("Could not parse timestamp '" + timestampString + "' while decoding JSON message.");
            }
        }

        // If timestamp wasn't set in the above block,
        // then set it to current time.
        if (timestamp == 0) {
            log.warn("Couldn't find or parse timestamp field '" + timestampField + "' in JSON message, defaulting to current time.");
            timestamp = System.currentTimeMillis();
        }

        return new CamusWrapper<String>(payloadString, timestamp);
    }
}
