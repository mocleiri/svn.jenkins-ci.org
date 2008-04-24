package hudson.model;

import hudson.ExtensionPoint;

import java.util.List;

/**
 * Extension point to allow control over how Slaves are started.
 *
 * @author Stephen Connolly
 * @since 24-Apr-2008 22:12:35
 */
public interface SlaveStartMethod extends Describable<SlaveStartMethod>, ExtensionPoint {

    public static final List<Descriptor<SlaveStartMethod>> LIST = Descriptor.toList(
            Slave.JNLPStartMethod.DESCRIPTOR,
            Slave.CommandStartMethod.DESCRIPTOR
    );
}
