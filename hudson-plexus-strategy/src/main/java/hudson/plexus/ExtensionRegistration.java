package hudson.plexus;

import hudson.maven.MavenReporterDescriptor;
import hudson.maven.MavenReporters;
import hudson.model.Descriptor;
import hudson.model.Items;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Jobs;
import hudson.model.ParameterDefinition;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.UserProperties;
import hudson.model.UserPropertyDescriptor;
import hudson.model.ParameterDefinition.ParameterDescriptor;
import hudson.node_monitors.AbstractNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import hudson.scm.RepositoryBrowser;
import hudson.scm.RepositoryBrowsers;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMS;
import hudson.security.AuthorizationStrategy;
import hudson.security.SecurityRealm;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.RetentionStrategy;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.BuildWrappers;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.triggers.TriggerDescriptor;
import hudson.triggers.Triggers;

import java.util.logging.Logger;

public class ExtensionRegistration {

	private static Logger log = Logger.getLogger(ExtensionRegistration.class
			.getName());

	public static void register(Descriptor descriptor) {
		if (descriptor instanceof BuildStepDescriptor) {
			if (Builder.class.isAssignableFrom(descriptor.clazz)) {
				BuildStep.BUILDERS.add(descriptor);
			} else if (Publisher.class.isAssignableFrom(descriptor.clazz)) {
				BuildStep.PUBLISHERS.add(descriptor);
			}
		} else if (descriptor instanceof BuildWrapperDescriptor) {
			BuildWrappers.WRAPPERS.add(descriptor);
		} else if (descriptor instanceof AbstractNodeMonitorDescriptor) {
			NodeMonitor.LIST.add(descriptor);
		} else if (descriptor instanceof JobPropertyDescriptor) {
			Jobs.PROPERTIES.add((JobPropertyDescriptor) descriptor);
		} else if (descriptor instanceof MavenReporterDescriptor) {
			MavenReporters.LIST.add((MavenReporterDescriptor) descriptor);
		} else if (descriptor instanceof ParameterDescriptor) {
			ParameterDefinition.LIST.add(descriptor);
		} else if (descriptor instanceof SCMDescriptor) {
			SCMS.SCMS.add((SCMDescriptor) descriptor);
		} else if (descriptor instanceof TopLevelItemDescriptor) {
			Items.LIST.add((TopLevelItemDescriptor) descriptor);
		} else if (descriptor instanceof TriggerDescriptor) {
			Triggers.TRIGGERS.add((TriggerDescriptor) descriptor);
		} else if (descriptor instanceof UserPropertyDescriptor) {
			UserProperties.LIST.add((UserPropertyDescriptor) descriptor);
		} else {
			if (RepositoryBrowser.class.isAssignableFrom(descriptor.clazz)) {
				RepositoryBrowsers.LIST.add(descriptor);
			} else if (AuthorizationStrategy.class
					.isAssignableFrom(descriptor.clazz)) {
				AuthorizationStrategy.LIST.add(descriptor);
			} else if (SecurityRealm.class.isAssignableFrom(descriptor.clazz)) {
				SecurityRealm.LIST.add(descriptor);
			} else if (RetentionStrategy.class
					.isAssignableFrom(descriptor.clazz)) {
				RetentionStrategy.LIST.add(descriptor);
			} else if (ComputerLauncher.class
					.isAssignableFrom(descriptor.clazz)) {
				ComputerLauncher.LIST.add(descriptor);
			} else {
				log.warning("Unknown extension: "
						+ descriptor.getClass().getName() + "("
						+ descriptor.clazz.getName() + ")");
			}
		}
	}
}
