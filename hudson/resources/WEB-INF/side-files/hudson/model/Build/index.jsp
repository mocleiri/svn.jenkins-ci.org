<jsp:include page="sidepanel.jsp" />
<l:main-panel>
<t:buildCaption>
  Build #${it.number}
  (<i:formatDate value="${it.timestamp.time}" type="both" dateStyle="medium" timeStyle="medium"/>)
</t:buildCaption>

<st:include page="logKeep.jsp" />

<table style="margin-top: 1em; margin-left:1em;">
  <t:artifactList build="${it}" caption="Build Artifacts" />

  <c:set var="tr" value="${it.testResultAction}" />
  <c:if test="${tr!=null}">
    <t:summary icon="clipboard.gif">
      <a href="testReport/">Latest Test Result</a>
      <c:choose>
        <c:when test="${tr.failCount==0}">
          (no failures)
        </c:when>
        <c:when test="${tr.failCount==1}">
          (1 failure)
        </c:when>
        <c:otherwise>
          (${tr.failCount} failures)
        </c:otherwise>
      </c:choose>
    </t:summary>
  </c:if>


  <c:set var="set" value="${it.changeSet}" />
  <t:summary icon="notepad.gif">
    <c:choose>
      <c:when test="${f:length(set)==0}">
        No changes.
      </c:when>
      <c:otherwise>
        Changes
        <ol>
          <c:forEach var="cs" items="${set}">
            <li><c:out value="${cs.msgEscaped}" escapeXml="false" /> (<a href="changes#detail${cs.index}">detail</a>)
          </c:forEach>
        </ol>
      </c:otherwise>
    </c:choose>
  </t:summary>
</table>



<h2>Permalinks</h2>
<ul>
  <li><a href="buildNumber">Build number</a>
</ul>
</l:main-panel>
<l:footer/>
