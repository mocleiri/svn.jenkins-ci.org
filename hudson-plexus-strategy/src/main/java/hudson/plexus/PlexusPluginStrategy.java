package hudson.plexus;

import hudson.ClassicPluginStrategy;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Items;
import hudson.model.ManagementLink;
import hudson.model.listeners.RunListener;
import hudson.scm.ChangeLogAnnotator;
import hudson.tasks.DynamicLabeler;
import hudson.tasks.LabelFinder;
import hudson.tasks.MailAddressResolver;
import hudson.tasks.UserNameResolver;
import hudson.widgets.Widget;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.discovery.DefaultComponentDiscoverer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.component.repository.exception.ComponentRepositoryException;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.JVM;

public class PlexusPluginStrategy extends ClassicPluginStrategy {

	private static Logger LOGGER = Logger.getLogger(PlexusPluginStrategy.class
			.getName());

	private ClassWorld classWorld;
	private DefaultPlexusContainer plexusContainer;

	public PlexusPluginStrategy(PluginManager owner) {
		super(owner);

		ReflectionProvider reflectionProvider = new JVM().bestReflectionProvider();
		Hudson.XSTREAM.registerConverter(new PlexusConverter(Hudson.XSTREAM
				.getMapper(), reflectionProvider), -15);
		Items.XSTREAM.registerConverter(new PlexusConverter(Items.XSTREAM
				.getMapper(), reflectionProvider), -15);

		try {
			classWorld = new ClassWorld("hudson-core", getClass()
					.getClassLoader());

			ContainerConfiguration cc = new DefaultContainerConfiguration()
					.addComponentDiscoverer(new DefaultComponentDiscoverer())
					.setClassWorld(classWorld).setRealm(
							classWorld.getRealm("hudson-core"));

			plexusContainer = new DefaultPlexusContainer(cc);
		} catch (NoSuchRealmException e) {
			LOGGER
					.log(Level.SEVERE, "Unable to initialize Plexus container",
							e);
		} catch (PlexusContainerException e) {
			LOGGER
					.log(Level.SEVERE, "Unable to initialize Plexus container",
							e);
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public void initializeComponents(PluginWrapper plugin) {
		try {
			ClassRealm realm = getClassRealm(plugin.getShortName());

			List<Descriptor> hudsonDescriptors = plexusContainer.lookupList(
					Descriptor.class.getName(), realm);

			for (Descriptor descriptor : hudsonDescriptors) {
				ExtensionRegistration.register(descriptor);
			}

			for (ChangeLogAnnotator annotator : (List<ChangeLogAnnotator>) plexusContainer
					.lookupList(ChangeLogAnnotator.class.getName(), realm)) {
				annotator.register();
			}
			for (MailAddressResolver resolver : (List<MailAddressResolver>) plexusContainer
					.lookupList(MailAddressResolver.class.getName(), realm)) {
				MailAddressResolver.LIST.add(resolver);
			}
			for (DynamicLabeler resolver : (List<DynamicLabeler>) plexusContainer
					.lookupList(DynamicLabeler.class.getName(), realm)) {
				LabelFinder.LABELERS.add(resolver);
			}
			for (ManagementLink link : (List<ManagementLink>) plexusContainer
					.lookupList(ManagementLink.class.getName(), realm)) {
				ManagementLink.LIST.add(link);
			}

			for (UserNameResolver link : (List<UserNameResolver>) plexusContainer
					.lookupList(UserNameResolver.class.getName(), realm)) {
				UserNameResolver.LIST.add(link);
			}
			for (RunListener link : (List<RunListener>) plexusContainer
					.lookupList(RunListener.class.getName(), realm)) {
				RunListener.LISTENERS.add(link);
			}
			for (Widget widget : (List<Widget>) plexusContainer.lookupList(
					Widget.class.getName(), realm)) {
				Hudson.getInstance().getWidgets().add(widget);
			}
		} catch (NoSuchRealmException e) {
			throw new IllegalStateException("shouldn't happen", e);
		} catch (ComponentLookupException e) {
			LOGGER.log(Level.WARNING,
					"Could not initialize plexus components for "
							+ plugin.getShortName(), e);
			e.printStackTrace();
		}

	}

	public void startPlugin(PluginWrapper plugin) throws Exception {
		addPluginToPlexusContainer(plugin);
		super.startPlugin(plugin);
	}

	public ClassRealm getClassRealm(String plugin) throws NoSuchRealmException {
		return classWorld.getRealm(plugin);
	}

	void addPluginToPlexusContainer(PluginWrapper plugin) {
		if (plexusContainer == null) {
			// container initialization failed previously
			return;
		}
		try {
			classWorld.newRealm(plugin.getShortName(), plugin.classLoader);
			ClassRealm realm = classWorld.getRealm(plugin.getShortName());
			plexusContainer.discoverComponents(realm);
		} catch (DuplicateRealmException e) {
			LOGGER.log(Level.SEVERE, "Unable to add plugin "
					+ plugin.getShortName() + " to Plexus container", e);
		} catch (NoSuchRealmException e) {
			LOGGER.log(Level.SEVERE, "Unable to add plugin "
					+ plugin.getShortName() + " to Plexus container", e);
		} catch (PlexusConfigurationException e) {
			LOGGER.log(Level.SEVERE, "Unable to add plugin "
					+ plugin.getShortName() + " to Plexus container", e);
		} catch (ComponentRepositoryException e) {
			LOGGER.log(Level.SEVERE, "Unable to add plugin "
					+ plugin.getShortName() + " to Plexus container", e);
		}
	}

	public PlexusContainer getPlexusContainer() {
		return plexusContainer;
	}

}
