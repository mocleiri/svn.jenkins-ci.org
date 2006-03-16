<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<jsp:include page="sidepanel.jsp" />
<l:main-panel>
<t:buildCaption>
  Build #${it.number}
  (<i:formatDate value="${it.timestamp.time}" type="both" dateStyle="medium" timeStyle="medium"/>)
</t:buildCaption>

<st:include page="logKeep.jsp" />

<table style="margin-top: 1em; margin-left:1em;">
  <c:if test="${it.result!=null && !empty(it.artifacts)}">
    <t:summary icon="package.gif">
      Build Artifacts<br>
      <ul>
        <c:forEach var="f" items="${it.artifacts}">
          <li><a href="artifact/${f}">${f.fileName}</a></li>
        </c:forEach>
      </ul>
    </t:summary>
  </c:if>

  <c:set var="tr" value="${it.testResult}" />
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
