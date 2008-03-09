package hudson.widgets;

import hudson.Functions;
import hudson.model.ModelObject;
import hudson.model.Run;
import org.kohsuke.stapler.Header;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Displays the history of records (normally {@link Run}s) on the side panel.
 *
 * @param <O>
 *      Owner of the widget.
 * @param <T>
 *      Type individual record.
 * @author Kohsuke Kawaguchi
 */
public class HistoryWidget<O extends ModelObject,T> extends Widget {
    /**
     * The given data model of records.
     */
    public Iterable<T> baseList;

    /**
     * Indicates the next build number that client ajax should fetch.
     */
    private String nextBuildNumberToFetch;

    /**
     * URL of the {@link #owner}.
     */
    public final String baseUrl;

    public final O owner;

    private boolean trimmed;

    public final Adapter<? super T> adapter;

    /**
     * @param owner
     *      The parent model object that owns this widget.
     */
    public HistoryWidget(O owner, Iterable<T> baseList, Adapter<? super T> adapter) {
        this.adapter = adapter;
        this.baseList = baseList;
        this.baseUrl = Functions.getNearestAncestorUrl(Stapler.getCurrentRequest(),owner);
        this.owner = owner;
    }

    public String getUrlName() {
        return "buildHistory";
    }

    /**
     * The records to be rendered this time.
     */
    public Iterable<T> getRenderList() {
        if(trimmed) {
            List<T> lst;
            if (baseList instanceof List) {
                lst = (List<T>) baseList;
                if(lst.size()>THRESHOLD)
                    return lst.subList(0,THRESHOLD);
                trimmed=false;
                return lst;
            } else {
                lst = new ArrayList<T>(THRESHOLD);
                Iterator<T> itr = baseList.iterator();
                while(lst.size()<=THRESHOLD && itr.hasNext())
                    lst.add(itr.next());
                trimmed = itr.hasNext();
                return lst;
            }
        } else
            return baseList;
    }

    public boolean isTrimmed() {
        return trimmed;
    }

    public void setTrimmed(boolean trimmed) {
        this.trimmed = trimmed;
    }

    /**
     * Handles AJAX requests from browsers to update build history.
     *
     * @param n
     *      The build 'number' to fetch. This is string because various variants
     *      uses non-numbers as the build key.
     */
    public void doAjax( StaplerRequest req, StaplerResponse rsp,
                  @Header("n") String n ) throws IOException, ServletException {

        rsp.setContentType("text/html;charset=UTF-8");

        // pick up builds to send back
        List<T> items = new ArrayList<T>();

        for (T t : baseList) {
            if(adapter.compare(t,n)>=0)
                items.add(t);
            else
                break;
        }

        baseList = items;
        if(!items.isEmpty()) {
            T b = items.get(0);
            n = adapter.getKey(b);
            if(!adapter.isBuilding(b))
                n = adapter.getNextKey(n);
        }

        rsp.setHeader("n",n);

        req.getView(this,"ajaxBuildHistory.jelly").forward(req,rsp);
    }

    private static final int THRESHOLD = 30;

    public String getNextBuildNumberToFetch() {
        return nextBuildNumberToFetch;
    }

    public void setNextBuildNumberToFetch(String nextBuildNumberToFetch) {
        this.nextBuildNumberToFetch = nextBuildNumberToFetch;
    }

    public interface Adapter<T> {
        /**
         * If record is newer than the key, return a positive number.
         */
        int compare(T record, String key);
        String getKey(T record);
        boolean isBuilding(T record);
        String getNextKey(String key);
    }
}
