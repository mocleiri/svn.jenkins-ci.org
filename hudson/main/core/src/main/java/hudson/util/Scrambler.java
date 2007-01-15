package hudson.util;

import ch.ethz.ssh2.crypto.Base64;

import java.io.UnsupportedEncodingException;
import java.io.IOException;

/**
 * Used when storing passwords in configuration files.
 *
 * <p>
 * This doesn't make passwords secure, but it prevents unwanted
 * exposure to passwords, such as when one is grepping the file system
 * or looking at config files for trouble-shooting.
 *
 * @author Kohsuke Kawaguchi
 */
public class Scrambler {
    public static String scramble(String secret) {
        try {
            return new String(Base64.encode(secret.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new Error(e); // impossible
        }
    }

    public static String descramble(String scrambled) {
        try {
            return new String(Base64.decode(scrambled.toCharArray()),"UTF-8");
        } catch (IOException e) {
            return "";  // corrupted data.
        }
    }
}
