<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="h" uri="http://hudson.dev.java.net/" %>

<c:if test="${it.failCount!=0}">
  <h2>Failed Tests</h2>
  <ol>
    <c:forEach var="f" items="${it.failedTests}" varStatus="i">
      <li>
        <a href="failedTests/${i.index}/">${f.fullName}</a>
        <c:set var="fst" value="${f.status}" />
        <c:if test="${fst.regression}">
          <span class="result-regression">(regression)</span>
        </c:if>
      </li>
    </c:forEach>
  </ol>
</c:if>

<c:if test="${it.totalCount!=0}">
  <table class=pane id=testresult>
    <tr>
      <td class="pane-header" colspan="5">All Tests</td>
    </tr>
    <tr>
      <th class=pane>${it.childTitle}</th>
      <th class=pane style="width:5em">Fail</th>
      <th class=pane style="width:1em; font-size:smaller; white-space:nowrap;">(diff)</th>
      <th class=pane style="width:5em">Total</th>
      <th class=pane style="width:1em; font-size:smaller; white-space:nowrap;">(diff)</th>
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
