/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Optional interface for {@link Action}s that are attached
 * to {@link AbstractProject} (through {@link JobProperty#getJobActions(Job)}),
 * which allows plugins to define additional permalinks in the project.
 *
 * <p>
 * Permalinks are listed together in the UI for better ease of use,
 * plus other plugins can use this information elsewhere (for example,
 * a plugin to download an artifact from one of the permalinks.)
 *
 * @author Kohsuke Kawaguchi
 * @since 1.253
 * @see JobProperty
 */
public interface PermalinkProjectAction extends Action {
    /**
     * Gets the permalinks defined for this project.
     *
     * <p>
     * Because {@link Permalink} is a strategy-pattern object,
     * this method should normally return a pre-initialzied
     * read-only static list object.  
     *
     * @return
     *      can be empty, but never null.
     */
    List<Permalink> getPermalinks();

    /**
     * Permalink as a strategy pattern.
     */
    public static abstract class Permalink {
        /**
         * String to be displayed in the UI, such as "Last successful build".
         * The convention is to upper case the first letter.
         */
        public abstract String getDisplayName();

        /**
         * ID that uniquely identifies this permalink.
         *
         * <p>
         * The is also used as an URL token to represent the permalink.
         * This becomes the part of the permanent URL.
         *
         * <p>
         * The expected format is the camel case,
         * such as "lastSuccessfulBuild".
         */
        public abstract String getId();

        /**
         * Resolves the permalink to a build.
         *
         * @return null
         *      if the target of the permalink doesn't exist.
         */
        public abstract Run<?,?> resolve(Job<?,?> job);

        /**
         * List of {@link Permalink}s that are built into Hudson.
         */
        public static final List<Permalink> BUILTIN = new CopyOnWriteArrayList<Permalink>();

        static {
            BUILTIN.add(new Permalink() {
                public String getDisplayName() {
                    return Messages.Permalink_LastBuild();
                }

                public String getId() {
                    return "lastBuild";
                }

                public Run<?,?> resolve(Job<?,?> job) {
                    return job.getLastBuild();
                }
            });

            BUILTIN.add(new Permalink() {
                public String getDisplayName() {
                    return Messages.Permalink_LastStableBuild();
                }

                public String getId() {
                    return "lastStableBuild";
                }

                public Run<?,?> resolve(Job<?,?> job) {
                    return job.getLastStableBuild();
                }
            });

            BUILTIN.add(new Permalink() {
                public String getDisplayName() {
                    return Messages.Permalink_LastSuccessfulBuild();
                }

                public String getId() {
                    return "lastSuccessfulBuild";
                }

                public Run<?,?> resolve(Job<?,?> job) {
                    return job.getLastSuccessfulBuild();
                }
            });

            BUILTIN.add(new Permalink() {
                public String getDisplayName() {
                    return Messages.Permalink_LastFailedBuild();
                }

                public String getId() {
                    return "lastFailedBuild";
                }

                public Run<?,?> resolve(Job<?,?> job) {
                    return job.getLastFailedBuild();
                }
            });
        }
    }
}
