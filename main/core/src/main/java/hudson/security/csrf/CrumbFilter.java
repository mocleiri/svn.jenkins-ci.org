/**
 * Copyright (c) 2008-2009 Yahoo! Inc. 
 * All rights reserved. 
 * The copyrights to the contents of this file are licensed under the MIT License (http://www.opensource.org/licenses/mit-license.php)
 */

package hudson.security.csrf;

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Checks for and validates crumbs on requests that cause state changes, to
 * protect against cross site request forgeries.
 * 
 * @author dty
 *
 */
public class CrumbFilter implements Filter {
	private volatile CrumbIssuer crumbIssuer;
	
	public CrumbIssuer getCrumbIssuer() {
		return crumbIssuer;
	}
	
	public void setCrumbIssuer(CrumbIssuer issuer) {
		crumbIssuer = issuer;
	}
	
    /**
     * Gets the {@link CrumbFilter} created for the given {@link ServletContext}.
     */
    public static CrumbFilter get(ServletContext context) {
        return (CrumbFilter)context.getAttribute(CrumbFilter.class.getName());
    }

    /**
	 * {@inheritDoc}
	 */
	public void init(FilterConfig filterConfig) throws ServletException {
        // this is how we make us available to the rest of Hudson.
        filterConfig.getServletContext().setAttribute(CrumbFilter.class.getName(),this);
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (crumbIssuer == null) {
			chain.doFilter(request, response);
			return;
		}
		if (!(request instanceof HttpServletRequest)) {
			chain.doFilter(request, response);
			return;
		}

		HttpServletRequest httpRequest = (HttpServletRequest)request;
		String crumbFieldName = crumbIssuer.getDescriptor().getCrumbRequestField();
		String crumbSalt = crumbIssuer.getDescriptor().getCrumbSalt();
		
		if ("POST".equals(httpRequest.getMethod())) {
			String crumb = httpRequest.getHeader(crumbFieldName);
			boolean valid = false;
			if (crumb == null) {
				Enumeration<?> paramNames = request.getParameterNames();
				while (paramNames.hasMoreElements()) {
					String paramName = (String) paramNames.nextElement();
					if (crumbFieldName.equals(paramName)) {
						crumb = request.getParameter(paramName);
						break;
					}
				}
			}
			if (crumb != null) {
				if (crumbIssuer.validateCrumb(httpRequest, crumbSalt, crumb)) {
					valid = true;
				} else {
					LOGGER.warning("Found invalid crumb " + crumb +
							".  Will check remaining parameters for a valid one...");
				}				
			}
			// Multipart requests need to be handled by each handler.
			if (valid || isMultipart(httpRequest)) {
				chain.doFilter(request, response);
			} else {
				LOGGER.warning("No valid crumb was included in request for " + httpRequest.getRequestURI() + ".  Returning " + HttpServletResponse.SC_FORBIDDEN + ".");
				((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
			}
		} else {
			chain.doFilter(request, response);
		}
	}


	protected static boolean isMultipart(HttpServletRequest request)
	{
	    if (request == null) {
	        return false;
	    }

	    String contentType = request.getContentType();
	    if (contentType == null) {
	        return false;
	    }

	    String[] parts = contentType.split( ";" );
	    if (parts.length == 0) {
	        return false;
	    }

	    for (int i = 0; i < parts.length; i++) {
	        if ("multipart/form-data".equals(parts[i])) {
	            return true;
	        }
	    }

	    return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void destroy() {
	}
	
	private static final Logger LOGGER = Logger.getLogger(CrumbFilter.class.getName());
}
