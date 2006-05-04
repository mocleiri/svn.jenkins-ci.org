<c:set var="tr" value="${it.lastSuccessfulBuild.testResultAction}" />
<c:if test="${tr.previousResult!=null}">
  <%-- at least two data points are requierd for a trend report --%>
  <div style="float:right;">
    <div class="test-trend-caption">
      Test Result Trend
    </div>
    <div>
      <a href="testResultTrend?width=800&height=600"><img src="testResultTrend"></a>
    </div>
    <div style="text-align:right">
      <a href="testResultTrend?width=800&height=600">enlarge</a>
    </div>
  </div>
</c:if>

<table style="margin-top: 1em; margin-left:1em;">

  <c:if test="${it.hasJavadoc}">
    <t:summary icon="help.gif">
      <a href="javadoc">Javadoc</a>
    </t:summary>
  </c:if>
  <t:summary icon="folder.gif">
    <a href="ws">Workspace</a>
  </t:summary>

  <t:artifactList caption="Latest Artifacts"
      build="${it.lastSuccessfulBuild}" baseURL="lastSuccessfulBuild/" />

  <c:if test="${tr!=null}">
    <t:summary icon="clipboard.gif">
      <a href="lastSuccessfulBuild/testReport/">Latest Test Result</a>
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
</table>
