<%--
  Displays the console output
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<st:include page="sidepanel.jsp" />
<l:main-panel>
<h1>
  <img src="buildStatus" width="32" height="32" border="0">
  Console Output
</h1>
<pre><c:out value="${it.log}" escapeXml="true" /></pre>
</l:main-panel>
<l:footer/>