/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer
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
package hudson.plugins.clearcase;

import hudson.FilePath;
import hudson.Util;
import hudson.plugins.clearcase.util.PathUtil;
import hudson.util.ArgumentListBuilder;
import hudson.util.VariableResolver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.lang.StringUtils;

public class ClearToolSnapshot extends ClearToolExec {

    private String optionalMkviewParameters;
    private String viewPath;
    
    public ClearToolSnapshot(VariableResolver variableResolver, ClearToolLauncher launcher) {
        super(variableResolver, launcher);
    }

    public ClearToolSnapshot(VariableResolver variableResolver, ClearToolLauncher launcher, String optionalParameters) {
        this(variableResolver, launcher);
        this.optionalMkviewParameters = optionalParameters;
    }

    /**
     * To set the config spec of a snapshot view, you must be in or under the snapshot view root directory.
     * @see http://www.ipnom.com/ClearCase-Commands/setcs.html
     */
    public void setcs(String viewName, String configSpec) throws IOException,
                                                                 InterruptedException {
        String path = getViewPath(viewName);
        FilePath workspace = launcher.getWorkspace();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("setcs");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (configSpec != null) {
            FilePath configSpecFile = workspace.createTextTempFile("configspec", ".txt", configSpec);
            String csLocation = ".." + File.separatorChar + configSpecFile.getName();
            csLocation = PathUtil.convertPathForOS(csLocation, launcher.getLauncher());
            cmd.add(csLocation);
            InputStream in = new ByteArrayInputStream("yes".getBytes());
            // Answer yes if there are load rules to remove
            launcher.run(cmd.toCommandArray(), in, baos, workspace.child(path));
            configSpecFile.delete();
        } else {
            cmd.add("-current");
            launcher.run(cmd.toCommandArray(), null, baos, workspace.child(path));
        }
        
        BufferedReader reader = new BufferedReader( new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
        baos.close();
        StringBuilder builder = new StringBuilder();
        for(String line; (line = reader.readLine()) != null ; ) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(line);
        }
        reader.close();
        
        if (builder.toString().contains("cleartool: Warning: An update is already in progress for view")) {
            throw new IOException("View update failed: " + builder.toString());
        }
    }

    private String getViewPath(String viewName) {
        if (StringUtils.isNotEmpty(this.viewPath)) {
            return viewPath;
        } else {
            return viewName;
        }
    }

    /**
     * To set the config spec of a snapshot view, you must be in or under the snapshot view root directory.
     * @see http://www.ipnom.com/ClearCase-Commands/setcs.html
     */
    public void setcsStream(String viewName) throws IOException,
                                                     InterruptedException {
        String path = getViewPath(viewName);
        FilePath workspace = launcher.getWorkspace();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("setcs");
        cmd.add("-stream");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        launcher.run(cmd.toCommandArray(), null, baos, workspace.child(path));
        BufferedReader reader = new BufferedReader( new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
        baos.close();
        String line = reader.readLine();
        StringBuilder builder = new StringBuilder();
        while (line != null) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(line);
            line = reader.readLine();
        }
        reader.close();
        
        if (builder.toString().contains("cleartool: Warning: An update is already in progress for view")) {
            throw new IOException("View update failed: " + builder.toString());
        }
    }

    public void mkview(String viewName, String streamSelector) throws IOException, InterruptedException {
        String path = getViewPath(viewName);
        boolean isOptionalParamContainsHost = false;
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("mkview");
        cmd.add("-snapshot");
        if (streamSelector != null) {
            cmd.add("-stream");
            cmd.add(streamSelector);
        }
        cmd.add("-tag");
        cmd.add(viewName);
        
        if ((optionalMkviewParameters != null) && (optionalMkviewParameters.length() > 0)) {
            String variabledResolvedParams = Util.replaceMacro(optionalMkviewParameters, this.variableResolver);
            cmd.addTokenized(variabledResolvedParams);
            isOptionalParamContainsHost = optionalMkviewParameters.contains("-host");
        }
        
        if (!isOptionalParamContainsHost) {
            cmd.add(path);
        }
        
        launcher.run(cmd.toCommandArray(), null, null, null);
    }
    
    public void mkview(String viewName, String streamSelector, String defaultStorageDir) throws IOException, InterruptedException {
    	launcher.getListener().fatalError("Snapshot view does not support mkview (String, String)");
    }

    public void rmview(String viewName) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmview");
        cmd.add("-force");
        cmd.add(viewName);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
        launcher.run(cmd.toCommandArray(), null, baos, null);
        BufferedReader reader = new BufferedReader( new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
        baos.close();
        String line = reader.readLine();
        StringBuilder builder = new StringBuilder();
        while (line != null) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(line);
            line = reader.readLine();
        }
        reader.close();
        
        if (builder.toString().contains("cleartool: Error")) {
            throw new IOException("Failed to remove view: " + builder.toString());
        }
        
        
        FilePath viewFilePath = launcher.getWorkspace().child(viewName);
        if (viewFilePath.exists()) {
            launcher.getListener().getLogger().println(
                                                       "Removing view folder as it was not removed when the view was removed.");
            viewFilePath.deleteRecursive();
        }
    }

    public void update(String viewName, String loadRules) throws IOException, InterruptedException {
        String path = getViewPath(viewName);
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("update");
        cmd.add("-force");
        cmd.add("-overwrite");
        cmd.add("-log", "NUL");
        if (loadRules != null) {
            cmd.add("-add_loadrules");
            // We're assuming loadRules already has a leading slash or backslash, since the only place
            // I can find where it's called like this is in UcmSnapshotCheckoutAction, where we
            // definitely put the slash/backslash.
            for(String loadRule : loadRules.split(";")) {
                if (loadRule.contains(" ")) {
                    cmd.addQuoted(loadRule);
                } else {
                    cmd.add(loadRule);
                }
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        launcher.run(cmd.toCommandArray(), null, baos, launcher.getWorkspace().child(path));
        BufferedReader reader = new BufferedReader( new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
        baos.close();
        String line = reader.readLine();
        StringBuilder builder = new StringBuilder();
        while (line != null) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(line);
            line = reader.readLine();
        }
        reader.close();
        
        if (builder.toString().contains("cleartool: Warning: An update is already in progress for view")) {
            throw new IOException("View update failed: " + builder.toString());
        }
    }


    @Override
    protected FilePath getRootViewPath(ClearToolLauncher launcher) {
        return launcher.getWorkspace();
    }

    public void startView(String viewTag) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Snapshot view does not support startview");
    }
    
    public void syncronizeViewWithStream(String viewName, String stream) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Snapshot view does not support syncronize");
    }

    public void setViewPath(String viewPath) {
        this.viewPath = viewPath;
    }
}
