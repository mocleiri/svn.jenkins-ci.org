import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.providers.anonymous.AnonymousProcessingFilter;
import org.acegisecurity.ui.ExceptionTranslationFilter;
import org.acegisecurity.ui.basicauth.BasicProcessingFilter;
import org.acegisecurity.ui.basicauth.BasicProcessingFilterEntryPoint;
import org.acegisecurity.userdetails.memory.UserAttribute;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Kohsuke Kawaguchi
 */
public class AcegiFilter implements Filter {
    private ServletContext servletContext;
    private ChainedServletFilter filter = new ChainedServletFilter();

    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            servletContext = filterConfig.getServletContext();

            BasicProcessingFilter bpf = new BasicProcessingFilter();
            bpf.setAuthenticationManager(WebAppMain.MAIN.authenticationManager);
            BasicProcessingFilterEntryPoint bpfep = new BasicProcessingFilterEntryPoint();
            bpfep.setRealmName("Hudson's security realm");
            bpfep.afterPropertiesSet();
            bpf.setAuthenticationEntryPoint(bpfep);

            // this filter initiates the security protocol if the authentication is required
            ExceptionTranslationFilter etf = new ExceptionTranslationFilter();
            etf.setAuthenticationEntryPoint(bpfep);

            // anonymous if no authentication information is provided
            AnonymousProcessingFilter apf = new AnonymousProcessingFilter();
            UserAttribute ua = new UserAttribute();
            ua.setAuthorities(Arrays.asList(new GrantedAuthorityImpl("")));
            ua.setPassword("anonymous");
            apf.setUserAttribute(ua);
            apf.setKey("anonymous"); // this must match with AnonymousAuthenticationProvider

            // see Acegi security user manual 'Filters' section for the discussion of the order
            filter.setFilters(Arrays.asList(bpf,apf,etf));
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
