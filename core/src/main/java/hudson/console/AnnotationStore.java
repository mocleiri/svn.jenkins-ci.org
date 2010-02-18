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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Storage of {@link ConsoleAnnotation}s.
 *
 * <p>
 * {@link AnnotationStore} is always associated with an {@link OutputStream},
 * and stored annotations are used to annotate the text in that {@link OutputStream}.  
 *
 * <p>
 * There's really only one concrete implementation of annotation store, that is
 * {@link FileAnnotationStore}s, but defining it as an interface lets us use
 * this as a remoting proxy.
 *
 * @author Kohsuke Kawaguchi
 * @see FileAnnotationStore
 */
public interface AnnotationStore extends Closeable {
    /**
     * Adds a new annotation at the current position of the paired stream.
     */
    void add(ConsoleAnnotation a) throws IOException;
}
