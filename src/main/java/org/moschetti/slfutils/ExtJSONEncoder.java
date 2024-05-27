package org.moschetti.slfutils;

import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.nio.charset.StandardCharsets;


import java.time.Instant;
import java.util.Date;
import java.util.Date;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Base64;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.format.DateTimeFormatter;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import java.io.IOException;

public class ExtJSONEncoder extends EncoderBase<ILoggingEvent> {

    private boolean includeCallerData = false;

    public void setIncludeCallerData(boolean includeCallerData) {
        this.includeCallerData = includeCallerData;
    }
    
    @Override
    public byte[] encode(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder();

	// Preserve order: t,l,msg,class,method,x
	Map m = new LinkedHashMap();

	m.put("t", Instant.ofEpochMilli(event.getTimeStamp()));
	m.put("l", event.getLevel().toString());
	m.put("msg", escapeJsonString(event.getFormattedMessage()));
	m.put("class", event.getLoggerName());

	if(includeCallerData) {
	    if (!event.hasCallerData()) {
		event.getCallerData();
	    }	
	    if (event.hasCallerData()) {
		// java.lang.StackTraceElement
		StackTraceElement ste = event.getCallerData()[1];
		m.put("method", ste.getMethodName());
		m.put("line", ste.getLineNumber());	    
	    }
	}
	
        // Add custom MDC properties if available
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        if (mdcProperties != null) {
	    String xx = mdcProperties.get("ExtJSONLogger-payload");
	    if(xx != null) {
		try {
		    Map xtra = (Map) deserializeFromString(xx);
		    m.put("x", xtra);
		} catch(Exception e) {
		    // for now, ignore failed decode
		}
	    }
	}

	String xx = toJson(m) + "\n";
	return xx.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    	
    private static Object deserializeFromString(String data)
	throws IOException, ClassNotFoundException
    {
	byte[] bytes = Base64.getDecoder().decode(data);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return in.readObject();
        }
    }


    // Our JSON needs are simple so avoid dependency hell and just code
    // up a to-string imp here.  100 lines of code AND it will properly
    // handle Date, Instance, BigDecimal, and BigInteger.  ;-)
    private static final DateTimeFormatter ISO_INSTANT_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    /**
     *  ONLY string, int, long, double, BigDecimal, BigInteger, Date,
     *  and Instant types are allowed.  
     */
    private static String toJson(Object obj) {
	// In order of most frequent appearance for performance...
	if (obj instanceof String) {
            return "\"" + escapeJsonString((String) obj) + "\"";

        } else if (obj instanceof Integer || obj instanceof Long || obj instanceof Double || obj instanceof Boolean) {
            return obj.toString();
	    
        } else if (obj instanceof Date) {
            return "\"" + ISO_INSTANT_FORMATTER.format(((Date) obj).toInstant()) + "\"";
        } else if (obj instanceof Instant) {
            return "\"" + ISO_INSTANT_FORMATTER.format((Instant) obj) + "\"";

        } else if (obj instanceof BigDecimal || obj instanceof BigInteger) {
            return obj.toString();
	    
	} else if (obj instanceof Map) {
            return mapToJson((Map<String, ?>) obj);
        } else if (obj instanceof List) {
            return listToJson((List<?>) obj);

	} else {
            //throw new IllegalArgumentException("Unsupported type: " + obj.getClass().getName());
	    // Rather than have the low-level logging system throw an
	    // exception and potentially drop the runtime, just make a
	    // question mark string:
            return "\"?\"";
        }
    }

    private static String mapToJson(Map<String, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        boolean first = true;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escapeJsonString(entry.getKey())).append("\":");	    
            sb.append(toJson(entry.getValue()));
        }

        sb.append("}");
        return sb.toString();
    }

    private static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        boolean first = true;
        for (Object item : list) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(toJson(item));
        }

        sb.append("]");
        return sb.toString();
    }

    private static String escapeJsonString(String value) {
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    // Check for other control characters and unicode escaping
                    if (c < 0x20 || (c >= 0x80 && c < 0xa0) || (c >= 0x2000 && c < 0x2100)) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }    
}
