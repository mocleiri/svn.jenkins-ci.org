/*
 *  The MIT License
 * 
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Seiji Sogabe
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 * 
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 * 
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package hudson.model;

import hudson.Extension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.sf.json.JSONObject;
import org.jvnet.localizer.LocaleProvider;
import org.kohsuke.stapler.StaplerRequest;

/**
 * A UserProperty that remembers user's locale.
 *
 * @author Seiji Sogabe
 */
public class UserLocaleProperty extends UserProperty {

    /**
     * Supported locales by Hudson.
     */
    private static final Locale[] SUPPORTED_LOCALES;

    static {
        List<Locale> locales = new ArrayList<Locale>();
        for (Locale locale : Locale.getAvailableLocales()) {
            if (locale.getLanguage().equals("") || locale.getCountry().equals("") || !locale.getVariant().equals(""))
                continue;
            locales.add(locale);
        }
        Collections.sort(locales, new Comparator<Locale>() {
            public int compare(Locale o1, Locale o2) {
                String l1 = o1.getLanguage();
                String l2 = o2.getLanguage();
                if (!l1.equals(l2))
                    return l1.compareTo(l2);
                return o1.getCountry().compareTo(o2.getCountry());
            }
        });
        SUPPORTED_LOCALES = locales.toArray(new Locale[0]);
    }

    public static Locale[] getSupportedLocales() {
        return SUPPORTED_LOCALES.clone();
    }

    private final Locale locale;

    public UserLocaleProperty(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }

    public Locale getDisplayLocale() {
        return locale != null ? locale : LocaleProvider.getLocale();
    }

    @Extension
    public static class DescriptorImpl extends UserPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.UserLocaleProperty_DisplayName();
        }

        @Override
        public UserProperty newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            String value = formData.getString("locale");
            Locale locale = str2Locale(value);
            return new UserLocaleProperty(locale);
        }

        @Override
        public UserProperty newInstance(User user) {
            return new UserLocaleProperty(null);
        }

        /**
         * "ja_JP" -> Locale("ja", "JP"). variant is not supported.
         */
        private Locale str2Locale(String value) {
            if (value == null || value.length() == 0)
                return null;
            String parts[] = value.split("_", -1);
            if (parts.length == 1) 
                return new Locale(parts[0]);
            else if (parts.length == 2) 
                return new Locale(parts[0], parts[1]);
            return null;
        }
    }
}
