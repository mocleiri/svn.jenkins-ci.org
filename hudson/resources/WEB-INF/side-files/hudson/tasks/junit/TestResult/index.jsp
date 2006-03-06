<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<st:include it="${it.owner}" page="sidepanel.jsp" />
<l:main-panel>
  <h1>Test Result</h1>

  <c:set var="failCount" value="${f:length(it.failedTests)}" />
  <div>
    <c:choose>
      <c:when test="${it.totalTests==0}">
        No tests
      </c:when>
      <c:otherwise>
        <div>
          ${failCount} failures
        </div>
        <div style="width:100%; height:1em; background-color: #729FCF">
          <div style="width:${failCount*100/it.totalTests}%; height: 1em; background-color: #EF2929"></div>
        </div>
        <div align="right">
          ${it.totalTests} tests
        </div>
      </c:otherwise>
    </c:choose>
  </div>

  <c:if test="${failCount!=0}">
    <h2>Failed Tests</h2>
    <ol>
      <c:forEach var="f" items="${it.failedTests}" varStatus="i">
        <li>
          <a href="failedTests/${i.index}/">${f.fullName}</a>
        </li>
      </c:forEach>
    </ol>
  </c:if>
</l:main-panel>
<l:footer/>
