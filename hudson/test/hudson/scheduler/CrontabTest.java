package hudson.scheduler;

import java.text.ParseException;

/**
 * @author Kohsuke Kawaguchi
 */
public class CrontabTest {
    public static void main(String[] args) throws ParseException {
        for (String arg : args) {
            CronTab ct = new CronTab(new Runnable() {
                public void run() {

                }
            }, arg);
            System.out.println(ct.toString());
        }
    }
}
