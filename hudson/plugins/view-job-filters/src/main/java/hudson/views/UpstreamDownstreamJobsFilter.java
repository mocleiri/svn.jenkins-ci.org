package hudson.views;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.TopLevelItem;
import hudson.model.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kohsuke.stapler.DataBoundConstructor;

public class UpstreamDownstreamJobsFilter extends ViewJobFilter {
	
	private boolean includeDownstream;
	private boolean includeUpstream;
	private boolean recursive;
	private boolean excludeOriginals;
	
	@DataBoundConstructor
	public UpstreamDownstreamJobsFilter(boolean includeDownstream, boolean includeUpstream,  
			boolean recursive, boolean excludeOriginals) {
		this.includeDownstream = includeDownstream;
		this.excludeOriginals = excludeOriginals;
		this.includeUpstream = includeUpstream;
		this.recursive = recursive;
	}

    @Override
    public List<TopLevelItem> filter(List<TopLevelItem> added, List<TopLevelItem> all, View filteringView) {
    	Set<TopLevelItem> filtered = new HashSet<TopLevelItem>();

   		for (TopLevelItem next: added) {
   			if (includeUpstream) {
   				addUpstream(next, filtered, all);
   			}
   			if (includeDownstream) {
   				addDownstream(next, filtered, all);
   			}
    	}
    	
    	List<TopLevelItem> sorted = new ArrayList<TopLevelItem>(all);
    	
    	// ensure all the previously added items are included
    	if (!excludeOriginals) {
    		filtered.addAll(added);
    	}
    	
    	sorted.retainAll(filtered);
    	
        return sorted;
    }

    public void addUpstream(TopLevelItem current, Set<TopLevelItem> filtered, List<TopLevelItem> all) {
    	for (TopLevelItem next: all) {
    		if (filtered.contains(next)) {
    			continue;
    		}
			boolean isFirstUpstreamFromSecond = isFirstUpstreamFromSecond(next, current);
			if (isFirstUpstreamFromSecond) {
    			filtered.add(next);
    			if (recursive) {
    				addUpstream(next, filtered, all);
    			}
			}
    	}
    }
    public void addDownstream(TopLevelItem current, Set<TopLevelItem> filtered, List<TopLevelItem> all) {
    	for (TopLevelItem next: all) {
    		if (filtered.contains(next)) {
    			continue;
    		}
			boolean isFirstUpstreamFromSecond = isFirstUpstreamFromSecond(current, next);
			if (isFirstUpstreamFromSecond) {
    			filtered.add(next);
    			if (recursive) {
    				addDownstream(next, filtered, all);
    			}
			}
    	}
    }
    
	@SuppressWarnings("unchecked")
	private boolean isFirstUpstreamFromSecond(TopLevelItem first, TopLevelItem second) {
    	if (second instanceof AbstractProject) {
    		AbstractProject secondProject = (AbstractProject) second;
        	List<AbstractProject> upstream = secondProject.getBuildTriggerUpstreamProjects();
    		return upstream.contains(first);
    	}
    	return false;
    }
    
	
	@Extension
	public static class DescriptorImpl extends Descriptor<ViewJobFilter> {
		@Override
		public String getDisplayName() {
			return "Upstream/Downstream Jobs Filter";
		}
		@Override
        public String getHelpFile() {
            return "/plugin/view-job-filters/upstream-downstream-help.html";
        }
	}


	public boolean isIncludeDownstream() {
		return includeDownstream;
	}

	public boolean isIncludeUpstream() {
		return includeUpstream;
	}

	public boolean isRecursive() {
		return recursive;
	}

	public boolean isExcludeOriginals() {
		return excludeOriginals;
	}

}
