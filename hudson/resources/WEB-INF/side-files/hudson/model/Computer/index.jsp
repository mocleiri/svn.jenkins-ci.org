<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<st:include page="sidepanel.jsp" />
<l:main-panel>
  <h1>Slave ${it.displayName}</h1>

  <h2>Projects tied on ${it.displayName}</h2>
  <c:set var="jobs" value="${it.tiedJobs}" />
  <c:choose>
    <c:when test="${empty(jobs)}">
      <p>
        None
      </p>
    </c:when>
    <c:otherwise>
      <t:projectView jobs="${it.tiedJobs}" showViewTabs="false" />
    </c:otherwise>
  </c:choose>

</l:main-panel>
<l:footer/>
