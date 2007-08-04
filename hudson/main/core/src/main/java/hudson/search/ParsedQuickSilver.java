package hudson.search;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.beans.Introspector;

/**
 * Parsed {@link QuickSilver}s so that {@link SearchIndex} can be easily created.
 * One instance per one class.
 *
 * @author Kohsuke Kawaguchi
 */
final class ParsedQuickSilver {
    private static final Map<Class,ParsedQuickSilver> TABLE = new HashMap<Class,ParsedQuickSilver>();

    synchronized static ParsedQuickSilver get(Class<? extends SearchableModelObject> clazz) {
        ParsedQuickSilver pqs = TABLE.get(clazz);
        if(pqs==null)
            TABLE.put(clazz,pqs = new ParsedQuickSilver(clazz));
        return pqs;
    }

    private final List<Getter> getters = new ArrayList<Getter>();

    private ParsedQuickSilver(Class<? extends SearchableModelObject> clazz) {
        QuickSilver qs;

        for (Method m : clazz.getMethods()) {
            qs = m.getAnnotation(QuickSilver.class);
            if(qs!=null) {
                String url = stripGetPrefix(m);
                if(qs.value().length==0)
                    getters.add(new MethodGetter(url,splitName(url),m));
                else {
                    for (String name : qs.value())
                        getters.add(new MethodGetter(url,name,m));
                }
            }
        }
        for (Field f : clazz.getFields()) {
            qs = f.getAnnotation(QuickSilver.class);
            if(qs!=null) {
                if(qs.value().length==0)
                    getters.add(new FieldGetter(f.getName(),splitName(f.getName()),f));
                else {
                    for (String name : qs.value())
                        getters.add(new FieldGetter(f.getName(),name,f));
                }
            }
        }
    }

    /**
     * Convert names like "abcDefGhi" to "abc def ghi".
     */
    private String splitName(String url) {
        StringBuilder buf = new StringBuilder(url.length()+5);
        for(String token : url.split("(?<=[a-z])(?=[A-Z])")) {
            if(buf.length()>0)  buf.append(' ');
            buf.append(Introspector.decapitalize(token));
        }
        return buf.toString();
    }

    private String stripGetPrefix(Method m) {
        String n = m.getName();
        if(n.startsWith("get"))
            n = Introspector.decapitalize(n.substring(3));
        return n;
    }


    static abstract class Getter {
        final String url;
        final String searchName;

        protected Getter(String url, String searchName) {
            this.url = url;
            this.searchName = searchName;
        }

        abstract Object get(Object obj);
    }

    static final class MethodGetter extends Getter {
        private final Method method;

        public MethodGetter(String url, String searchName, Method method) {
            super(url, searchName);
            this.method = method;
        }

        Object get(Object obj) {
            try {
                return method.invoke(obj);
            } catch (IllegalAccessException e) {
                throw toError(e);
            } catch (InvocationTargetException e) {
                Throwable x = e.getTargetException();
                if (x instanceof Error)
                    throw (Error) x;
                if (x instanceof RuntimeException)
                    throw (RuntimeException) x;
                throw new Error(e);
            }
        }
    }

    static final class FieldGetter extends Getter {
        private final Field field;

        public FieldGetter(String url, String searchName, Field field) {
            super(url, searchName);
            this.field = field;
        }

        Object get(Object obj) {
            try {
                return field.get(obj);
            } catch (IllegalAccessException e) {
                throw toError(e);
            }
        }
    }

    private static IllegalAccessError toError(IllegalAccessException e) {
        IllegalAccessError iae = new IllegalAccessError();
        iae.initCause(e);
        return iae;
    }

    public void addTo(SearchIndexBuilder builder, final Object instance) {
        for (final Getter getter : getters)
            builder.add(new SearchItem() {
                public String getSearchName() {
                    return getter.searchName;
                }

                public String getSearchUrl() {
                    return getter.url;
                }

                public SearchIndex getSearchIndex() {
                    return ((SearchableModelObject)getter.get(instance)).getSearchIndex();
                }
            });
    }
}
