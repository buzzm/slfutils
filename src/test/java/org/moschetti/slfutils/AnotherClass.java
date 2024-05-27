package test;

//  djijwd
import org.moschetti.slfutils.ExtJSONLogger;

import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Map;
import java.util.HashMap;

import java.util.List;

public class AnotherClass {

    private static final ExtJSONLogger LOG = new ExtJSONLogger(LoggerFactory.getLogger(AnotherClass.class));

    public static void doSomething() {
        LOG.info("Doing something in AnotherClass");

	// How to not incur cost of extra info construction if we are
	// not in DEBUG level logging:

	if(LOG.isDebugEnabled()) {
	    Map m2 = new HashMap();
	    m2.put("random_junk", List.of("zponc", -1, new java.util.Date()));
	    LOG.debug("in the weeds in doSomething", m2);
	} else {
	    // OBVIOUSLY we wouldn't print this; it is just a confirming aid...
	    System.out.println("debug NOT enabled; not logging");
	}

	Marker importantMarker = MarkerFactory.getMarker("IMPORTANT");

        // Log a message with the marker
        LOG.warn(importantMarker, "This is an important WARN log message");


        LOG.trace("very low level here");
	
    }
}
