<%--
    displays the status of executors.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<l:pane title="Build Executor Status" width="3">
  <tr>
    <th class="pane">No.</th>
    <th class="pane">Status</th>
    <th class="pane">&nbsp;</th>
  </tr>

  <c:forEach var="e" items="${app.executors}" varStatus="loop">
    <tr>
      <td class="pane">
        ${loop.index+1}
      </td>
      <td class="pane" width="70%">
        <c:choose>
          <c:when test="${e.currentBuild==null}">
            Idle
          </c:when>
          <c:otherwise>
            <div nowrap>Building <a href="${e.currentBuild.url}">${e.currentBuild}</a></div>
            <c:set var="pos" value="${e.progress}" />
            <c:if test="${pos>0}">
              <t:progressBar pos="${pos}"/>
            </c:if>
          </c:otherwise>
        </c:choose>
      </td>
      <td class="pane">&nbsp;</td>
    </tr>
  </c:forEach>
</l:pane>
