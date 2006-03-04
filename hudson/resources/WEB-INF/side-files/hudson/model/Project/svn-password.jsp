<%-- show the icon legend --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<st:include page="sidepanel.jsp" />
<l:main-panel>
  <h1>How to set Subversion password?</h1>
  <p>
    While subversion allows you to specify the '--password' option explicitly in the command line,
    this is generally not desirable when you are using Hudson, because:
  </p>
  <ol>
    <li>People can read your password by using <tt>pargs</tt></li>
    <li>Password will be stored in a clear text in Hudson</li>
  </ol>
  <p>
    A preferrable approach is to do the following steps:
  </p>
  <ol>
    <li>Logon to the server that runs Hudson, by using the same user account Hudson uses</li>
    <li>Manually run <tt>svn co ...</tt></li>
    <li>Subversion asks you the password interactively. Type in the password</li>
    <li>
      Subversion stores it in its authentication cache, and for successive <tt>svn co ...</tt>
      it will use the password stored in the cache.
    </li>
  </ol>
  <p>
    Note that this approach still doesn't really make your password secure,
    it just makes it bit harder to read.
  </p>
</l:main-panel>
<l:footer/>
