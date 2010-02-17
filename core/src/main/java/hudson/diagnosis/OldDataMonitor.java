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

import hudson.model.AdministrativeMonitor;
import hudson.model.Hudson;
import hudson.Extension;
import hudson.model.Saveable;
import hudson.util.VersionNumber;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
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
                else            odm.data.put(obj, new VersionRange(version));
            } catch (IllegalArgumentException ex) {
                LOGGER.log(Level.WARNING, "Bad parameter given to OldDataMonitor", ex);
            }
        }
    }

    public static class VersionRange {
        private static VersionNumber currentVersion = new VersionNumber(Hudson.VERSION);

        VersionNumber min, max;
        boolean single = true;

        public VersionRange(String version) {
            min = max = new VersionNumber(version);
        }

        public void add(String version) {
            VersionNumber ver = new VersionNumber(version);
            if (ver.isOlderThan(min)) { min = ver; single = false; }
            if (ver.isNewerThan(max)) { max = ver; single = false; }
        }

        @Override
        public String toString() {
            return min.toString() + (single ? "" : " - " + max.toString());
        }

        /**
         * Does this version range contain a version more than the given number of releases ago?
         * @param threshold Number of releases
         * @return True if the major version# differs or the minor# differs by >= threshold
         */
        public boolean isOld(int threshold) {
            if (currentVersion.digit(0) > min.digit(0)
                    || (currentVersion.digit(0) == min.digit(0)
                        && currentVersion.digit(1) - min.digit(1) >= threshold))
                return true;
            return false;
        }
    }

    /**
     * Sorted list of unique max-versions in the data set.  For select list in jelly.
     */
    public synchronized Iterator<VersionNumber> getVersionList() {
        TreeSet<VersionNumber> set = new TreeSet<VersionNumber>();
        for (VersionRange vr : data.values()) set.add(vr.max);
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
            if (thruVer == null || !entry.getValue().max.isNewerThan(thruVer)) {
                entry.getKey().save();
                it.remove();
            }
        }
        return HttpResponses.forwardToPreviousPage();
    }
}
