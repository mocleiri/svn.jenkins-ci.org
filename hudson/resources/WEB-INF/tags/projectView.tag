<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="jobs" required="true" type="java.util.Collection" %>
<div class="dashboard">

  <%-- view tab bar --%>
  <table cellpadding=0 cellspacing=0 id="viewList">
    <c:set var="views" value="${app.views}" />
    <%-- dummy row to get spacing right --%>
    <tr style="height:3px;">
      <td style="height:3px; padding:0px"></td>
      <c:forEach var="v" varStatus="vs" items="${app.views}">
        <c:choose>
          <c:when test="${v==it}">
            <td class=active rowspan=2>${v.viewName}</td>
            <c:set var="activeIndex" value="${vs.index}" />
          </c:when>
          <c:otherwise>
            <td style="height:3px; padding:0px"></td>
          </c:otherwise>
        </c:choose>
      </c:forEach>
    </tr>
    <tr>
      <td style="border: none; border-bottom: 1px solid #bbb;">&nbsp;</td>
      <c:forEach var="v" varStatus="vs" items="${app.views}">
        <c:if test="${v!=it}">
          <td class="inactive
            <c:choose>
              <c:when test="${vs.index<activeIndex}">noRight</c:when>
              <c:when test="${vs.index>activeIndex}">noLeft</c:when>
            </c:choose>
            ">
              <a href="${rootURL}/${v.url}">${v.viewName}</a>
          </td>
        </c:if>
      </c:forEach>
      <td class="inactive noLeft"><a href="${rootURL}/newView">+</a></td>
      <td class="filler">&nbsp;</td>
    </tr>
  </table>
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
        <img width="32" height="32" src="${rootURL}/${job.buildStatusUrl}" />
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
          <a href="${rootURL}/${job.url}build"><img src="${rootURL}/images/24x24/gears_run.gif" title="Schedule a build" border=0></a>
        </td>
      </l:isAdmin>
    </tr>
  </c:forEach>
</table>
<div align=right style="margin:1em">
    <a href="legend">Legend</a>
    &nbsp;&nbsp;&nbsp;&nbsp;
    <a href="rssAll"><img src="${rootURL}/images/atom.png" border=0> for all</a>
    &nbsp;&nbsp;&nbsp;&nbsp;
    <a href="rssFailed"><img src="${rootURL}/images/atom.png" border=0> for failures</a>
</div>
