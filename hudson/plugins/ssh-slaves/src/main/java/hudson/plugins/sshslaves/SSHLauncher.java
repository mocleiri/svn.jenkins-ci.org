package hudson.plugins.sshslaves;

import com.trilead.ssh2.*;
import hudson.model.Descriptor;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.util.StreamTaskListener;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A computer launcher that tries to start a linux slave by opening an SSH connection and trying to find java.
 */
public class SSHLauncher extends ComputerLauncher {

    private final String host;
    private final int port;
    private final String username;
    private final String password;  // TODO obfuscate the password
    // TODO add support for key files

    private transient Connection connection;

    @DataBoundConstructor
    public SSHLauncher(String host, int port, String username, String password) {
        this.host = host;
        this.port = port == 0 ? 22 : port;
        this.username = username;
        this.password = password;
    }

    public boolean isLaunchSupported() {
        return true;
    }

    public synchronized void launch(SlaveComputer slaveComputer, StreamTaskListener listener) {
        connection = new Connection(host, port);

        try {
            // TODO open ssh connection to the host
            listener.getLogger().println("[SSH] Opening SSH connection to " + host + ":" + port);
            connection.connect();

            // TODO if using a key file, use the key file instead of password
            listener.getLogger().println("[SSH] Authenticating as " + username + "/******");
            boolean isAuthenicated = connection.authenticateWithPassword(username, password);

            if (isAuthenicated && connection.isAuthenticationComplete()) {
                listener.getLogger().println("[SSH] Authentication successful.");
            } else {
                listener.getLogger().println("[SSH] Authentication failed.");
                connection.close();
                connection = null;
                listener.getLogger().println("[SSH] Connection closed.");
                return;
            }

            listener.getLogger().println("[SSH] Checking default java version");
            String line;
            Session session = connection.openSession();
            try {
                session.execCommand("java -version");
                StreamGobbler out = new StreamGobbler(session.getStdout());
                StreamGobbler err = new StreamGobbler(session.getStderr());
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(out));

                    // TODO make sure this works with IBM JVM & JRocket

                    while (null != (line = r.readLine()) && !line.startsWith("java version \"")) {
                        listener.getLogger().println("  " + line);
                    }
                } finally {
                    out.close();
                    err.close();
                }
            } finally {
                session.close();
            }

            if (line == null || !line.startsWith("java version \"")) {
                throw new IOException("The default version of java is either unsupported version or unknown");
            }

            line = line.substring(line.indexOf('\"') + 1, line.lastIndexOf('\"'));
            listener.getLogger().println("[SSH] java version = " + line);

            // TODO check if the default java is 1.5 or newer

            // TODO if not, find a java that is or throw an error

            String fileDir = slaveComputer.getNode().getRemoteFS();
            while (fileDir.endsWith("/")) {
                fileDir = fileDir.substring(0, fileDir.length() - 1);
            }
            String fileName = fileDir + "/slave.jar";

            listener.getLogger().println("[SSH] Starting sftp client...");
            SFTPv3Client sftpClient = null;
            try {
                sftpClient = new SFTPv3Client(connection);

                SFTPv3FileHandle fh = null;
                try {
                    // TODO decide best permissions and handle errors if exists already
                    sftpClient.mkdir(fileDir, 0700);

                    // TODO handle the file existing already
                    listener.getLogger().println("[SSH] Copying latest slave.jar...");
                    fh = sftpClient.createFile(fileName);

                    InputStream is = null;
                    try {
                        // TODO get the slave jar the correct way... this may not be working
                        is = getClass().getResourceAsStream("/WEB-INF/slave.jar");
                        byte[] buf = new byte[2048];

                        listener.getLogger().println("[SSH] Sending data...");

                        int count = 0;
                        int bufsiz = 0;
                        try {
                            while ((bufsiz = is.read(buf)) != -1) {
                                sftpClient.write(fh, (long) count, buf, 0, bufsiz);
                                count += bufsiz;
                            }
                            listener.getLogger().println("[SSH] Sent " + count + " bytes.");
                            is.close();
                        } catch (Exception e) {
                            listener.getLogger().println("[SSH] Error writing to remote file");
                            e.printStackTrace(listener.getLogger());
                        }
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                } catch (Exception e) {
                    listener.getLogger().println("[SSH] Error creating file");
                    e.printStackTrace(listener.getLogger());
                }
            } finally {
                if (sftpClient != null) {
                    sftpClient.close();
                }
            }

            // TODO launch the slave.jar with a command that removes it once it terminates

            // TODO set the channel to the STD I/O of the ssh connection

            throw new IOException("Implementation not yet finished");
        } catch (IOException e) {
            e.printStackTrace(listener.getLogger());
            connection.close();
            connection = null;
            listener.getLogger().println("[SSH] Connection closed.");
        }


    }

    public synchronized void afterDisconnect(SlaveComputer slaveComputer, StreamTaskListener listener) {
        if (connection != null) {

            // TODO remove slave.jar

            connection.close();
            connection = null;
            listener.getLogger().println("[SSH] Connection closed.");
        }
        super.afterDisconnect(slaveComputer, listener);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Descriptor<ComputerLauncher> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final Descriptor<ComputerLauncher> DESCRIPTOR = new DescriptorImpl();

    private static class DescriptorImpl extends Descriptor<ComputerLauncher> {

        protected DescriptorImpl() {
            super(SSHLauncher.class);
        }

        public String getDisplayName() {
            return "Launch slave agents on Linux machines via SSH";
        }
    }
}
