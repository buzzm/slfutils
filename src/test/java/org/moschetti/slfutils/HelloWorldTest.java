package test;

import org.junit.Test;

import org.moschetti.slfutils.ExtJSONLogger;

import java.math.BigDecimal;
import java.time.Instant;
//import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class HelloWorldTest {

    // Must keep ExtJSONLogger type (instead of Logger interface type) in
    // order to properly invoke info (or warn, error, etc.) with arg #2
    // as a Map, not an Object to be formatted into the msg!
    private static final ExtJSONLogger LOG = new ExtJSONLogger(LoggerFactory.getLogger(HelloWorldTest.class));

    @Test
    public void breathing() {
	//if(true){return;}
	
	LOG.info("hello; nothing fancy");
	LOG.info("parameters still {}", "work");	

	// Fluent API still works:
	LOG.atWarn().setMessage("temp changed").addKeyValue("oldT", 72).addKeyValue("newT", 68).log();
	

	// Compact way of send a bunch of structured data.
	// Map.of() only compiles up to 10 *immediate* child key-value
	// pairs -- but those children can be Map.to(), each of which
	// can compile up to 10 items.
	// For unlimited items, use ofEntries() and Map.entry():
	//   Map.ofEntries(Map.entry(111, "Mumbai"), 
	//                 Map.entry(222, "London"), 
	//                 Map.entry(333, "Bangalore"),
	//                 ...);
	//
	// Map.of() returns java.util.ImmutableCollections
	LOG.info("fancy",
		 Map.of( //
			"name", "plain ol' string",
			"escape", "\"Huh?\"\nThe\tpath C:\\foo\\bar",
			"badClass", new java.util.Random(), 
			"price", new BigDecimal("100.09"),
			"when", Instant.now(),
			"the_doubles", List.of(34.11, -0.03, 17.55),
			"randos", Map.of(
					 "a",1,
					 "b",2,
					 "c",3,
					 "d",4,
					 "e",5,
					 "f",6),
			"randos2", Map.of(
					 "a",1,
					 "b",2,
					 "c",3,
					 "d",4,
					 "e",5,
					 "f",6)			
			
			));


	AnotherClass.doSomething();

	LOG.warn("be careful");
	
	SeparateClass.doSomethingElse();	
    }
}
