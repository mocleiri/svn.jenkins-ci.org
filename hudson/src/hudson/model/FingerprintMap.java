package hudson.model;

import hudson.Util;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache of {@link Fingerprint}s.
 *
 * <p>
 * This implementation makes sure that no two {@link Fingerprint} objects
 * lie around for the same hash code, and that unused {@link Fingerprint}
 * will be adequately GC-ed to prevent memory leak.
 *
 * @author Kohsuke Kawaguchi
 */
public final class FingerprintMap {
    private final Map<String,WeakReference<Fingerprint>> core = new HashMap<String, WeakReference<Fingerprint>>();

    public Fingerprint getOrCreate(Build build, String fileName, byte[] md5sum) throws IOException {
        return getOrCreate(build,fileName, Util.toHexString(md5sum,0,16));
    }

    public Fingerprint getOrCreate(Build build, String fileName, String md5sum) throws IOException {
        assert build!=null;
        assert fileName!=null;
        return get0(build,fileName, md5sum);
    }

    public Fingerprint get(String md5sum) throws IOException {
        return get0(null,null,md5sum);
    }

    private synchronized Fingerprint get0(Build build, String fileName, String md5sum) throws IOException {
        if(md5sum.length()!=32)
            return null;    // illegal input
        md5sum = md5sum.toLowerCase();

        WeakReference<Fingerprint> wfp = core.get(md5sum);
        if(wfp!=null) {
            Fingerprint fp = wfp.get();
            if(fp!=null)
                return fp;  // found it
        }

        byte[] data = new byte[16];
        for( int i=0; i<md5sum.length(); i+=2 )
            data[i/2] = (byte)Integer.parseInt(md5sum.substring(i,i+2),16);

        Fingerprint fp = Fingerprint.load(data);
        if(fp==null) {
            if(build==null)
                return null;    // this is get with 1 arg. return "not found"

            // creates a new one
            fp = new Fingerprint(build,fileName,data);
        }

        core.put(md5sum,new WeakReference<Fingerprint>(fp));
        return fp;
    }

}
