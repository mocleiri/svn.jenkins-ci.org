<%--
  History of runs.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<l:pane width="2" title="Build history">
  <%-- pending build --%>
  <c:if test="${it.inQueue}">
    <tr class=build-row>
      <td nowrap=nowrap>
        <img width="16" height="16" src="${rootURL}/images/16x16/grey.gif">&nbsp;
        #${it.nextBuildNumber}
      </td>
      <td nowrap=nowrap>
        (pending)
      </td>
    </tr>
  </c:if>

  <%-- build history --%>
  <c:forEach var="build" items="${it.builds}">
    <c:set var="link" value="${rootURL}/${it.url}${build.number}/" />
    <tr class="build-row">
      <td nowrap="nowrap">
        <img width="16" height="16" src="${rootURL}/images/16x16/${build.buildStatusUrl}">&nbsp;
        #${build.number}
      </td>
      <td nowrap="nowrap">
        <a class="tip" href="${link}">
          <i:formatDate value="${build.timestamp.time}" type="both" dateStyle="medium" timeStyle="medium"/>
        </a>
      </td>
    </tr>
    <c:if test="${build.building}">
      <tr><td></td><td style="padding:0">
        <table class="middle-align">
          <tr><td>
            <t:progressBar pos="${build.executor.progress}" href="${link}console"/>
          </td><td style="padding:0">
            <a href="${link}executor/stop"><img src="${rootURL}/images/16x16/stop.gif" alt="[cancel]"></a>
          </td></tr>
        </table>
      </td></tr>
    </c:if>
  </c:forEach>
  <tr class=build-row>
    <td colspan=2 align=right>
      <a href="rssAll"><img src="${rootURL}/images/atom.gif" border=0> for all</a>
      <a href="rssFailed"><img src="${rootURL}/images/atom.gif" border=0> for failures</a>
    </td>
  </tr>
</l:pane>
