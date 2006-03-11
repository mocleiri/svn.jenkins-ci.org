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
          <td class=pane>
            <c:choose>
              <c:when test="${p.passed}">
                Passed
              </c:when>
              <c:otherwise>
                Failed
              </c:otherwise>
            </c:choose>
          </td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</c:if>
