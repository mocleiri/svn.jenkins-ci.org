<%--
  Displays the console output
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<st:include page="sidepanel.jsp" />
<l:main-panel>
<t:buildCaption>
  Console Output
</t:buildCaption>
<c:choose>
  <%-- do progressive console output --%>
  <c:when test="${it.building}">
    <pre id=out></pre>
    <t:progressiveText href="progressiveLog" idref="out" />
  </c:when>
  <%-- output is completed now. --%>
  <c:otherwise>
    <pre><c:out value="${it.log}" escapeXml="true" /></pre>
  </c:otherwise>
</c:choose>
</l:main-panel>
<l:footer/>