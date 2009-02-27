/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package no.twingine.hudson.greenballs;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author ANilsen
 */
public class GreenBallFilter implements Filter {

  final Pattern pattern = Pattern.compile(".+/(\\d{2}x\\d{2})/blue(_anime|)\\.gif");

  public void init(FilterConfig config) throws ServletException {
  }

  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
    if (req instanceof HttpServletRequest && resp instanceof HttpServletResponse) {
      final HttpServletRequest httpServletRequest = (HttpServletRequest) req;
      final HttpServletResponse httpServletResponse = (HttpServletResponse) resp;
      final String uri = httpServletRequest.getRequestURI();
      final Matcher m = pattern.matcher(uri);
      if (m.matches())
        httpServletResponse.sendRedirect("/plugin/greenballs/" + m.group(1) + "/green" + m.group(2) + ".gif");
    }
    chain.doFilter(req, resp);
  }

  public void destroy() {
  }

}
