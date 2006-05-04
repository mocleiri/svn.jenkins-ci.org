<%--
  Displays the form to choose the tag name.

  This belongs to a build view.
--%>
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
      <th></th>
    </tr>
    <c:forEach var="e" items="${it.fingerprints}">
      <c:set var="f" value="${e.value}" />
      <tr>
        <td>
          ${e.key}
        </td>
        <td>
          <c:choose>
            <c:when test="${f.original==null}">
              outside Hudson
            </c:when>
            <c:when test="${f.original.run==it.build}">
              this build
            </c:when>
            <c:otherwise>
              <t:buildLink jobName="${f.original.name}" job="${f.original.job}" number="${f.original.number}" />
            </c:otherwise>
          </c:choose>
        </td>
        <td>
          ${f.timestampString} old
        </td>
        <td>
          <a href="${rootURL}/fingerprint/${f.hashString}/">
            <img src="${rootURL}/images/16x16/fingerprint.gif"> more details
          </a>
        </td>
      </tr>
    </c:forEach>
  </table>
</l:main-panel>
<l:footer/>