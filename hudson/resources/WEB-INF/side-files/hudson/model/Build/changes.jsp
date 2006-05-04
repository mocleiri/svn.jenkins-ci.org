<%--
  Displays the console output
--%>
<jsp:include page="sidepanel.jsp" />
<l:main-panel>
<t:buildCaption>Changes</t:buildCaption>

<c:set var="set" value="${it.changeSet}" />

<h2>Summary</h2>
<ol>
  <c:forEach var="cs" items="${set}">
    <li><c:out value="${cs.msg}" escapeXml="true" />
  </c:forEach>
</ol>
<table class=pane style="border:none">
  <c:forEach var="cs" items="${set}">
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
</l:main-panel>
<l:footer/>