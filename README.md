# slfutils
slf4j v2.x compatible logger and encoder for JSON, supporting extra rich data 

## Overview
```
import org.moschetti.slfutils.ExtJSONLogger;
import org.slf4j.LoggerFactory;

public class MyClass {
    // Wrap a Logger returned from the factory:
    private static final ExtJSONLogger LOG = new ExtJSONLogger(LoggerFactory.getLogger(MyClass.class)); 

    public static void main(String[] args) {
        LOG.info("hello; nothing fancy"); // Just like normal
        LOG.info("parameters still {}", "work");		

        // Fluent API still works:
        LOG.atWarn().setMessage("temp changed").addKeyValue("oldT", 72).addKeyValue("newT", 68).log();

        // ExtJSONLogger recognizes (String msg, Map m) signature:
        LOG.info("fancy", Map.of(
			"name", "plain ol' string",
			"price", new BigDecimal("100.09"),
			"when", Instant.now(),
			"the_doubles", List.of(34.11, -0.03, 17.55),
			"randos", Map.of("a",1, "b",2)
			)
        );
    }
}


Run with API, the ExtJSONLogger in moschetti-slfutils.jar, and the required backend.
Note that logstash and other jars (in particular jackson) are not necessary:
java -cp .:moschetti-slfutils.jar:logback-classic-1.5.6.jar:logback-core-1.5.6.jar:slf4j-api-2.0.13.jar MyClass

Log Output:
(Adding a CR here to make the line less long for the docs:)
{"t":"2024-05-24T19:02:10.301Z","l":"INFO","msg":"fancy data","class":"MyClass","method":"INFO","line":14,\
"x":{"name":"plain ol' string","price":100.09,"randos":{"a":1,"b":2},"when":"2024-05-24T19:02:10.285671Z","the_doubles":[34.11,-0.03,17.55]}}
```

## Motivation

`slf4j` is a very popular logging framework for Java.  Much has been written
on the benefits of `slf4j` over the built-in `java.util.logging` framework --
in particular, physically separating interface/API from backend implementation
so that libraries can robustly log messages without dealing with the specific
needs of the output (console, files, log rotation, filtering, etc.)

Out-of-the-box backend implementations include `logstash` 
are capable of emitting JSON logs.  JSON as a logging format is
significantly more practical than unstructured output in the long term
because it is much more easily queried, parsed, loaded into analytics, and
capable of changing shape over time without breaking the parse.

These out-of-the-box implementations have two non-trivial problems that
are addressed by `ExtJSONLogger`:

  1.  Most of them use the `jackson` libraries to convert Objects to JSON.
  By itself this is not a problem as the `jackson` JSON libs are extremely well
  known but their use in the logging framework introduces a dependency where
  the likelihood "versionitis" is high; that it, something else in the compile
  and/or execution of the runtime will rely on a different version of jackson.
  The goal here is to ensure the logging framework does not "wag the dog" by
  creating a dependency at the very lowest level of code.
  The encoder for `ExtJSONLogger` has zero dependencies.

  2.  Since the output format is JSON, it is desirable, if necessary, to
  add extra information to the log output in structured form, not a formatted
  string.  Here is a representative example of data "baked-into" a formatted
  string:
      ```
      // mname e.g. "xsonutils"
      // versArr e.g. [3,0,2]
      // millis e.g. 3241
      // someDate e.g. java.util.Date()
      LOG.info("request load: module: " + mname + "; version: " + String.join(",",versArr) + "; loadtime: " + millis + "; authdate: " + DateTimeFormatter.ISO_INSTANT.format(someDate.toInstant()));

      // Emits:
      {...,"l":"INFO","msg":"request load: module: xsonutils; version: 3.0.2; loadtime: 3241; authdate: 2024-05-24T13:41:05.184Z"}
      ```
      The overall result is poor in two ways:

        *  It is somewhat tedious to format the data into the desired string.
	   Formatting of dates in UTC+0 time is particularly irksome.

        *  It is even more troublesome to parse the data from the formatted string


      The `ExtJSON` logger enables this alternative:
      ```
      LOG.info("request load", Map.of("module",mname,"version",versArr,"loadtime",someDate));

      // Emits:
      {...,"l":"INFO","msg":"request load", "x": {"module":"xsonutils","version":[3,0,2], "loadtime":3241, "authdate":"2024-05-24T13:41:05.184Z"}}
      ```
      The overall experience is substantially improved:

       *  Existing common-but-not-string objects including dates and BigDecimal
          can be used right in the construction of the log message without
	  having to concern oneself with formatting.

       *  It is now easy, for example, to search the logs for occurances of
       	  module `xsonutils` where the major version is 3 and the minor
	  version is greater than or equal to 2; note we first check that `.x`
	  exists and is an object:
      ```
          jq 'select((.x|type)=="object") and .x.module == "xsonutils" and .x.version[0] == 3 and .x.version[1] >= 2)' myApp.log

          # Can also do direct array comparison:
          jq 'select((.x|type)=="object" and .x.version[:2]==[3,0])' myApp.log


      ```
      
## Detail
`ExtJSONLogger` is actually two parts:

  1.  A delegator class `ExtJSONLogger` that adds the `Map`-aware functionality.
      Constructors for this class will appear "at the top" of a class file in
      an idiom nearly identical to that of standard `slf4j` Logger classes.
      
  2.  A new JSON encoder that has no dependencies and is capable of rendering
      to string a limited number of types.

The `org.slf4j.MDC` facility is used to carry the `Map` over to the encoder.

To make rich data logging more convenient, the following types are directly supported in the `Map`:

|Type|Output|
|:--|:----|
|`String`|JSON strings, appropriately escaped|
|`int, long, double`|these will render as JSON numerics|
|`BigDecimal, BigInteger`|these will render as JSON numerics|
|`Date, Instant`|UTC+0 (Z) ISO-8601 string e.g. `2024-05-24T14:41:48.572Z`|
|`Map, List`|will be recursed|


The Overview section above serves as a sufficient example.

Logs are always emitted with JSON keys in the following order.  `x` is always
the last field:

|#|Field Name|Description of value|
|:-|:-|:-------|
|1|`t`|UTC+0 (Z) ISO-8601 string as created by the logback implementation i.e. `ExtJSONEncoder` does **not** create this timestamp|
|2|`l`|level e.g. `WARN`, `INFO`|
|3|'msg'|message, appropriately escaped|
|4|`class`|class name as provided in the Logger constructor|
|5 (if enabled)|`method`|method in the class generating the log|
|6 (if enabled)|`line`|line in the source code generating the log|
|7 (if present in `log` call)|`x`|JSON representation of the additional data passed in Map|



###  logback.xml
The `ExtJSONLogger` class only defines the `Map`-aware methods; it is the
`ExtJSONEncoder` that actually does the work.  Additionally -- and
importantly -- there is no need to create another `appender` in the `slf4j`
framework.  Common appenders like `ch.qos.logback.core.ConsoleAppender` and
`ch.qos.logback.core.rolling.RollingFileAppender` are completely compatible.
You need only change the `encoder` property as follows:
```
OLD (example):
<configuration>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  ...

NEW:
<configuration>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="org.moschetti.slfutils.ExtJSONEncoder"/>
  ...

  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <encoder class="org.moschetti.slfutils.ExtJSONEncoder">
          <includeCallerData>true</includeCallerData>
    </encoder>

    <file>myApp.log</file>
    <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
   ...
```


###  ExtJSONEncoder options
  * `<includeCallerData>true|false</includeCallerData> default false`<br>
    Accoridng to the `logback` documentation, getting method and line number
    from the caller can be "expensive".  Enabling this option will cause
    method name and line number to appear in structured output e.g.:
    ```
    {"t":"2024-05-24T15:02:30.891Z","l":"INFO","msg":"Logging using regular plain logs","class":"test.SeparateClass","method":"breathing","line":69}
    ```

###  Compatibility and Other Packages
Simply changing the encoder for an appender to `ExtJSONEncoder` will properly
convert all material logged by `slf4j`-compliant loggers to JSON; it is not
necessary to change existing code to create an `ExtJSONLogger` class.  However,
if code wishes to pass additional material as a Map to the log, then that
class must be changed to use `ExtJSONLogger`.

If code calls `LOG.info(String msg, Map m)` (or `warn()` or any of the other
levels) and `LOG` is a regular `LoggerFactory` logger and not `ExtJSONLogger`,
the `Map` argument will simply be ignored as the base `Logger` backend does not
know how to emit a `Map`.  The timestamp, level, message, class, and optional
method and line will be emitted, however.  Similarly, if a standard encoder
is called from `ExtJSONLogger LOG.info(String msg, Map m)`, the log will be
emitted as normal and the `Map` will be quietly ignored.



If an application places an unsupported object into the `Map` (e.g. a custom
class), then `ExtJSONEncoder` will emit a question mark (?) as a string for
the value.  It cannot rely on the object `toString()` to yield a reasonable
value for JSON, even with careful escaping.


## Building, Testing, and Installing
`ExtJSONLogger` uses an `ant`-based build.  Rather than let `mvn` do
transitive closure on dependencies (and possibly negate one of the motivations
for this package), "manually" procure these three libs:

 *  slf4j-api-2.0.13.jar

 *  logback-classic-1.5.6.jar

 *  logback-core-1.5.6.jar

We require a backend implementation because `ExtJSONEncoder` extends
`ch.qos.logback.core.encoder.EncoderBase<ILoggingEvent>` and thus needs access
to those class definitions.

```
git clone https://github.com/buzzm/slfutils.git
cd slfutils
ant clean   # just to be sure
ant testcompile 
ant testrun  # not an official test
ant install  # copies jar to the directory specified in build.xml
```




