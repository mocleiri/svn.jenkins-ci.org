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
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
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
    private final File output;

    public ExtensionPointLister(AnnotationProcessorEnvironment env, String pageName, File output) {
        this.env = env;
        this.pageName = pageName;
        this.output = output;
    }

    public void process() {
        // build type name Map
        Map<String/*simple name*/,String/*qualified name*/> typeNames = new HashMap<String, String>();
        for (TypeDeclaration td : env.getTypeDeclarations()) {
            if(typeNames.put(td.getSimpleName(),td.getQualifiedName())!=null)
                // conflict
                typeNames.put(td.getSimpleName(),"");
        }

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
            PrintWriter pw = new PrintWriter(output);
            IOUtils.copy(
                    new InputStreamReader(getClass().getResourceAsStream("preamble.txt")),
                    pw);

            for (TypeDeclaration ep : extensionPoints) {
                pw.printf("h4.[%s|%s@javadoc]\n",ep.getSimpleName(),ep.getQualifiedName().replace('.','/'));
                for( String line : ep.getDocComment().split("\n")) {
                    if(line.trim().length()==0) break;

                    {// replace @link
                        Matcher m = LINK.matcher(line);
                        StringBuffer sb = new StringBuffer();
                        while(m.find()) {
                            String simpleName = m.group(1);
                            String fullName = typeNames.get(simpleName);
                            if(fullName==null || fullName.length()==0) {
                                // unknown
                                m.appendReplacement(sb,"{{$1{}}}");
                            } else {
                                m.appendReplacement(sb, '['+simpleName+'|'+fullName.replace('.','/')+"@javadoc]");
                            }
                        }
                        m.appendTail(sb);
                        line = sb.toString();
                    }

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
                p.setContent(FileUtils.readFileToString(output));
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

    private static final Pattern LINK = Pattern.compile("\\{@link ([^}]+)}");
    private static final Macro[] MACROS = new Macro[] {
        new Macro(LINK,     "{{$1{}}}"),
        new Macro(Pattern.compile("<tt>([^<]+?)</tt>"),  "{{$1{}}}"),
        new Macro(Pattern.compile("<b>([^<]+?)</b>"),  "*$1*"),
        new Macro(Pattern.compile("<p/?>"),  "\n"),
        new Macro(Pattern.compile("</p>"),  "")
    };
}
