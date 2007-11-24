import org.acegisecurity.providers.ProviderManager
import org.acegisecurity.providers.anonymous.AnonymousAuthenticationProvider
import org.acegisecurity.providers.dao.DaoAuthenticationProvider

builder.beans {
    daoProvider(DaoAuthenticationProvider) {
        userDetailsService = bean(org.acegisecurity.userdetails.memory.InMemoryDaoImpl) {
            userMap = """
                alice=alice,NO_ROLE
                bob=bob,NO_ROLE
                charlie=charlie,NO_ROLE
                """
        }
    }

    anonymousProvider(AnonymousAuthenticationProvider) {
        key = "anonymous"
    }

    authenticationManager(ProviderManager) {
        providers = [
            daoProvider,
            anonymousProvider
        ]
    }
}
