<%--
  Displays the CVS change log.
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
  <c:forEach var="cs" items="${it.logs}">
    <tr class=pane>
      <td colspan=3 class=changeset>
        <a name="detail${cs.index}"></a>
        <div class="changeset-message">
          <b>${cs.author}:</b><br>
          <c:out value="${cs.msgEscaped}" escapeXml="false" />
        </div>
      </td>
    </tr>

    <c:forEach var="f" items="${cs.files}">
      <tr>
        <td>
          <img width="16" height="16"
            src="${rootURL}/images/16x16/document_${f.editType.name}.gif"
            title="${f.editType.description}">
        </td>
        <td>${f.revision}</td>
        <td>${f.name}</td>
      </tr>
    </c:forEach>
  </c:forEach>
</table>
