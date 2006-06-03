<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<%-- floating box --%>
<div style="float:right">
<c:set var="tr" value="${it.lastSuccessfulBuild.testResultAction}" />
<c:if test="${tr.previousResult!=null}">
  <%-- at least two data points are requierd for a trend report --%>
  <div align="right">
    <c:set var="mode" value="${cookie.TestResultAction_failureOnly.value}" />
    <c:if test="${mode!=null}">
      <c:set var="trendQueryString1" value="?failureOnly=${mode}" />
      <c:set var="trendQueryString2" value="&failureOnly=${mode}" />
    </c:if>
    <div class="test-trend-caption">
      Test Result Trend
    </div>
    <div>
      <a href="testResultTrend?width=800&height=600${trendQueryString2}"><img src="testResultTrend${trendQueryString1}"></a>
    </div>
    <div style="text-align:right">
      <a href="flipTestResultTrend">
        <c:choose>
          <c:when test="${mode}">
            (show test # and failure #)
          </c:when>
          <c:otherwise>
            (just show failures)
          </c:otherwise>
        </c:choose>
      </a> &nbsp;
      <a href="testResultTrend?width=800&height=600${trendQueryString2}">enlarge</a>
    </div>
  </div>
</c:if>
</div>

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

<%-- downstream projects --%>
<c:set var="downstream" value="${it.buildTriggerPublisher.childProjects}" />
<c:if test="${downstream!=null}">
  <h2>Downstream Projects</h2>
  <ul>
    <c:forEach var="item" items="${downstream}">
      <li><a href="../${item.name}/">${item.name}</a></li>
    </c:forEach>
  </ul>
</c:if>