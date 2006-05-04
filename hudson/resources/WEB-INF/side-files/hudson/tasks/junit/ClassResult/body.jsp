<c:if test="${it.totalCount!=0}">
  <h2>All Tests</h2>
  <table class="pane sortable" id=testresult>
    <tr>
      <td class=pane-header>Test name</td>
      <td class=pane-header style="width:6em">Status</td>
    </tr>
    <tbody>
      <c:forEach var="p" items="${it.children}" varStatus="status">
        <tr>
          <td class=pane><a href="${p.safeName}">${p.name}</td>
          <td class=pane style="width:6em">
            <c:set var="pst" value="${p.status}" />
            <span class="${pst.cssClass}">
              ${pst.message}
            </span>
          </td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</c:if>
