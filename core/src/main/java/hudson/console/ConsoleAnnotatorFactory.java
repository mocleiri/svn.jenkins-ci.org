/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.console;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Hudson;
import org.jvnet.tiger_types.Types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class ConsoleAnnotatorFactory<T> implements ExtensionPoint {
    /**
     * For which context type does this annotator work?
     */
    public Class type() {
        Type type = Types.getBaseClass(getClass(), ConsoleAnnotator.class);
        if (type instanceof ParameterizedType)
            return Types.erasure(Types.getTypeArgument(type,0));
        else
            return Object.class;
    }

    public abstract ConsoleAnnotator newInstance(T context);

    /**
     * All the registered instances.
     */
    public static ExtensionList<ConsoleAnnotatorFactory> all() {
        return Hudson.getInstance().getExtensionList(ConsoleAnnotatorFactory.class);
    }
}
