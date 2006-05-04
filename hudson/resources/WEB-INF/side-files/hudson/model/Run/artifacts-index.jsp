<st:include page="sidepanel.jsp" />
<l:main-panel>
<t:buildCaption>
  Build Artifacts
</t:buildCaption>
<ul>
  <c:forEach var="f" items="${it.artifacts}">
    <li><a href="artifact/${f}">${f}</a></li>
  </c:forEach>
</ul>
</l:main-panel>
<l:footer/>