package hudson.views;

import hudson.model.TopLevelItem;

import java.util.ArrayList;
import java.util.List;

public class EmailValuesHelper {

	private static List<AbstractEmailValuesProvider> matchers = buildMatchers();
	
	public static List<String> getValues(TopLevelItem item) {
		List<String> values = new ArrayList<String>();
		if (item == null) {
			return values;
		}
		for (AbstractEmailValuesProvider matcher: matchers) {
			List<String> some = matcher.getValues(item);
			if (some != null) {
				values.addAll(some);
			}
		}
		return values;
	}
	
	private static List<AbstractEmailValuesProvider> buildMatchers() {
		List<AbstractEmailValuesProvider> matchers = new ArrayList<AbstractEmailValuesProvider>();
		try {
			matchers.add(PluginHelperUtils.validateAndThrow(new CoreEmailValuesProvider()));
		} catch (Throwable e) {
			// provider not available
		}
		try {
			matchers.add(buildEmailExt());
		} catch (Throwable e) {
			// plug-in probably not installed
		}
		try {
			matchers.add(buildEmailMaven());
		} catch (Throwable e) {
			// plug-in probably not installed
		}
		return matchers;
	}
	private static AbstractEmailValuesProvider buildEmailExt() {
		return PluginHelperUtils.validateAndThrow(new EmailExtValuesProvider());
	}
	private static AbstractEmailValuesProvider buildEmailMaven() {
		return PluginHelperUtils.validateAndThrow(new EmailMavenValuesProvider());
	}
	
}
