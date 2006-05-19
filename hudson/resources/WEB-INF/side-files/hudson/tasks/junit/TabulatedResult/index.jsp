<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>
<%@ taglib prefix="h" uri="http://hudson.dev.java.net/" %>

<st:include it="${it.owner}" page="sidepanel.jsp" />
<l:main-panel>
  <h1>${it.title}</h1>

  <c:set var="prev" value="${it.previousResult}" />

  <div>
    <c:choose>
      <c:when test="${it.totalCount==0}">
        No tests
      </c:when>
      <c:otherwise>
        <div>
          ${it.failCount} failures
          <c:if test="${prev!=null}">
            (${h:diff(it.failCount-prev.failCount)})
          </c:if>
        </div>
        <div style="width:100%; height:1em; background-color: #729FCF">
          <div style="width:${it.failCount*100/it.totalCount}%; height: 1em; background-color: #EF2929"></div>
        </div>
        <div align="right">
          ${it.totalCount} tests
          <c:if test="${prev!=null}">
            (${h:diff(it.totalCount-prev.totalCount)})
          </c:if>
        </div>
      </c:otherwise>
    </c:choose>
  </div>

  <st:include page="body.jsp" />
</l:main-panel>
<l:footer/>
