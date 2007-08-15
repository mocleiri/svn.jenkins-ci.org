package hudson.plugins.violations.parse;

import java.util.Map;
import java.util.HashMap;

import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;

import hudson.plugins.violations.TypeDescriptor;
import hudson.plugins.violations.util.CloseUtil;

/**
 * The descriptor class for findbugs violations type.
 */
public final class FindBugsDescriptor
    extends TypeDescriptor {

    /** The descriptor for the findbugs violations type. */
    public static final FindBugsDescriptor DESCRIPTOR
        = new FindBugsDescriptor();

    private FindBugsDescriptor() {
        super("findbugs");
    }

    /**
     * Create a parser for the findbugs.
     * @return a new findbugs parser.
     */
    @Override
    public AbstractTypeParser createParser() {
        return new FindBugsParser();
    }

    private static Map<String, String> messageMap
        = new HashMap<String, String>();

    private static Map<String, String> detailMap
        = new HashMap<String, String>();

    /**
     * Get a detailed description of a violation source.
     * @param source the code name of the violation.
     * @return a detailed description, if available, null otherwise.
     */
    public String getDetailForSource(String source) {
        return detailMap.get(source);
    }

    /**
     * Get the map of cause to message.
     * @return the map.
     */
    public static Map<String, String> getMessageMap() {
        return messageMap;
    }

    static {
        parseMessages();
    }

    private static void parseMessages() {
        InputStream is = null;
        try {
            is = FindBugsParser.class.getResourceAsStream(
                "FindBugsParser/messages.xml");
            if (is != null) {
                ParseXML.parse(is, new ParseMessages());
            }
        } catch (Exception ex) {
            // Ignore
        } finally {
            CloseUtil.close(is);
        }
    }

    private static class ParseMessages extends AbstractParser {
        public void execute() {
            try {
                doIt();
            } catch (Exception ex) {
                // ignore
            }
        }
        private void doIt() throws Exception {
            while (true) {
                toTag("BugPattern");
                String type = checkNotBlank("type");
                toTag("ShortDescription");
                String desc = getNextText("missin");
                messageMap.put(type, desc);
                toTag("Details");
                String details = getNextText("Details");
                detailMap.put(type, details);
            }
        }
        private void toTag(String name) throws Exception {
            while (true) {
                if (getParser().getEventType() == XmlPullParser.START_TAG) {
                    if (getParser().getName().equals(name)) {
                        return;
                    }
                }
                getParser().next();
            }
        }
    }


}

