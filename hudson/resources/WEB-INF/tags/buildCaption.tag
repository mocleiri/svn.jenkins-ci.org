<%--
    displays a caption for build/externalRun.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<h1>
  <c:if test="${it.building}">
    <div style="float:right">
      <table class="middle-align"><tr>
        <td>
          Progress:
        </td><td>
          <t:progressBar pos="${it.executor.progress}"/>
        </td><td>
          <a href="executor/stop"><img src="${rootURL}/images/16x16/stop.gif" alt="[cancel]"></a>
        </td>
      </tr></table>
    </div>
  </c:if>
  
  <img src="buildStatus" width=48 height=48>
  <jsp:doBody />
</h1>
