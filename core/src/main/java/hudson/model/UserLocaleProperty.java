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
import hudson.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.jvnet.localizer.LocaleProvider;
import org.kohsuke.stapler.DataBoundConstructor;

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

    private final String locale;

    @DataBoundConstructor
    public UserLocaleProperty(String locale) {
        this.locale = Util.fixEmptyAndTrim(locale);
    }

    public Locale getLocale() {
        return parse(locale);
    }

    public Locale getDisplayLocale() {
        return locale != null ? parse(locale) : LocaleProvider.getLocale();
    }

    /**
     * "ja_JP" -> Locale("ja", "JP")
     */
    private Locale parse(String value) {
        String parts[] = value.split("_");
        switch (parts.length) {
            case 1:
                return new Locale(parts[0]);
            case 2:
                return new Locale(parts[0], parts[1]);
            case 3:
                return new Locale(parts[0], parts[1], parts[2]);
            default:
                return null;
        }
    }

    @Extension
    public static class DescriptorImpl extends UserPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.UserLocaleProperty_DisplayName();
        }

        @Override
        public UserProperty newInstance(User user) {
            return new UserLocaleProperty(null);
        }

    }
}
