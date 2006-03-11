<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:if test="${it.totalCount!=0}">
  <table class=pane id=testresult>
    <tr>
      <td class="pane-header" colspan="2">All Tests</td>
    </tr>
    <tr>
      <th class=pane>Test name</th>
      <th class=pane style="width:6em">Status</th>
    </tr>
    <tbody>
      <c:forEach var="p" items="${it.children}" varStatus="status">
        <tr>
          <td class=pane><a href="children/${status.index}/">${p.name}</td>
          <td class=pane style="width:6em">
            <span class="${p.status.cssClass}">
              ${p.status.message}
            </span>
          </td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</c:if>
