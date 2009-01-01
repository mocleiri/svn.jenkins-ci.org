package org.jvnet.hudson.tools;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.type.InterfaceType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.jvnet.hudson.confluence.Confluence;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.net.URL;

import hudson.plugins.jira.soap.ConfluenceSoapService;
import hudson.plugins.jira.soap.RemotePage;

import javax.xml.rpc.ServiceException;

/**
 * @author Kohsuke Kawaguchi
 */
public class ExtensionPointLister implements AnnotationProcessor {
    private final AnnotationProcessorEnvironment env;
    private final String pageName;

    public ExtensionPointLister(AnnotationProcessorEnvironment env, String pageName) {
        this.env = env;
        this.pageName = pageName;
    }

    public void process() {
        // list up extension points
        List<TypeDeclaration> extensionPoints = new ArrayList<TypeDeclaration>();
        for( TypeDeclaration td : env.getTypeDeclarations() )
            if(isExtensionPoint(td))
                extensionPoints.add(td);

        Collections.sort(extensionPoints,new Comparator<TypeDeclaration>() {
            public int compare(TypeDeclaration o1, TypeDeclaration o2) {
                return o1.getSimpleName().compareTo(o2.getSimpleName());
            }
        });

        // without a proper context classloader Axis dies with NPE.
        ClassLoader oldCC = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        try {
            File target = new File("./target/extension-points.wiki");
            PrintWriter pw = new PrintWriter(target);
            IOUtils.copy(
                    new InputStreamReader(getClass().getResourceAsStream("preamble.txt")),
                    pw);

            for (TypeDeclaration ep : extensionPoints) {
                pw.printf("h4.[%s|%s@javadoc]\n",ep.getSimpleName(),ep.getQualifiedName().replace('.','/'));
                for( String line : ep.getDocComment().split("\n")) {
                    if(line.trim().length()==0) break;
                    for (Macro m : MACROS)
                        line = m.replace(line);
                    pw.print(line);
                    pw.print(' ');
                }
                pw.println();
            }

            pw.close();

            if(pageName!=null) {
                env.getMessager().printNotice("Uploading to "+pageName);
                ConfluenceSoapService service = Confluence.connect(new URL("http://hudson.gotdns.com/wiki/"));
                RemotePage p = service.getPage("", "HUDSON", pageName);
                p.setContent(FileUtils.readFileToString(target));
                service.storePage("",p);
            }
        } catch (IOException e) {
            env.getMessager().printError(e.getMessage());
        } catch (ServiceException e) {
            e.printStackTrace();
            env.getMessager().printError(e.getMessage());
        } finally {
            Thread.currentThread().setContextClassLoader(oldCC);
        }
    }

    private boolean isExtensionPoint(TypeDeclaration td) {
        for (InterfaceType intf : td.getSuperinterfaces())
            if(intf.getDeclaration().getQualifiedName().equals("hudson.ExtensionPoint"))
                return true;
        return false;
    }

    private static final class Macro {
        private final Pattern from;
        private final String to;

        private Macro(Pattern from, String to) {
            this.from = from;
            this.to = to;
        }

        public String replace(String s) {
            return from.matcher(s).replaceAll(to);
        }
    }

    private static final Macro[] MACROS = new Macro[] {
        new Macro(Pattern.compile("\\{@link ([^}]+)}"),     "{{$1{}}}"),
        new Macro(Pattern.compile("<tt>([^<]+?)</tt>"),  "{{$1{}}}"),
        new Macro(Pattern.compile("<b>([^<]+?)</b>"),  "*$1*"),
        new Macro(Pattern.compile("<p/?>"),  "\n"),
        new Macro(Pattern.compile("</p>"),  "")
    };
}
