<%--
  Displays the form to choose the tag name.

  This belongs to a build view.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<st:include it="${it.build}" page="sidepanel.jsp" />
<l:main-panel>
  <h1>
    <img src="${rootURL}/images/48x48/fingerprint.gif">
    Recorded Fingerprints
  </h1>
  <table class="fingerprint-in-build sortable">
    <tr>
      <th>File</th>
      <th>Original owner</th>
      <th>Age</th>
    </tr>
    <c:forEach var="e" items="${it.fingerprints}">
      <c:set var="f" value="${e.value}" />
      <tr>
        <td>
          <a href="${rootURL}/fingerprint/${f.hashString}/">
            <img src="${rootURL}/images/16x16/text.gif">${e.key}
          </a>
        </td>
        <td>
          <c:choose>
            <c:when test="${f.original==null}">
              (outside Hudson)
            </c:when>
            <c:when test="${f.original.run==it.build}">
              (this build)
            </c:when>
            <c:otherwise>
              <t:buildLink jobName="${f.original.name}" job="${f.original.job}" number="${f.original.number}" />
            </c:otherwise>
          </c:choose>
        </td>
        <td>
          ${f.timestampString} old
        </td>
      </tr>
    </c:forEach>
  </table>
</l:main-panel>
<l:footer/>