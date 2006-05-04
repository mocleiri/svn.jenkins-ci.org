<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<st:include page="sidepanel.jsp" />
<l:main-panel>
  <h1>Project ${it.name}</h1>
  <div>
    <c:out value="${it.description}" escapeXml="false" />
  </div>

  <%-- inject main part here --%>
  <st:include page="main.jsp" />

  <h2>Permalinks</h2>
  <ul>
    <c:if test="${it.lastBuild!=null}">
      <li>
        <a href="lastBuild">Last build
          (#${it.lastBuild.number}), ${it.lastBuild.timestampString} ago</a>
      </li>
    </c:if>
    <c:if test="${it.lastStableBuild!=null}">
      <li>
        <a href="lastStableBuild">Last stable build
          (#${it.lastStableBuild.number}), ${it.lastStableBuild.timestampString} ago</a>
      </li>
    </c:if>
    <c:if test="${it.lastSuccessfulBuild!=null}">
      <li>
        <a href="lastSuccessfulBuild">Last successful build
          (#${it.lastSuccessfulBuild.number}), ${it.lastSuccessfulBuild.timestampString} ago</a>
      </li>
    </c:if>
    <c:if test="${it.lastFailedBuild!=null}">
      <li>
        <a href="lastFailedBuild">Last failed build
          (#${it.lastFailedBuild.number}), ${it.lastFailedBuild.timestampString} ago</a>
      </li>
    </c:if>
  </ul>
</l:main-panel>
<l:footer/>
