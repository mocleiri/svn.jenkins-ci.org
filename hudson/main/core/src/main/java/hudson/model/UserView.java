package hudson.model;

import hudson.Util;
import hudson.util.CaseInsensitiveComparator;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class UserView extends View {
    /**
     * Name of this view.
     */
    private String name;

    /**
     * Message displayed in the view page.
     */
    private String description;
    
	private User user;
	
    /**
     * List of job names. This is what gets serialized.
     */
    final Set<String> jobNames = new TreeSet<String>(CaseInsensitiveComparator.INSTANCE);
	
    public UserView(String name, User user) {
    	this.name = name;
    	this.user = user;
    }
    
    public String getUrl() {
        return user.getUrl() + "/userView/" + getViewName() + '/';
    }

    public User getUser() {
    	return user;
    }
    
    /**
     * Returns a read-only view of all {@link Job}s in this view.
     *
     * <p>
     * This method returns a separate copy each time to avoid
     * concurrent modification issue.
     */
    public List<TopLevelItem> getItems() {
        Set<String> names = (Set<String>) ((TreeSet<String>) jobNames).clone();        
        
        TopLevelItem[] items = new TopLevelItem[names.size()];
        int i=0;
        for (String name : names)
            items[i++] = Hudson.getInstance().getItem(name);
        return Arrays.asList(items);
    }
    
    public TopLevelItem getItem(String name) {
        return Hudson.getInstance().getItem(name);
    }

    public TopLevelItem getJob(String name) {
        return getItem(name);
    }

    public boolean contains(TopLevelItem item) {
        return jobNames.contains(item.getName());
    }

    public String getViewName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return name;
    }

    public boolean removeJob(String jobName) {
    	return jobNames.remove(jobName);
    }

    /**
     * Accepts submission from the configuration page.
     */
    public void doConfigSubmit( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        checkPermission(CONFIGURE);

        req.setCharacterEncoding("UTF-8");

        synchronized (user) {
	        jobNames.clear();
	        for (TopLevelItem item : Hudson.getInstance().getItems()) {
	            if(req.getParameter(item.getName())!=null)
	                jobNames.add(item.getName());
	        }
	
	        description = Util.nullify(req.getParameter("description"));
	
	        try {
	            String n = req.getParameter("name");
	            Hudson.checkGoodName(n);
	            name = n;
	        } catch (ParseException e) {
	            sendError(e, req, rsp);
	            return;
	        }
	
	        user.save();
        }

        rsp.sendRedirect("../"+name);
    }
    
    /**
     * Accepts the new description.
     */
    public void doSubmitDescription( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        checkPermission(CONFIGURE);

        req.setCharacterEncoding("UTF-8");
        synchronized (user) {
	        this.description = req.getParameter("description");
	        user.save();
        }
        rsp.sendRedirect(".");  // go to the top page
    }
    
    /**
     * Deletes this view.
     */
    public void doDoDelete( StaplerRequest req, StaplerResponse rsp ) throws IOException {
        user.deleteView(this);
        rsp.sendRedirect2(req.getContextPath()+"/"+user.getViewsUrl());
    }
    
    public Item doCreateItem(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        Item item = Hudson.getInstance().doCreateItem(req, rsp);
        if(item!=null) {
        	synchronized (user) {
        		jobNames.add(item.getName());
        		user.save();
        	}
        }
        return item;
    }
}
