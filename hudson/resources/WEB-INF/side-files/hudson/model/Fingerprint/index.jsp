<l:header title="${it.fileName}">
  <!-- RSS? -->
</l:header>
<l:side-panel>
  <l:tasks>
    <l:task icon="images/24x24/up.gif" href="${rootURL}/" title="Back to Dashboard" />
  </l:tasks>
</l:side-panel>
<l:main-panel>
  <h1>
    <img src="${rootURL}/images/48x48/fingerprint.gif">
    ${it.fileName}
  </h1>
  <div class=md5sum>
    MD5: ${it.hashString}
  </div>
  <div>
    Introduced ${it.timestampString} ago
    <c:choose>
      <c:when test="${it.original==null}">
        outside Hudson
      </c:when>
      <c:otherwise>
        <t:buildLink job="${it.original.job}" number="${it.original.number}" jobName="${it.original.name}" />
      </c:otherwise>
    </c:choose>
  </div>
  <h2>Usage</h2>
  <p>
    This file has been used in the following places:
  </p>
  <table class="fingerprint-summary">
    <c:forEach var="j" items="${it.jobs}">
      <c:set var="job" value="${h:getJob(j)}" />
      <c:set var="range" value="${it.usages[j]}" />
      <tr>
        <td class="fingerprint-summary-header">
          <c:choose>
            <c:when test="${job!=null}">
              <a href="${rootURL}/${job.url}">${j}</a>
            </c:when>
            <c:otherwise>
              ${j}
            </c:otherwise>
          </c:choose>
        </td>
        <td>
          <c:forEach var="r" items="${range.ranges}">
            <c:choose>
              <c:when test="${r.start==r.end-1}">
                <t:buildLink job="${job}" number="${r.start}" />
              </c:when>
              <c:when test="${r.start==r.end-2}">
                <t:buildLink job="${job}" number="${r.start}" />
                <t:buildLink job="${job}" number="${r.end-1}" />
              </c:when>
              <c:otherwise>
                <t:buildLink job="${job}" number="${r.start}" />-<t:buildLink job="${job}" number="${r.end-1}" />
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </td>
      </tr>
    </c:forEach>
  </table>
</l:main-panel>
<l:footer/>
