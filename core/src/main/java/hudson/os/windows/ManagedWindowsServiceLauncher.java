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
package hudson.os.windows;

import hudson.lifecycle.WindowsSlaveInstaller;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.util.StreamTaskListener;
import hudson.util.Secret;
import hudson.util.jna.DotNet;
import jcifs.smb.SmbFile;
import org.apache.commons.io.IOUtils;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JIDefaultAuthInfoImpl;
import org.jinterop.dcom.core.JISession;
import org.kohsuke.stapler.DataBoundConstructor;
import org.jvnet.hudson.wmi.WMI;
import org.jvnet.hudson.wmi.SWbemServices;
import org.jvnet.hudson.wmi.Win32Service;
import static org.jvnet.hudson.wmi.Win32Service.Win32OwnProcess;
import org.dom4j.io.SAXReader;
import org.dom4j.Document;
import org.dom4j.DocumentException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.PrintStream;
import java.net.UnknownHostException;

/**
 * Windows slave installed/managed as a service entirely remotely
 *
 * @author Kohsuke Kawaguchi
 */
public class ManagedWindowsServiceLauncher extends ComputerLauncher {
    /**
     * "[DOMAIN\\]USERNAME" to follow the Windows convention.
     */
    public final String userName;
    
    public final Secret password;

    @DataBoundConstructor
    public ManagedWindowsServiceLauncher(String userName, String password) {
        this.userName = userName;
        this.password = Secret.fromString(password);
    }

    private JIDefaultAuthInfoImpl createAuth() {
        String[] tokens = userName.split("\\\\");
        if(tokens.length==2)
            return new JIDefaultAuthInfoImpl(tokens[0], tokens[1], password.toString());
        return new JIDefaultAuthInfoImpl("", userName, password.toString());
    }

    public void launch(SlaveComputer computer, StreamTaskListener listener) throws IOException, InterruptedException {
        try {
            PrintStream logger = listener.getLogger();

            logger.println("Connecting to "+computer.getName());
            JIDefaultAuthInfoImpl auth = createAuth();
            JISession session = JISession.createSession(auth);
            session.setGlobalSocketTimeout(60000);
            SWbemServices services = WMI.connect(session, computer.getName());

            Win32Service slaveService = services.getService("hudsonslave");
            if(slaveService==null) {
                logger.println("Installing the Hudson slave service");
                if(!DotNet.isInstalled(2,0,computer.getName(), auth)) {
                    // abort the launch
                    logger.println(".NET Framework 2.0 or later is required to run a Hudson slave as a Windows service");
                    return;
                }
    
                String path = computer.getNode().getRemoteFS();
                SmbFile remoteRoot = new SmbFile("smb://" + computer.getName() + "/" + path.replace('\\', '/').replace(':', '$'));

                // copy exe
                logger.println("Copying hudson-slave.exe");
                copyAndClose(getClass().getResource("/windows-service/hudson.exe").openStream(),
                        new SmbFile(remoteRoot,"hudson-slave.exe").getOutputStream());

                // copy slave.jar
                logger.println("Copying slave.jar");
                copyAndClose(Hudson.getInstance().getJnlpJars("slave.jar").getURL().openStream(),
                        new SmbFile(remoteRoot,"slave.jar").getOutputStream());

                // copy hudson-slave.xml
                logger.println("Copying hudson-slave.xml");
                String xml = WindowsSlaveInstaller.generateSlaveXml("java.exe",Hudson.getInstance().getRootUrl()+computer.getUrl()+"slave-agent.jnlp");
                copyAndClose(new ByteArrayInputStream(xml.getBytes("UTF-8")),
                        new SmbFile(remoteRoot,"hudson-slave.xml").getOutputStream());

                // install it as a service
                logger.println("Registering the service");
                Document dom = new SAXReader().read(new StringReader(xml));
                Win32Service svc = services.Get("Win32_Service").cast(Win32Service.class);
                int r = svc.Create(
                        dom.selectSingleNode("/service/id").getText(),
                        dom.selectSingleNode("/service/name").getText(),
                        "java "+dom.selectSingleNode("/service/arguments").getText(),
                        Win32OwnProcess, 0, "Auto", true);
                if(r!=0) {
                    listener.error("Failed to create a service. Error code="+r);
                    return;
                }
                slaveService = services.getService("hudsonslave");
            }

            logger.println("Starting the service");
            slaveService.StartService();
        } catch (JIException e) {
            e.printStackTrace(listener.error(e.getMessage()));
        } catch (DocumentException e) {
            e.printStackTrace(listener.error(e.getMessage()));
        }
    }

    @Override
    public void afterDisconnect(SlaveComputer computer, StreamTaskListener listener) {
        try {
            JIDefaultAuthInfoImpl auth = createAuth();
            JISession session = JISession.createSession(auth);
            session.setGlobalSocketTimeout(60000);
            SWbemServices services = WMI.connect(session, computer.getName());
            Win32Service slaveService = services.getService("hudsonslave");
            if(slaveService!=null) {
                listener.getLogger().println("Stopping the service");
                slaveService.StopService();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace(listener.error(e.getMessage()));
        } catch (JIException e) {
            e.printStackTrace(listener.error(e.getMessage()));
        }
    }

    private static void copyAndClose(InputStream in, OutputStream out) {
        try {
            IOUtils.copy(in,out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    public Descriptor<ComputerLauncher> getDescriptor() {
        return DescriptorImpl.INSTANCE;
    }

    public static class DescriptorImpl extends Descriptor<ComputerLauncher> {
        public static final DescriptorImpl INSTANCE = new DescriptorImpl();

        public String getDisplayName() {
            return "Let Hudson control this Windows slave as a Windows service";
        }
    }
}
