import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Kohsuke Kawaguchi
 */
public class ChainedServletFilter implements Filter {
    // array is immutable once set
    private volatile Filter[] filters;

    public ChainedServletFilter() {
        filters = new Filter[0];
    }

    public ChainedServletFilter(Collection<? extends Filter> filters) {
        setFilters(filters);
    }

    public void setFilters(Collection<? extends Filter> filters) {
        this.filters = filters.toArray(new Filter[filters.size()]);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        for (Filter f : filters)
            f.init(filterConfig);
    }

    public void doFilter(ServletRequest request, ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        new FilterChain() {
            private int position=0;
            // capture the array for thread-safety
            private final Filter[] filters = ChainedServletFilter.this.filters;
            
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                if(position==filters.length) {
                    // reached to the end
                    chain.doFilter(request,response);
                } else {
                    // call next
                    filters[position++].doFilter(request,response,this);
                }
            }
        }.doFilter(request,response);
    }

    public void destroy() {
        for (Filter f : filters)
            f.destroy();
    }
}
