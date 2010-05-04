/**
 * Copyright (c) 2008-2009 Yahoo! Inc.
 * All rights reserved.
 * The copyrights to the contents of this file are licensed under the MIT License (http://www.opensource.org/licenses/mit-license.php)
 */
package hudson.security.csrf;

import javax.servlet.ServletRequest;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Api;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.MultipartFormDataParser;

/**
 * A CrumbIssuer represents an algorithm to generate a nonce value, known as a
 * crumb, to counter cross site request forgery exploits. Crumbs are typically
 * hashes incorporating information that uniquely identifies an agent that sends
 * a request, along with a guarded secret so that the crumb value cannot be
 * forged by a third party.
 *
 * @author dty
 * @see http://en.wikipedia.org/wiki/XSRF
 */
@ExportedBean
public abstract class CrumbIssuer implements Describable<CrumbIssuer>, ExtensionPoint {

    private static final String CRUMB_ATTRIBUTE = CrumbIssuer.class.getName() + "_crumb";

    /**
     * Get the name of the request parameter the crumb will be stored in. Exposed
     * here for the remote API.
     */
    @Exported
    public String getCrumbRequestField() {
        return getDescriptor().getCrumbRequestField();
    }

    /**
     * Get a crumb value based on user specific information in the current request.
     * Intended for use only by the remote API.
     * @return
     */
    @Exported
    public String getCrumb() {
        return getCrumb(Stapler.getCurrentRequest());
    }

    /**
     * Get a crumb value based on user specific information in the request.
     * @param request
     * @return
     */
    public String getCrumb(ServletRequest request) {
        String crumb = null;
        if (request != null) {
            crumb = (String) request.getAttribute(CRUMB_ATTRIBUTE);
        }
        if (crumb == null) {
            crumb = issueCrumb(request, getDescriptor().getCrumbSalt());
            if (request != null) {
                if ((crumb != null) && crumb.length()>0) {
                    request.setAttribute(CRUMB_ATTRIBUTE, crumb);
                } else {
                    request.removeAttribute(CRUMB_ATTRIBUTE);
                }
            }
        }

        return crumb;
    }

    /**
     * Create a crumb value based on user specific information in the request.
     * The crumb should be generated by building a cryptographic hash of:
     * <ul>
     *  <li>relevant information in the request that can uniquely identify the client
     *  <li>the salt value
     *  <li>an implementation specific guarded secret.
     * </ul>
     *
     * @param request
     * @param salt
     * @return
     */
    protected abstract String issueCrumb(ServletRequest request, String salt);

    /**
     * Get a crumb from a request parameter and validate it against other data
     * in the current request. The salt and request parameter that is used is
     * defined by the current configuration.
     *
     * @param request
     * @return
     */
    public boolean validateCrumb(ServletRequest request) {
        CrumbIssuerDescriptor<CrumbIssuer> desc = getDescriptor();
        String crumbField = desc.getCrumbRequestField();
        String crumbSalt = desc.getCrumbSalt();

        return validateCrumb(request, crumbSalt, request.getParameter(crumbField));
    }

    /**
     * Get a crumb from multipart form data and validate it against other data
     * in the current request. The salt and request parameter that is used is
     * defined by the current configuration.
     *
     * @param request
     * @param parser
     * @return
     */
    public boolean validateCrumb(ServletRequest request, MultipartFormDataParser parser) {
        CrumbIssuerDescriptor<CrumbIssuer> desc = getDescriptor();
        String crumbField = desc.getCrumbRequestField();
        String crumbSalt = desc.getCrumbSalt();

        return validateCrumb(request, crumbSalt, parser.get(crumbField));
    }

    /**
     * Validate a previously created crumb against information in the current request.
     *
     * @param request
     * @param salt
     * @param crumb The previously generated crumb to validate against information in the current request
     * @return
     */
    public abstract boolean validateCrumb(ServletRequest request, String salt, String crumb);

    /**
     * Access global configuration for the crumb issuer.
     */
    public CrumbIssuerDescriptor<CrumbIssuer> getDescriptor() {
        return (CrumbIssuerDescriptor<CrumbIssuer>) Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    /**
     * Returns all the registered {@link CrumbIssuer} descriptors.
     */
    public static DescriptorExtensionList<CrumbIssuer, Descriptor<CrumbIssuer>> all() {
        return Hudson.getInstance().<CrumbIssuer, Descriptor<CrumbIssuer>>getDescriptorList(CrumbIssuer.class);
    }

    public Api getApi() {
        return new Api(this);
    }
}
