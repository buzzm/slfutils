package test;

//  djijwd
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class SeparateClass {

    // Part of a build where other source uses ExtJSONLogger -- but this
    // one does not but still wants to log with the others!
    
    private static final Logger LOG = LoggerFactory.getLogger(SeparateClass.class);

    public static void doSomethingElse() {
        LOG.info("Logging using regular plain logs");
    }
}
