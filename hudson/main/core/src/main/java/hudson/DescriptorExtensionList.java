/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc.
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
package hudson;

import hudson.model.Descriptor;
import hudson.model.Describable;
import hudson.model.Hudson;
import hudson.model.ViewDescriptor;
import hudson.model.Descriptor.FormException;
import hudson.util.Memoizer;
import hudson.slaves.NodeDescriptor;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.concurrent.CopyOnWriteArrayList;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;

import org.jvnet.tiger_types.Types;
import org.kohsuke.stapler.Stapler;
import net.sf.json.JSONObject;

/**
 * {@link ExtensionList} for holding a set of {@link Descriptor}s, which is a group of descriptors for
 * the same extension point.
 *
 * Use {@link Hudson#getDescriptorList(Class)} to obtain instances.
 *
 * @param <D>
 *      Represents the descriptor type. This is {@code Descriptor<T>} normally but often there are subtypes
 *      of descriptors, like {@link ViewDescriptor}, {@link NodeDescriptor}, etc, and this parameter points
 *      to those for better type safety of users.
 *
 *      The actual value of 'D' is not necessary for the operation of this code, so it's purely for convenience
 *      of the users of this class.
 *
 * @since 1.286
 */
public final class DescriptorExtensionList<T extends Describable<T>, D extends Descriptor<T>> extends ExtensionList<D> {
    /**
     * Type of the {@link Describable} that this extension list retains.
     */
    private final Class<T> describableType;

    public DescriptorExtensionList(Hudson hudson, Class<T> describableType) {
        super(hudson, (Class)Descriptor.class, legacyDescriptors.get(describableType));
        this.describableType = describableType;
    }

    /**
     * Finds the descriptor that has the matching fully-qualified class name.
     *
     * @param fqcn
     *      Fully qualified name of the descriptor, not the describable.
     */
    public D find(String fqcn) {
        return Descriptor.find(this,fqcn);
    }

    /**
     * Creates a new instance of a {@link Describable}
     * from the structured form submission data posted
     * by a radio button group.
     */
    public T newInstanceFromRadioList(JSONObject config) throws FormException {
        if(config.isNullObject())
            return null;    // none was selected
        int idx = config.getInt("value");
        return get(idx).newInstance(Stapler.getCurrentRequest(),config);
    }

    public T newInstanceFromRadioList(JSONObject parent, String name) throws FormException {
        return newInstanceFromRadioList(parent.getJSONObject(name));
    }

    /**
     * Loading the descriptors in this case means filtering the descriptor from the master {@link ExtensionList}.
     */
    @Override
    protected List<D> load() {
        List r = new ArrayList();
        for( Descriptor d : hudson.getExtensionList(Descriptor.class) ) {
            Type subTyping = Types.getBaseClass(d.getClass(), Descriptor.class);
            if (!(subTyping instanceof ParameterizedType)) {
                LOGGER.severe(d.getClass()+" doesn't extend Descriptor with a type parameter");
                continue;   // skip this one
            }
            if(Types.getTypeArgument(subTyping,0)==describableType)
                r.add(d);
        }
        return r;
    }

    /**
     * Stores manually registered Descriptor instances.
     */
    private static final Memoizer<Class,List> legacyDescriptors = new Memoizer<Class,List>() {
        public List compute(Class key) {
            return new CopyOnWriteArrayList();
        }
    };

    private static final Logger LOGGER = Logger.getLogger(DescriptorExtensionList.class.getName());
}
