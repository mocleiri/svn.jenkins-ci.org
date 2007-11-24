import org.acegisecurity.providers.anonymous.AnonymousProcessingFilter
import org.acegisecurity.ui.AccessDeniedHandlerImpl
import org.acegisecurity.ui.ExceptionTranslationFilter
import org.acegisecurity.ui.basicauth.BasicProcessingFilter
import org.acegisecurity.ui.basicauth.BasicProcessingFilterEntryPoint

def entryPoint = bean(BasicProcessingFilterEntryPoint) {
    realmName = "Hudson's security realm"
}

filter(ChainedServletFilter) {
    // NOTE: all the variable names, even those that show up LHS,
    // are first resolved as a class name, so if the entire script is
    // named like "filters.groovy", this will fail to compile.
    // so what this probably means is that the script should be named
    // like a Java class ("FilterDef.groovy")
    filters = [
        bean(BasicProcessingFilter) {
            authenticationManager = parentRef("authenticationManager")
            authenticationEntryPoint = entryPoint
        },
        bean(AnonymousProcessingFilter) {
            key = "anonymous" // must match with the AnonymousProvider
            userAttribute = "anonymous,"
        },
        bean(ExceptionTranslationFilter) {
            accessDeniedHandler = new AccessDeniedHandlerImpl()
            authenticationEntryPoint = entryPoint
        }
    ]
}