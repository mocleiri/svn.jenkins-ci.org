<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>
<%@ taglib prefix="h" uri="http://hudson.dev.java.net/" %>
<%--
  Displays two projects side by side and show their relationship
--%>
<st:include page="sidepanel.jsp" />
<l:main-panel>
  <h1><img src="${rootURL}/images/48x48/search.gif" />Project Rlatipnship</h1>
  <form action="projectRelationship" method="get">
    <table width="100%">
      <tr>
        <c:set var="names" value="${app.jobNames}" />
        <td style="text-align:right;">
          upstream project:
          <s:editableComboBox id="lhs" name="lhs" value="${param.lhs}" items="${names}" />
        </td>
        <td style="width:32px; text-align:center;"><img src="${rootURL}/images/24x24/next.gif" alt="->"/></td>
        <td>
          downstream project:
          <s:editableComboBox id="rhs" name="rhs" value="${param.rhs}" items="${names}" />
        </td>
      </tr>
      <tr>
        <td colspan="3" style="text-align:right">
          <input type="submit" value="Compare" />
          <a href="projectRelationship-help"><img src="${rootURL}/images/16x16/help.gif" /></a>
        </td>
      </tr>

      <c:if test="${!empty(param.lhs) && !empty(param.rhs)}">
        <c:set var="jl" value="${h:getJob(param.lhs)}" />
        <c:set var="jr" value="${h:getJob(param.rhs)}" />

        <c:choose>
          <c:when test="${jl==null}">
            <tr><td colspan="3" class="error">
              No such project '${param.lhs}'
            </td></tr>
          </c:when>
          <c:when test="${jr==null}">
            <tr><td colspan="3" class="error">
              No such project '${param.rhs}'
            </td></tr>
          </c:when>
          <c:otherwise>
            <c:forEach var="e" items="${h:getProjectRelationshipMap(jl,jr)}">
              <tr>
                <td style="text-align:right">
                  <t:buildLink job="${jl}" number="${e.key}"/>
                </td>
                <td>&nbsp;</td>
                <td>
                  <t:buildRangeLink job="${jr}" range="${e.value}"/>
                </td>
              </tr>
            </c:forEach>
          </c:otherwise>
        </c:choose>
      </c:if>
    </table>
  </form>
</l:main-panel>
<l:footer/>
