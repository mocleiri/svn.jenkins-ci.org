package org.jvnet.hudson.tools;

import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;

import java.util.Collection;
import java.util.Set;
import java.util.Collections;
import java.io.File;

/**
 * @author Kohsuke Kawaguchi
 */
public class ExtensionPointListerFactory implements AnnotationProcessorFactory {
    public Collection<String> supportedOptions() {
        return Collections.emptyList();
    }

    public Collection<String> supportedAnnotationTypes() {
        return Collections.singletonList("*");
    }

    public AnnotationProcessor getProcessorFor(Set<AnnotationTypeDeclaration> decls, AnnotationProcessorEnvironment env) {
        String page = null;
        File output = new File("extension-points.wiki");
        for( String k : env.getOptions().keySet() ) {
            if(k.startsWith("-Apage="))
                page = k.substring("-Apage=".length());
            if(k.startsWith("-Atarget="))
                output = new File(k.substring("-Atarget=".length()));
        }
        return new ExtensionPointLister(env,page, output);
    }
}
