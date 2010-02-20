/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Alan Harder
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
package hudson.diagnosis;

import hudson.XmlFile;
import hudson.model.AdministrativeMonitor;
import hudson.model.Hudson;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.Saveable;
import hudson.model.listeners.ItemListener;
import hudson.model.listeners.RunListener;
import hudson.model.listeners.SaveableListener;
import hudson.util.VersionNumber;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;

/**
 * Tracks whether any data structure changes were corrected when loading XML,
 * that could be resaved to migrate that data to the new format.
 *
 * @author Alan.Harder@Sun.Com
 */
@Extension
public class OldDataMonitor extends AdministrativeMonitor {
    private static Logger LOGGER = Logger.getLogger(OldDataMonitor.class.getName());

    private HashMap<Saveable,VersionRange> data = new HashMap<Saveable,VersionRange>();

    public OldDataMonitor() {
        super("OldData");
    }

    @Override
    public String getDisplayName() {
        return Messages.OldDataMonitor_DisplayName();
    }

    public boolean isActivated() {
        return !data.isEmpty();
    }

    public synchronized Map<Saveable,VersionRange> getData() {
        return Collections.unmodifiableMap(data);
    }

    private synchronized void remove(Saveable obj) {
        data.remove(obj);
        if (obj instanceof Job<?,?>)
            for (Run r : ((Job<?,?>)obj).getBuilds())
                data.remove(r);
    }

    // Listeners to remove data here if resaved or deleted in regular Hudson usage

    @Extension
    public static final SaveableListener changeListener = new SaveableListener() {
        @Override
        public void onChange(Saveable obj, XmlFile file) {
            ((OldDataMonitor)Hudson.getInstance().getAdministrativeMonitor("OldData")).remove(obj);
        }
    };

    @Extension
    public static final ItemListener itemDeleteListener = new ItemListener() {
        @Override
        public void onDeleted(Item item) {
            ((OldDataMonitor)Hudson.getInstance().getAdministrativeMonitor("OldData")).remove(item);
        }
    };

    @Extension
    public static final RunListener<Run> runDeleteListener = new RunListener<Run>(Run.class) {
        @Override
        public void onDeleted(Run run) {
            ((OldDataMonitor)Hudson.getInstance().getAdministrativeMonitor("OldData")).remove(run);
        }
    };

    /**
     * Inform monitor that some data in a deprecated format has been loaded,
     * and converted in-memory to a new structure.
     * @param obj Saveable object; calling save() on this object will persist
     *            the data in its new format to disk.
     * @param version Hudson release when the data structure changed.
     */
    public static void report(Saveable obj, String version) {
        OldDataMonitor odm = (OldDataMonitor)Hudson.getInstance().getAdministrativeMonitor("OldData");
        synchronized (odm) {
            try {
                VersionRange vr = odm.data.get(obj);
                if (vr != null) vr.add(version);
                else            odm.data.put(obj, new VersionRange(version, null));
            } catch (IllegalArgumentException ex) {
                LOGGER.log(Level.WARNING, "Bad parameter given to OldDataMonitor", ex);
            }
        }
    }

    /**
     * Inform monitor that some unreadable data was found while loading.
     * @param obj Saveable object; calling save() on this object will discard the unreadable data.
     * @param errors Exception(s) thrown while loading, regarding the unreadable classes/fields.
     */
    public static void report(Saveable obj, Collection<Throwable> errors) {
        StringBuilder buf = new StringBuilder();
        int i = 0;
        for (Throwable e : errors) {
            if (++i > 1) buf.append(", ");
            buf.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
        }
        OldDataMonitor odm = (OldDataMonitor)Hudson.getInstance().getAdministrativeMonitor("OldData");
        synchronized (odm) {
            VersionRange vr = odm.data.get(obj);
            if (vr != null) vr.extra = buf.toString();
            else            odm.data.put(obj, new VersionRange(null, buf.toString()));
        }
    }

    public static class VersionRange {
        private static VersionNumber currentVersion = new VersionNumber(Hudson.VERSION);

        VersionNumber min, max;
        boolean single = true;
        public String extra;

        public VersionRange(String version, String extra) {
            min = max = version != null ? new VersionNumber(version) : null;
            this.extra = extra;
        }

        public void add(String version) {
            VersionNumber ver = new VersionNumber(version);
            if (min==null) { min = max = ver; }
            else {
                if (ver.isOlderThan(min)) { min = ver; single = false; }
                if (ver.isNewerThan(max)) { max = ver; single = false; }
            }
        }

        @Override
        public String toString() {
            return min==null ? "" : min.toString() + (single ? "" : " - " + max.toString());
        }

        /**
         * Does this version range contain a version more than the given number of releases ago?
         * @param threshold Number of releases
         * @return True if the major version# differs or the minor# differs by >= threshold
         */
        public boolean isOld(int threshold) {
            if (min!=null && (currentVersion.digit(0) > min.digit(0)
                              || (currentVersion.digit(0) == min.digit(0)
                                  && currentVersion.digit(1) - min.digit(1) >= threshold)))
                return true;
            return false;
        }
    }

    /**
     * Sorted list of unique max-versions in the data set.  For select list in jelly.
     */
    public synchronized Iterator<VersionNumber> getVersionList() {
        TreeSet<VersionNumber> set = new TreeSet<VersionNumber>();
        for (VersionRange vr : data.values())
            if (vr.max!=null) set.add(vr.max);
        return set.iterator();
    }

    /**
     * Depending on whether the user said "yes" or "no", send him to the right place.
     */
    public HttpResponse doAct(StaplerRequest req, StaplerResponse rsp) throws IOException {
        if(req.hasParameter("no")) {
            disable(true);
            return HttpResponses.redirectViaContextPath("/manage");
        } else {
            return new HttpRedirect("manage");
        }
    }

    /**
     * Save all or some of the files to persist data in the new forms.
     * Remove those items from the data map.
     */
    public synchronized HttpResponse doUpgrade(StaplerRequest req, StaplerResponse rsp) throws IOException {
        String thruVerParam = req.getParameter("thruVer");
        VersionNumber thruVer = thruVerParam.equals("all") ? null : new VersionNumber(thruVerParam);
        for (Iterator<Map.Entry<Saveable,VersionRange>> it = data.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Saveable,VersionRange> entry = it.next();
            VersionNumber version = entry.getValue().max;
            if (version != null && (thruVer == null || !version.isNewerThan(thruVer))) {
                entry.getKey().save();
                it.remove();
            }
        }
        return HttpResponses.forwardToPreviousPage();
    }

    /**
     * Save all files containing only unreadable data (no data upgrades), which discards this data.
     * Remove those items from the data map.
     */
    public synchronized HttpResponse doDiscard(StaplerRequest req, StaplerResponse rsp) throws IOException {
        for (Iterator<Map.Entry<Saveable,VersionRange>> it = data.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Saveable,VersionRange> entry = it.next();
            if (entry.getValue().max == null) {
                entry.getKey().save();
                it.remove();
            }
        }
        return HttpResponses.forwardToPreviousPage();
    }
}
