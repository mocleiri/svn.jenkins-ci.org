import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Launches <tt>hudson.war</tt> from JNLP.
 *
 * @author Kohsuke Kawaguchi
 */
public class JNLPMain {
    public static void main(String[] args) throws Exception {
        // don't know if this is really necessary, but jnlp-agent benefited from this,
        // so I'm doing it here, too.
        System.setSecurityManager(null);

        boolean headlessMode = Boolean.getBoolean("hudson.webstart.headless");
        if (!headlessMode) {
            // launch GUI to display output
            setUILookAndFeel();
            new MainDialog().setVisible(true);
        }

        Main.main(args);
    }

    /**
     * Sets to the platform native look and feel.
     *
     * see http://javaalmanac.com/egs/javax.swing/LookFeelNative.html
     */
    public static void setUILookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (InstantiationException e) {
        } catch (ClassNotFoundException e) {
        } catch (UnsupportedLookAndFeelException e) {
        } catch (IllegalAccessException e) {
        }
    }
}
