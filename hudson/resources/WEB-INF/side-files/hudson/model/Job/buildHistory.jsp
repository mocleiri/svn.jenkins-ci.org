<%--
  History of runs.
--%>
<l:pane width="2" title="Build history">
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
      <c:set var="pos" value="${build.executor.progress}" />
      <c:if test="${pos>0}">
        <tr><td></td><td style="padding:0">
          <table class="middle-align">
            <tr><td>
              <t:progressBar pos="${pos}"/>
            </td><td style="padding:0">
              <a href="${link}executor/stop"><img src="${rootURL}/images/16x16/stop.gif" alt="[cancel]"></a>
            </td></tr>
          </table>
        </td></tr>
      </c:if>
    </c:if>
  </c:forEach>
  <tr class=build-row>
    <td colspan=2 align=right>
      <a href="rssAll"><img src="${rootURL}/images/atom.gif" border=0> for all</a>
      <a href="rssFailed"><img src="${rootURL}/images/atom.gif" border=0> for failures</a>
    </td>
  </tr>
</l:pane>
