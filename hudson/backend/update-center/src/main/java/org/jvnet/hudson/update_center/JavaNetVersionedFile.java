package org.jvnet.hudson.update_center;

import org.kohsuke.jnt.JNFile;
import org.kohsuke.jnt.JNFileFolder;
import org.kohsuke.jnt.ProcessingException;

/**
 * {@link VersionedFile} on java.net
 *
 * @author Kohsuke Kawaguchi
 */
public class JavaNetVersionedFile extends VersionedFile {
    final JNFile file;

    public JavaNetVersionedFile(JNFile file) {
        super(file.getURL(),parseVersion(file),file.getLastModified(),
	      file.getModifiedBy().getName());
        this.file = file;
    }

    public static VersionedFile findLatestFrom(JNFileFolder dir) throws ProcessingException {
        VersionedFile latest=null;

        for( JNFile file : dir.getFiles().values() ) {
            try {
                VersionedFile vf = new JavaNetVersionedFile(file);
                if(latest==null || vf.compareTo(latest)>0) {
                    latest = vf;
                }
            } catch (IllegalArgumentException e) {
                System.out.println("   Ignoring "+file.getName());
            }
        }

        return latest;
    }

    public static VersionedFile findSecondLatestFrom(JNFileFolder dir) throws ProcessingException {
        VersionedFile latest=findLatestFrom(dir);
	VersionedFile secondLatest=null;
	
        for( JNFile file : dir.getFiles().values() ) {
            try {
                VersionedFile vf = new JavaNetVersionedFile(file);
                if(secondLatest==null ||
		   (vf.compareTo(secondLatest) > 0)
		   && vf.compareTo(latest) < 0) {
                    secondLatest = vf;
                }
            } catch (IllegalArgumentException e) {
                System.out.println("   Ignoring "+file.getName());
            }
        }

        return secondLatest;
    }

    private static VersionNumber parseVersion(JNFile file) {
        String n = file.getName();
        if(n.contains(" "))  n = n.substring(n.lastIndexOf(' ')+1);
        return new VersionNumber(n);
    }
}
