import grails.spring.BeanBuilder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class AcegiFilter implements Filter {
    private ServletContext servletContext;
    private ChainedServletFilter filter;

    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            servletContext = filterConfig.getServletContext();

            BeanBuilder builder = new BeanBuilder();
            builder.parse(getClass().getResourceAsStream("Filters.groovy"));
            filter = (ChainedServletFilter) builder.createApplicationContext().getBean("filter");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        filter.doFilter(request,response,chain);
    }

    public void destroy() {
        filter.destroy();
    }
}
