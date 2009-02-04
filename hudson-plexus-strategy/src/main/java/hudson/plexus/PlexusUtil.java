package hudson.plexus;

import hudson.PluginManager;
import hudson.model.Hudson;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.WeakHashMap;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.configuration.io.XmlPlexusConfigurationReader;

public class PlexusUtil {

	public static class ID {
		public final String role, roleHint, pluginName;

		public ID(String pluginName, String role, String roleHint) {
			super();
			this.pluginName = pluginName;
			this.role = role;
			this.roleHint = roleHint;
		}
	}

	private static PlexusPluginStrategy getPluginStrategy() {
		return (PlexusPluginStrategy) Hudson.getInstance().getPluginManager()
				.getPluginStrategy();
	}

	private static Map<Object, ID> components = new WeakHashMap<Object, ID>();

	public static boolean isComponent(Object component) {
		return components.containsKey(component);
	}

	public static ID getID(Object component) {
		return components.get(component);
	}

	public static <T> T lookupComponent(Class<T> klazz, String pluginName,
			String roleHint, String configuration) {
		try {
			PluginManager pluginManager = Hudson.getInstance()
					.getPluginManager();
			PlexusContainer container = getPluginStrategy()
					.getPlexusContainer();
			ClassRealm realm = getPluginStrategy().getClassRealm(pluginName);
			T component = (T) container
					.lookup(klazz.getName(), roleHint, realm);
			if (configuration != null) {
				PlexusConfiguration config = new XmlPlexusConfigurationReader()
						.read(new StringReader(configuration));

				new BasicComponentConfigurator().configureComponent(component,
						config, container.getContainerRealm());
			}

			components.put(component, new ID(pluginName, klazz.getName(),
					roleHint));

			return component;

		} catch (ComponentLookupException e) {
			throw new RuntimeException("Could not look up component with role "
					+ klazz.getName() + " and roleHint " + roleHint, e);
		} catch (IOException e) {
			throw new RuntimeException("Error getting plexus component", e);
		} catch (PlexusConfigurationException e) {
			throw new RuntimeException("Error getting plexus component", e);
		} catch (ComponentConfigurationException e) {
			throw new RuntimeException("Error getting plexus component", e);
		} catch (NoSuchRealmException e) {
			throw new RuntimeException("Error getting plexus component", e);
		}
	}

	public static Object lookupComponent(String realm, String role,
			String roleHint, String configuration) {
		try {
			PlexusContainer container = getPluginStrategy()
					.getPlexusContainer();
			ClassRealm r = getPluginStrategy().getClassRealm(realm);
			Object component = container.lookup(role, roleHint, r);
			if (configuration != null) {
				PlexusConfiguration config = new XmlPlexusConfigurationReader()
						.read(new StringReader(configuration));

				new BasicComponentConfigurator().configureComponent(component,
						config, container.getContainerRealm());
			}

			return component;

		} catch (ComponentLookupException e) {
			throw new RuntimeException("Could not look up component with role "
					+ role + " and roleHint " + roleHint, e);
		} catch (IOException e) {
			throw new RuntimeException("Error getting plexus component", e);
		} catch (PlexusConfigurationException e) {
			throw new RuntimeException("Error getting plexus component", e);
		} catch (ComponentConfigurationException e) {
			throw new RuntimeException("Error getting plexus component", e);
		} catch (NoSuchRealmException e) {
			throw new RuntimeException("Error getting plexus component", e);
		}
	}

	public static Class lookupComponentClass(String plugin, String role,
			String roleHint) {
		try {
			ClassRealm realm = getPluginStrategy().getClassRealm(plugin);
			ComponentDescriptor descriptor = getPluginStrategy()
					.getPlexusContainer().getComponentDescriptor(role,
							roleHint, realm);
			Class clazz = realm.loadClass(descriptor.getImplementation());
			return clazz;
		} catch (NoSuchRealmException e) {
			throw new RuntimeException("", e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("", e);
		}

	}

}
