<%-- show the icon legend --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<st:include page="sidepanel.jsp" />
<l:main-panel>
  <table cellpadding=5>
    <tr><td>
      <img src="images/48x48/grey.gif">
      The project has never been built before.
    </td></tr>
    <tr><td>
      <img src="images/48x48/grey_anime.gif">
      The first build of the project is in progress.
    </td></tr>
    <tr><td>
      <img src="images/48x48/blue.gif">
      The last build was successful.
    </td></tr>
    <tr><td>
      <img src="images/48x48/blue_anime.gif">
      The last build was successful. A new build is in progress.
    </td></tr>
    <tr><td>
      <img src="images/48x48/red.gif">
      The last build failed.
    </td></tr>
    <tr><td>
      <img src="images/48x48/red_anime.gif">
      The last build failed. A new build is in progress.
    </td></tr>
  </table>

</l:main-panel>
<l:footer/>
