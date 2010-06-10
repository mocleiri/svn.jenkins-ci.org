import org.acegisecurity.providers.anonymous.AnonymousProcessingFilter
import org.acegisecurity.ui.AccessDeniedHandlerImpl
import org.acegisecurity.ui.ExceptionTranslationFilter
import org.acegisecurity.ui.basicauth.BasicProcessingFilter
import org.acegisecurity.ui.basicauth.BasicProcessingFilterEntryPoint
import org.acegisecurity.ui.webapp.AuthenticationProcessingFilter
import org.acegisecurity.context.HttpSessionContextIntegrationFilter
import org.acegisecurity.ui.webapp.AuthenticationProcessingFilterEntryPoint

filter(ChainedServletFilter) {
    // we can define locally scope bean like this without polluting the global namespace.

// this definition is for the case where the basic authentication is the primary authentication means
//    def entryPoint = bean(BasicProcessingFilterEntryPoint) {
//        realmName = "Hudson's security realm"
//    }

    // this definition is for the case where the form authentication is the primary means
    def entryPoint = bean(AuthenticationProcessingFilterEntryPoint) {
        loginFormUrl = "/login"
    }

    // NOTE: all the variable names, even those that show up LHS,
    // are first resolved as a class name, so if the entire script is
    // named like "filters.groovy", this will fail to compile.
    // so what this probably means is that the script should be named
    // like a Java class ("FilterDef.groovy")
    filters = [
        // this persists the authentication across requests by using session
        bean(HttpSessionContextIntegrationFilter) {
        },
        // allow clients to submit basic authentication credential
        bean(BasicProcessingFilter) {
            authenticationManager = WebAppMain.AUTHENTICATION_MANAGER
            authenticationEntryPoint = entryPoint
        },
        bean(AuthenticationProcessingFilter) {
            authenticationManager = WebAppMain.AUTHENTICATION_MANAGER
            authenticationFailureUrl = "/login?failed"
            defaultTargetUrl = "/"
            filterProcessesUrl = "/j_acegi_security_check"
        },
        bean(AnonymousProcessingFilter) {
            key = "anonymous" // must match with the AnonymousProvider
            userAttribute = "anonymous,"
        },
        bean(ExceptionTranslationFilter) {
            // property can be created programatically/eagler like this,
            // instead of doing everything as managed Spring beans
            accessDeniedHandler = new AccessDeniedHandlerImpl()
            authenticationEntryPoint = entryPoint
        }
    ]
}
