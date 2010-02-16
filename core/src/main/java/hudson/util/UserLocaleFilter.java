/*
 *  The MIT License
 * 
 *  Copyright (c) 2004-2010, Sun Microsystems, Inc., Seiji Sogabe
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 * 
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 * 
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package hudson.util;

import hudson.model.User;
import hudson.model.UserLocaleProperty;
import java.io.IOException;
import java.util.Locale;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Filter which enables request to return user's locale in user configuration.
 * @author Seiji Sogabe
 */
public class UserLocaleFilter implements Filter {

    public void init(FilterConfig fc) throws ServletException {
        //
    }

    public void destroy() {
        //
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletRequestWrapper wrapper = new UserLocaleRequestWrapper(req);
        chain.doFilter(wrapper, response);
    }

    public static class UserLocaleRequestWrapper extends HttpServletRequestWrapper {

        public UserLocaleRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public Locale getLocale() {
            Locale locale = userLocale();
            if (locale != null)
                return locale;
            return super.getLocale();
        }

        private Locale userLocale() {
            User user = User.current();
            if (user == null)
                return null;
            UserLocaleProperty ulp = user.getProperty(UserLocaleProperty.class);
            if (ulp == null)
                return null;
            return ulp.getLocale();
        }
    }

}
