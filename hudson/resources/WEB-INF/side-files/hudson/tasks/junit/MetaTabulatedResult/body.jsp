<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="h" uri="http://hudson.dev.java.net/" %>

<c:if test="${it.failCount!=0}">
  <h2>All Failed Tests</h2>
  <table class="pane sortable">
    <tr>
      <td class="pane-header">Test Name</td>
      <td class="pane-header" style="width:4em">Age</td>
    </tr>
    <c:forEach var="f" items="${it.failedTests}" varStatus="i">
      <tr>
        <td class=pane>
          <a href="${h:getTestRelativePathFrom(it,f)}">${f.fullName}</a>
        </td>
        <td class=pane style="text-align:right;">
          ${f.age}
        </td>
      </tr>
    </c:forEach>
  </table>
</c:if>

<c:if test="${it.totalCount!=0}">
  <h2>All Tests</h2>
  <table class="pane sortable" id=testresult>
    <tr>
      <td class=pane-header>${it.childTitle}</td>
      <td class=pane-header style="width:5em">Fail</td>
      <td class=pane-header style="width:1em; font-size:smaller; white-space:nowrap;">(diff)</td>
      <td class=pane-header style="width:5em">Total</td>
      <td class=pane-header style="width:1em; font-size:smaller; white-space:nowrap;">(diff)</td>
    </tr>
    <tbody>
      <c:forEach var="p" items="${it.children}">
        <c:set var="prev" value="${p.previousResult}" />
        <tr>
          <td class=pane><a href="${p.name}/">${p.name}</td>
          <td class=pane style="text-align:right">${p.failCount}</td>
          <td class=pane style="text-align:right">
            ${h:diff2(p.failCount-prev.failCount)}
          </td>
          <td class=pane style="text-align:right">${p.totalCount}</td>
          <td class=pane style="text-align:right">
            ${h:diff2(p.totalCount-prev.totalCount)}
          </td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</c:if>
