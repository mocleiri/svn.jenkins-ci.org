<%--
  Displays the Subversion change log.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<h2>Summary</h2>
<ol>
  <c:forEach var="cs" items="${it.logs}">
    <li><c:out value="${cs.msg}" escapeXml="true" />
  </c:forEach>
</ol>
<table class=pane style="border:none">
  <c:forEach var="cs" items="${it.logs}" varStatus="loop">
    <tr class=pane>
      <td colspan=2 class=changeset>
        <a name="detail${loop.index}"></a>
        <div class="changeset-message">
          <b>Revision ${cs.revision} by ${cs.author}:</b><br>
          <c:out value="${cs.msgEscaped}" escapeXml="false" />
        </div>
      </td>
    </tr>

    <c:forEach var="p" items="${cs.paths}">
      <tr>
        <td><t:editTypeIcon type="${p.editType}" /></td>
        <td>${p.value}</td>
      </tr>
    </c:forEach>
  </c:forEach>
</table>
