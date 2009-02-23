package org.jvnet.hudson;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.kohsuke.jnt.ProcessingException;
import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.JNProject;
import org.kohsuke.jnt.JNIssueComponent;
import org.kohsuke.jnt.JNFileFolder;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
    /**
     * Make sure issue tracker component exists for all the released plugins.
     */
    public void testAllPlugins() throws ProcessingException {
        JavaNet net = JavaNet.connect();
        JNProject hudson = net.getProject("hudson");

        JNIssueComponent comp = hudson.getIssueTracker().getComponent("hudson");

        Set<String> names = new HashSet<String>();
        for(String n : comp.getSubcomponents().keySet())
            names.add(n.toLowerCase());

        Set<String> problems = new TreeSet<String>();

        JNFileFolder folder = hudson.getFolder("/plugins");
        for( JNFileFolder plugin : folder.getSubFolders().values() ) {
            String n = plugin.getName().toLowerCase();
            if(n.equals("bco"))
                continue;   // this isn't a plugin in Hudson SVN
            if(names.contains(n) || names.contains(n+"-plugin"))
                continue;   // OK
            problems.add(plugin.getName());
        }
        if(!problems.isEmpty())
            fail(problems+" doesn't have a component");
    }
}
