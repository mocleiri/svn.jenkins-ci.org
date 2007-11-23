import org.acegisecurity.AccessDeniedException;
import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.ProviderManager;
import org.acegisecurity.providers.anonymous.AnonymousAuthenticationToken;
import org.acegisecurity.providers.anonymous.AnonymousAuthenticationProvider;
import org.acegisecurity.providers.dao.DaoAuthenticationProvider;
import org.acegisecurity.userdetails.User;
import org.acegisecurity.userdetails.memory.InMemoryDaoImpl;
import org.acegisecurity.userdetails.memory.UserMap;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.springframework.context.support.GenericApplicationContext;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public final ProviderManager authenticationManager;
    public final DaoAuthenticationProvider daoProvider;
    public final InMemoryDaoImpl dao;
    private static final GrantedAuthority[] EMPTY_ARRAY = new GrantedAuthority[0];

    public Main() throws Exception {
        // just using this to satisfy MessageSource
        GenericApplicationContext gac = new GenericApplicationContext();

        // emulate the bean wiring of Spring
        dao = new InMemoryDaoImpl();
        UserMap userMap = new UserMap();
        userMap.addUser(new User("alice","alice",true,true,true,true,EMPTY_ARRAY));
        userMap.addUser(new User("bob","bob",true,true,true,true,EMPTY_ARRAY));
        dao.setUserMap(userMap);
        dao.afterPropertiesSet();

        daoProvider = new DaoAuthenticationProvider();
        daoProvider.setUserDetailsService(dao);
        daoProvider.setMessageSource(gac);
        daoProvider.afterPropertiesSet();

        AnonymousAuthenticationProvider aap = new AnonymousAuthenticationProvider();
        aap.setKey("anonymous");
        aap.setMessageSource(gac);
        aap.afterPropertiesSet();

        authenticationManager = new ProviderManager();
        authenticationManager.setProviders(Arrays.asList(daoProvider,aap));
        authenticationManager.afterPropertiesSet();

        gac.refresh();
    }

    public void doSecured(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if(a==null || a instanceof AnonymousAuthenticationToken) { // TODO: replace by a proper authorization
            /* this won't initiate basic authentication
            rsp.sendError(rsp.SC_UNAUTHORIZED);
            */

            throw new AccessDeniedException("This page is secured");
        } else {
            rsp.getWriter().println("Secret information");
        }
    }
}
