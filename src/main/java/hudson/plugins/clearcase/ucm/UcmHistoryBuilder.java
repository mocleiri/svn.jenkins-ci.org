package hudson.plugins.clearcase.ucm;

import hudson.util.ArgumentListBuilder;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Henrik L. Hansen
 */
public class UcmHistoryBuilder {

    // how cleartool format part are found  
    private static final String REGEX_GROUP = "\"(.+)\"\\s+";
    // Defines parts of the lshistory format
    private static final String LSHISTORY_DATE = "\\\"%Nd\\\" ";
    private static final String LSHISTORY_USERID = "\\\"%u\\\" ";
    private static final String LSHISTORY_USERNAME = "\\\"%Fu\\\" ";
    private static final String LSHISTORY_EVENT = "\\\"%e\\\" ";
    private static final String LSHISTORY_FILE = "\\\"%En\\\" ";
    private static final String LSHISTORY_VERSION = "\\\"%Vn\\\" ";
    private static final String LSHISTORY_OPERATION = "\\\"%o\\\" ";
    private static final String LSHISTORY_ACTIVITY = "\\\"%[activity]p\\\" ";
    private static final String LSHISTORY_COMMENT = "\\n%c\\n";

    // Defines parts of the lsactivity  format
    private static final String LSACTIVITY_NAME = "\\\"%n\\\" ";
    private static final String LSACTIVITY_HEADLINE = "\\\"%[headline]p\\\" ";
    private static final String LSACTIVITY_STREAM = "\\\"%[stream]p\\\" ";
    private static final String LSACTIVITY_CONTRIBUTING = "\\\"%[contrib_acts]p\\\" ";
    private static final String LSACTIVITY_PLACEHOLDER = "\\\" \\\" ";
    // full lshistory output and parsing 
    private static final int LOG_PATTERN_COUNT = 6;
    private static final String LOG_FORMAT = LSHISTORY_DATE + LSHISTORY_FILE + LSHISTORY_VERSION + LSHISTORY_ACTIVITY + LSHISTORY_EVENT + LSHISTORY_OPERATION + LSHISTORY_COMMENT;
    private static final String LOG_PATTERN = REGEX_GROUP + REGEX_GROUP + REGEX_GROUP + REGEX_GROUP + REGEX_GROUP + REGEX_GROUP;
    // full lsactivity output and parsing 
    private static final int ACTIVITY_PATTERN_COUNT = 3;
    private static final String ACTIVITY_FORMAT = LSACTIVITY_NAME + LSACTIVITY_HEADLINE + LSACTIVITY_STREAM + LSACTIVITY_PLACEHOLDER;
    private static final String ACTIVITY_PATTERN = REGEX_GROUP + REGEX_GROUP + REGEX_GROUP + REGEX_GROUP;
    private static final int INTEGRATION_ACTIVITY_PATTERN_COUNT = 4;
    private static final String INTEGRATION_ACTIVITY_FORMAT = LSACTIVITY_NAME + LSACTIVITY_HEADLINE + LSACTIVITY_STREAM + LSACTIVITY_CONTRIBUTING;
    private static final String INTEGRATION_ACTIVITY_PATTERN = REGEX_GROUP + REGEX_GROUP + REGEX_GROUP + REGEX_GROUP;
    
    private static final int DATE_INDEX = 1;
    private static final int FILE_INDEX = 2;
    private static final int VERSION_INDEX = 3;
    private static final int ACTIVITY_INDEX = 4;
    private static final int EVENT_INDEX = 5;
    private static final int OPERATION_INDEX = 6;
    
    private static final int ACT_NAME_INDEX = 1;
    private static final int ACT_HEADLINE_INDEX = 2;
    private static final int ACT_STREAM_INDEX = 3;
    private static final int ACT_CONTRIB_INDEX = 4;
        
    
    
    private Pattern historyPattern;
    private Pattern activityPattern;
    private Pattern integrationActivityPattern;
    private SimpleDateFormat dateFormatter;
    private Map<String, UcmActivity> activityNameToEntry;    

    public UcmHistoryBuilder() {
        historyPattern = Pattern.compile(LOG_PATTERN);
        activityPattern = Pattern.compile(ACTIVITY_PATTERN);
        integrationActivityPattern = Pattern.compile(INTEGRATION_ACTIVITY_PATTERN);
        dateFormatter = new SimpleDateFormat("yyyyMMdd.HHmmss");
        activityNameToEntry = new HashMap<String, UcmActivity>();
    }

    /**
     * Returns the log format that the parser supports
     * 
     * @return the format for the 'cleartool lshistory' command
     */
    public static String getLogFormat() {
        return LOG_FORMAT;
    }

    public List<UcmActivity> buildChangelog(Reader inReader,String vobName) {

        try {
            activityNameToEntry.clear();
            List<UcmActivity> result = parse(inReader);
            // we need it cleared as we are now going to use it as information cache for template UcmActivities
            activityNameToEntry.clear();
            for (UcmActivity activity : result) {
                enhanceActivity(activity,vobName);
                // entries don't have subactivities at this stage so don't loop those
            }

            return result;

        } catch (IOException e) {
            //TODO: How to write to Hudson console log ?
            System.out.println("IOException caught when building changelog " + e.getMessage());
            return new ArrayList<UcmActivity>();
        } catch (ParseException e) {
            System.out.println("Parsecaught when building changelog " + e.getMessage());
            return new ArrayList<UcmActivity>();
        }


    }

    public List<UcmActivity> parse(Reader inReader) throws IOException, ParseException {
        List<UcmActivity> result = new ArrayList<UcmActivity>();

        BufferedReader reader = new BufferedReader(inReader);

        StringBuilder commentBuilder = new StringBuilder();
        String line = reader.readLine();

        UcmActivity.File currentFile = null;
        while (line != null) {

            //TODO: better error handling
            if (line.startsWith("cleartool: Error:")) {
                line = reader.readLine();
                continue;
            }
            Matcher matcher = historyPattern.matcher(line);

            // finder find start of lshistory entry
            if (matcher.find()) {

                if (currentFile != null) {
                    currentFile.setComment(commentBuilder.toString());
                }
                commentBuilder = new StringBuilder();
                currentFile = new UcmActivity.File();

                // read values;
                currentFile.setDate(dateFormatter.parse(matcher.group(DATE_INDEX)).getTime());
                currentFile.setName(matcher.group(FILE_INDEX));
                currentFile.setVersion(matcher.group(VERSION_INDEX));
                currentFile.setEvent(matcher.group(EVENT_INDEX));
                currentFile.setOperation(matcher.group(OPERATION_INDEX));

                String activityName = matcher.group(ACTIVITY_INDEX);

                UcmActivity activity = activityNameToEntry.get(activityName);
                if (activity == null) {
                    activity = new UcmActivity();                    
                    activity.setName(activityName);                    
                    activityNameToEntry.put(activityName, activity);
                    result.add(activity);
                }

                activity.addFile(currentFile);
            } else {
                if (commentBuilder.length() > 0) {
                    commentBuilder.append("\n");
                }
                commentBuilder.append(line);
            }
            line = reader.readLine();
        }
        if (currentFile != null) {
            currentFile.setComment(commentBuilder.toString());
        }
        return result;
    }

    public void enhanceActivity(UcmActivity entry, String vobName) throws IOException, InterruptedException {
        
        // lookup activity in cache, top levels will never be present
        UcmActivity templateActivity = activityNameToEntry.get(entry.getName());
        
        if (templateActivity == null) {
            templateActivity = performActivityLookup(entry.getName(),vobName);
            activityNameToEntry.put(entry.getName(),templateActivity);
        }
        
        entry.setHeadline(templateActivity.getHeadline());
        entry.setStream(templateActivity.getStream());
        entry.setUser(templateActivity.getUser());
        for (UcmActivity templateSubActivity : templateActivity.getSubActivities()) {
            UcmActivity subActivity = new UcmActivity();
            subActivity.setName(templateSubActivity.getName());
            enhanceActivity(subActivity, vobName);
            entry.addSubActivity(subActivity);                       
        }
        
    }
    public UcmActivity performActivityLookup(String activityName,String vobName) {    
        UcmActivity result = new UcmActivity();
        result.setName(activityName);
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        //todo: how to obtain this. Perhaps move actual lsactivity call to other place ?
        //TODO:cmd.add(clearToolExec);
        cmd.add("lsactivity");

        // Clearcase cannot handle [contrib_acts] if not a deliver or rebase activity
        if (result.isIntegrationActivity()) {
            cmd.add("-fmt", INTEGRATION_ACTIVITY_FORMAT);
        } else {
            cmd.add("-fmt", ACTIVITY_FORMAT);
        }
        // activity name must be last
        cmd.add(activityName + "@" + vobName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // how to get Launcher ?
        if (launcher.run(cmd.toCommandArray(), null, baos, null)) {
            parseActivityOutput(result,new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())), vobName);
        }
        return result;
    }

    private void parseActivityOutput(UcmActivity activity,Reader consoleReader, String vobName) throws IOException, InterruptedException {
        
        BufferedReader reader = new BufferedReader(consoleReader);
        String line = reader.readLine();
        Pattern currentPattern;
        
        if (activity.isIntegrationActivity()) {
            currentPattern = integrationActivityPattern;
        } else {
            currentPattern = activityPattern;
        }
                
        while (line != null) {

            //TODO: better error handling
            if (line.startsWith("cleartool: Error:")) {
                line = reader.readLine();
                continue;
            }

            Matcher matcher = currentPattern.matcher(line);
            if (matcher.find()) {
                activity.setHeadline(matcher.group(ACT_HEADLINE_INDEX));
                activity.setStream(matcher.group(ACT_STREAM_INDEX));
                String contributing = matcher.group(ACT_CONTRIB_INDEX);
                                               
                if (contributing.length() > 1) {
                    String[] contribs = contributing.split(" ");
                    for (String contrib : contribs) {
                        UcmActivity subActivity = new UcmActivity();
                        subActivity.setName(contrib);
                    }
                }
            }
            line = reader.readLine();
        }
    }
}
