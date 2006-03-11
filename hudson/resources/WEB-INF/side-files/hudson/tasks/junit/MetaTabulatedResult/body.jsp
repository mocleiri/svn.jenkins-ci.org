<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:if test="${it.failCount!=0}">
  <h2>Failed Tests</h2>
  <ol>
    <c:forEach var="f" items="${it.failedTests}" varStatus="i">
      <li>
        <a href="failedTests/${i.index}/">${f.fullName}</a>
      </li>
    </c:forEach>
  </ol>
</c:if>

<c:if test="${it.totalCount!=0}">
  <table class=pane id=testresult>
    <tr>
      <td class="pane-header" colspan="3">All Tests</td>
    </tr>
    <tr>
      <th class=pane>${it.childTitle}</th>
      <th class=pane style="width:5em">Fail</th>
      <th class=pane style="width:5em">Total</th>
    </tr>
    <tbody>
      <c:forEach var="p" items="${it.children}">
        <tr>
          <td class=pane><a href="${p.name}/">${p.name}</td>
          <td class=pane style="text-align:right">${p.failCount}</td>
          <td class=pane style="text-align:right">${p.totalCount}</td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</c:if>
