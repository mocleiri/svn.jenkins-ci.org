<%--
  Displays the console output
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<st:include page="sidepanel.jsp" />
<l:main-panel>
<h1>Build Artifacts</h1>
<ul>
  <c:forEach var="f" items="${it.artifacts}">
    <li><a href="artifact/${f}">${f}</a></li>
  </c:forEach>
</ul>
</l:main-panel>
<l:footer/>