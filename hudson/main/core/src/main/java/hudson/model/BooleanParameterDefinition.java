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
package hudson.model;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import net.sf.json.JSONObject;
import hudson.Extension;

/**
 * @author huybrechts
 */
public class BooleanParameterDefinition extends ParameterDefinition {
    private final boolean defaultValue;

    @DataBoundConstructor
    public BooleanParameterDefinition(String name, boolean defaultValue, String description) {
        super(name, description);
        this.defaultValue = defaultValue;
    }

    public boolean isDefaultValue() {
        return defaultValue;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        BooleanParameterValue value = req.bindJSON(BooleanParameterValue.class, jo);
        value.setDescription(getDescription());
        return value;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req) {
        String[] value = req.getParameterValues(getName());
        if (value == null) {
            return getDefaultParameterValue();
        } else if (value.length != 1) {
            throw new IllegalArgumentException("Illegal number of parameter values for " + getName() + ": " + value.length);
        } else {
            boolean booleanValue = Boolean.parseBoolean(value[0]);
            return new BooleanParameterValue(getName(), booleanValue, getDescription());
        }
    }

    @Override
    public BooleanParameterValue getDefaultParameterValue() {
        return new BooleanParameterValue(getName(), defaultValue, getDescription());
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.BooleanParameterDefinition_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/help/parameter/boolean.html";
        }
    }

}
