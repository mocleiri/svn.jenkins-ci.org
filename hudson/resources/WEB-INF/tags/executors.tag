<%--
    displays the status of executors.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<l:pane title="Build Executor Status" width="3">
  <c:forEach var="c" items="${app.computers}" varStatus="cloop">
    <c:choose>
      <c:when test="${c.node==app}">
        <tr>
          <th class="pane">No.</th>
          <th class="pane">Status</th>
          <th class="pane">&nbsp;</th>
        </tr>
      </c:when>
      <c:otherwise>
        <tr>
          <th class="pane" colspan="3">
            <a href="${rootURL}/computer/${c.displayName}">${c.displayName}</a>
            <c:if test="${c.temporarilyOffline}">(offline)</c:if>
          </th>
        </tr>
      </c:otherwise>
    </c:choose>

    <c:forEach var="e" items="${c.executors}" varStatus="eloop">
      <tr>
        <td class="pane">
          ${eloop.index+1}
        </td>
        <c:choose>
          <c:when test="${e.currentBuild==null}">
            <td class="pane" width="70%">
              Idle
            </td>
            <td class="pane"></td>
          </c:when>
          <c:otherwise>
            <td class="pane" width="70%">
              <div nowrap>Building <a href="${rootURL}/${e.currentBuild.url}">${e.currentBuild}</a></div>
              <t:progressBar pos="${e.progress}" href="${rootURL}/${e.currentBuild.url}console"/>
            </td>
            <td class="pane" width=16 align=center valign=middle>
              <a href="${rootURL}/computers/${cloop.index}/executors/${eloop.index}/stop"><img src="${rootURL}/images/16x16/stop.gif" alt="terminate this build" /></a>
            </td>
          </c:otherwise>
        </c:choose>
      </tr>
    </c:forEach>
  </c:forEach>
</l:pane>
