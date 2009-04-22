import org.acegisecurity.providers.ProviderManager
import org.acegisecurity.providers.anonymous.AnonymousAuthenticationProvider
import org.acegisecurity.providers.dao.DaoAuthenticationProvider

authenticationManager(ProviderManager) {
    providers = [
        // let's say this is the configured external source, such as LDAP
        bean(DaoAuthenticationProvider) {
            userDetailsService = bean(org.acegisecurity.userdetails.memory.InMemoryDaoImpl) {
                userMap = """
                    alice=alice,NO_ROLE
                    bob=bob,NO_ROLE
                    charlie=charlie,NO_ROLE
                    """
            }
        },
        // let's say this is the user database maintained by Hudson.
        // this allows Hudson to authenticate by using external source, but still
        // allow people to create accounts just on Hudson.
        bean(DaoAuthenticationProvider) {
            userDetailsService = bean(org.acegisecurity.userdetails.memory.InMemoryDaoImpl) {
                userMap = """
                    david=david,NO_ROLE
                    emma=emma,NO_ROLE
                    """
            }
        },
        bean(AnonymousAuthenticationProvider) {
            key = "anonymous"
        }
    ]
}
