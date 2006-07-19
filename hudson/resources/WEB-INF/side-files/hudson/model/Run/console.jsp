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
  <table width="100%" cellpadding="0" cellspacing="0"><tr valign="middle">
    <td>
      <t:buildCaption>
        Console Output
      </t:buildCaption>
    </td><td align="right">
      <a href="consoleText">
        <img src="${rootURL}/images/24x24/document.gif" alt="">View as plain text
      </a>
    </td>
  </tr></table>
<c:choose>
  <%-- do progressive console output --%>
  <c:when test="${it.building}">
    <pre id=out></pre>
    <div id=spinner>
      <img src="${rootURL}/images/spinner.gif" /> 
    </div>
    <t:progressiveText href="progressiveLog" idref="out" spinner="spinner" />
  </c:when>
  <%-- output is completed now. --%>
  <c:otherwise>
    <pre><c:out value="${it.log}" escapeXml="true" /></pre>
  </c:otherwise>
</c:choose>
</l:main-panel>
<l:footer/>