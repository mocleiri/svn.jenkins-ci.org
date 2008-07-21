package hudson.model;

import hudson.util.RunList;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class GroupView extends View {

	private final Group group;
	private final Hudson owner;
	private final User user;
	
	public GroupView(Hudson owner, Group group, User user) {
		this.owner = owner;
		this.group = group;
		this.user = user;
	}

	public User getUser() {
		return user;
	}
	
	@Override
	public boolean contains(TopLevelItem item) {
		return false;
	}

	@Override
	public Item doCreateItem(StaplerRequest req, StaplerResponse rsp)
			throws IOException, ServletException {
        Item item = owner.doCreateItem(req, rsp);
        return item;
	}

	@Override
	public String getDescription() {
		return "Jobs defined for group " + group.getDisplayName();
	}

	@Override
	public TopLevelItem getItem(String name) {
		return owner.getItem(name);
	}
	
    public TopLevelItem getJob(String name) {
        return getItem(name);
    }

	@Override
	public Collection<TopLevelItem> getItems() {
		return group.getItems();
	}

	@Override
	public String getUrl() {
        return user.getUrl() + "/groupView/" + group.getDisplayName() + '/';
	}

	@Override
	public String getViewName() {
        return getDisplayName();
	}

	public String getDisplayName() {
        return group.getDisplayName() + " jobs";
	}
	
    public RunList getBuilds() {
    	return new RunList(this);
    }
}