<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="jobs" required="true" type="java.util.Collection" %>
<div class="dashboard">

  <%-- view tab bar --%>
  <l:tabBar>
    <c:forEach var="v" items="${app.views}">
      <l:tab name="${v.viewName}" active="${v==it}" href="${rootURL}/${v.url}" />
    </c:forEach>
    <l:tab name="+" href="${rootURL}/newView" active="false" />
  </l:tabBar>
  <%-- project list --%>
  <table id="projectstatus" class="pane">
    <tr style="border-top: 0px;">
      <th>&nbsp;</th>
      <th>Job</th>
      <th>Last Success</th>
      <th>Last Failure</th>
      <th>Last Duration</th>
      <th>&nbsp;</th>
      <l:isAdmin><%-- build icon --%>
        <th width=1>&nbsp;</th>
      </l:isAdmin>
    </tr>

  <c:forEach var="job" items="${jobs}">
    <c:set var="lsBuild" value="${job.lastSuccessfulBuild}" />
    <c:set var="lfBuild" value="${job.lastFailedBuild}" />
    <tr>
      <td>
        <img width="32" height="32" src="${rootURL}/images/32x32/${job.buildStatusUrl}" />
      </td>
      <td>
        <a href="${rootURL}/${job.url}">
            ${job.name}
        </a>
      </td>
      <td>
          ${lsBuild!=null ? lsBuild.timestampString : "N/A"}
        <c:if test="${lsBuild!=null}">
          (<a href="${rootURL}/${job.url}lastSuccessfulBuild">#${lsBuild.number}</a>)
        </c:if>
      </td>
      <td>
          ${lfBuild!=null ? lfBuild.timestampString : "N/A"}
        <c:if test="${lfBuild!=null}">
          (<a href="${rootURL}/${job.url}lastFailedBuild">#${lfBuild.number}</a>)
        </c:if>
      </td>
      <td>
          ${lsBuild!=null ? lsBuild.durationString : "N/A"}
      </td>
      <td>
        &nbsp;
      </td>
      <l:isAdmin>
        <td>
          <a href="${rootURL}/${job.url}build"><img src="${rootURL}/images/24x24/clock.png" title="Schedule a build" border=0></a>
        </td>
      </l:isAdmin>
    </tr>
  </c:forEach>
</table>
<div align=right style="margin:1em">
    <a href="${rootURL}/legend">Legend</a>
    &nbsp;&nbsp;&nbsp;&nbsp;
    <a href="rssAll"><img src="${rootURL}/images/atom.png" border=0> for all</a>
    &nbsp;&nbsp;&nbsp;&nbsp;
    <a href="rssFailed"><img src="${rootURL}/images/atom.png" border=0> for failures</a>
</div>
