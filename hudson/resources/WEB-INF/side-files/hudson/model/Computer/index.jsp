<st:include page="sidepanel.jsp" />
<l:main-panel>
  <%-- temporarily offline switch --%>
  <div style="float:right">
    <form method="get" action="toggleOffline">
      <c:if test="${it.temporarilyOffline}">
        <input type="submit" value="This node is back online"  />
      </c:if>
      <c:if test="${!it.temporarilyOffline}">
        <input type="submit" value="Mark this node temporarily offline"  />
      </c:if>
    </form>
  </div>

  <h1>
    <img src="${rootURL}/images/48x48/${it.icon}" width=48 height=48>
    Slave ${it.displayName}
  </h1>
  
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
