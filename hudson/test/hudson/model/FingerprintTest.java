package hudson.model;

import junit.framework.TestCase;
import hudson.model.Fingerprint.RangeSet;

/**
 * @author Kohsuke Kawaguchi
 */
public class FingerprintTest extends TestCase {
    public void test() {
        RangeSet rs = new RangeSet();
        assertFalse(rs.includes(0));
        assertFalse(rs.includes(3));
        assertFalse(rs.includes(5));

        rs.add(3);
        assertFalse(rs.includes(2));
        assertTrue(rs.includes(3));
        assertFalse(rs.includes(4));
        assertEquals("[3,4)",rs.toString());

        rs.add(4);
        assertFalse(rs.includes(2));
        assertTrue(rs.includes(3));
        assertTrue(rs.includes(4));
        assertFalse(rs.includes(5));
        assertEquals("[3,5)",rs.toString());

        rs.add(10);
        assertEquals("[3,5),[10,11)",rs.toString());

        rs.add(9);
        assertEquals("[3,5),[9,11)",rs.toString());

        rs.add(6);
        assertEquals("[3,5),[6,7),[9,11)",rs.toString());

        rs.add(5);
        assertEquals("[3,7),[9,11)",rs.toString());
    }
}
