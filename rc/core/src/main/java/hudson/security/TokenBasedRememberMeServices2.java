/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.security;

import org.acegisecurity.ui.rememberme.TokenBasedRememberMeServices;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.Authentication;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * {@link TokenBasedRememberMeServices} with modification so as not to rely
 * on the user password being available.
 *
 * <p>
 * This allows remember-me to work with security realms where the password
 * is never available in clear text.
 *
 * @author Kohsuke Kawaguchi
 */
public class TokenBasedRememberMeServices2 extends TokenBasedRememberMeServices {
    protected String makeTokenSignature(long tokenExpiryTime, UserDetails userDetails) {
        String expectedTokenSignature = DigestUtils.md5Hex(userDetails.getUsername() + ":" + tokenExpiryTime + ":"
                + "N/A" + ":" + getKey());
        return expectedTokenSignature;
    }

    protected String retrievePassword(Authentication successfulAuthentication) {
        return "N/A";
    }
}
