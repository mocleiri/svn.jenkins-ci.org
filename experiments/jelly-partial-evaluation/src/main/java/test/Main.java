package test;

import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.Script;
import org.apache.commons.jelly.XMLOutput;

import java.net.URL;
import java.io.PrintWriter;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) throws Exception {
        JellyContext context = new JellyContext();
        URL res = Main.class.getClassLoader().getResource("test.jelly");
        Script script = context.compileScript(res);
        System.out.println("===");
        XMLOutput out = XMLOutput.createXMLOutput(new PrintWriter(System.out));
        script.run(new JellyContext(), out);
        out.close();
    }
}
