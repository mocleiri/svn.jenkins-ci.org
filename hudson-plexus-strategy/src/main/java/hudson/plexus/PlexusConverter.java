package hudson.plexus;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class PlexusConverter extends AbstractReflectionConverter {

	public PlexusConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
		super(mapper, reflectionProvider);
	}

	@Override
	protected void doMarshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		
		PlexusUtil.ID id = PlexusUtil.getID(source);
		if (id != null) {
			writer.addAttribute("realm", id.pluginName);
			writer.addAttribute("role", id.role);
			writer.addAttribute("roleHint", id.roleHint);
			writer.startNode("configuration");
			super.doMarshal(source, writer, context);
			writer.endNode();
		} else {
			super.doMarshal(source, writer, context);
		}
		
	}

	@Override
	public Object doUnmarshal(Object result, HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		if (reader.getAttribute("realm") != null) {
			String realm = reader.getAttribute("realm");
			String role =reader.getAttribute("role");
			String roleHint = reader.getAttribute("roleHint");
			
			Object component = PlexusUtil.lookupComponent(realm, role, roleHint, null);
			
			reader.moveDown();
			component = super.doUnmarshal(component, reader, context);
			reader.moveUp();
			return component;
			
		} else {
			return super.doUnmarshal(result, reader, context);
		}
	}

	@Override
	public boolean canConvert(Class type) {
		return true;
	}

	
	
}
