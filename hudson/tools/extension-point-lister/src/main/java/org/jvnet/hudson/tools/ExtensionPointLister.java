package org.jvnet.hudson.tools;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.type.InterfaceType;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

/**
 * @author Kohsuke Kawaguchi
 */
public class ExtensionPointLister implements AnnotationProcessor {
    private final AnnotationProcessorEnvironment env;

    public ExtensionPointLister(AnnotationProcessorEnvironment env) {
        this.env = env;
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

        try {
            PrintWriter pw = new PrintWriter("./target/extension-points.wiki");
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
        } catch (IOException e) {
            env.getMessager().printError(e.getMessage());
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
