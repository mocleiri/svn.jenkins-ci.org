<%--
  Config page
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="jdks" value="${it.parent.JDKs}" />
<c:if test="${!empty(jdks)}">
  <s:entry title="JDK"
           description="JDK to be used for this project">
    <select class="setting-input" name="jdk">
      <option>(Default)</option>
      <c:forEach var="inst" items="${jdks}">
        <option <c:if test="${inst.name==it.JDK.name}">selected</c:if>>${inst.name}</option>
      </c:forEach>
    </select>
  </s:entry>
</c:if>

<%-- master/slave --%>
<c:set var="slaves" value="${it.parent.slaves}" />
<c:if test="${!empty(slaves)}">
  <s:optionalBlock name="hasSlaveAffinity" title="Tie this project to a node" checked="${it.assignedNode!=null}"
      help="/help/project-config/slave.html">
    <s:entry title="Node">
      <select class="setting-input" name="slave">
        <option>(This machine)</option>
        <c:forEach var="s" items="${slaves}">
          <option <c:if test="${s==it.assignedNode}">selected</c:if> value="${s.nodeName}"
            >${s.nodeName} <c:if test="${!empty(s.description)}">(${s.description})</c:if></option>
        </c:forEach>
      </select>
    </s:entry>
  </s:optionalBlock>
</c:if>

<s:section title="Advanced Project Options">
  <s:advanced>
    <%-- custom quiet period --%>
    <s:optionalBlock name="hasCustomQuietPeriod" title="Quiet period" checked="${it.hasCustomQuietPeriod}">
      <s:entry title="Quiet period"
        description="
          If set, a newly scheduled build wait for this many seconds before actually built.
          This is useful for collapsing multiple CVS change notification e-mails to one.
          If set, this value overrides the global setting.
        ">
        <input class="setting-input" name="quiet_period"
          type="text" value="${it.quietPeriod}">
      </s:entry>
    </s:optionalBlock>
  </s:advanced>
</s:section>

<%-- SCM config pane --%>
<s:section title="Source Code Management">
  <c:set var="scms" value="<%= hudson.scm.SCMManager.getSupportedSCMs() %>" />
  <c:forEach var="idx" begin="0" end="${f:length(scms)-1}">
    <c:set var="scmd" value="${scms[idx]}" />
    <s:radioBlock name="scm" value="${idx}" title="${scmd.displayName}" checked="${it.scm.descriptor==scmd}">
      <c:set var="scm" value="${it.scm.descriptor==scmd?it.scm:(null)}" scope="request" />
      <jsp:include page="${scmd.configPage}"/>
    </s:radioBlock>
  </c:forEach>
</s:section>


<%-- build config pane --%>
<s:section title="Build">
  <c:set var="builds" value="<%= hudson.tasks.BuildStep.BUILDERS %>" />
  <c:forEach var="idx" begin="0" end="${f:length(builds)-1}">
    <c:set var="bd" value="${builds[idx]}" />
    <s:optionalBlock name="builder${idx}"
      title="${bd.displayName}" checked="${it.builders[bd]!=null}">

      <c:set var="descriptor" value="${bd}" scope="request"  />
      <c:set var="builder" value="${it.builders[bd]}" scope="request"  />
      <jsp:include page="${bd.configPage}"/>
    </s:optionalBlock>
  </c:forEach>
</s:section>


<%-- publisher config pane --%>
<s:section title="Post-build Actions">
  <c:set var="publishers" value="<%= hudson.tasks.BuildStep.PUBLISHERS %>" />
  <c:forEach var="idx" begin="0" end="${f:length(publishers)-1}">
    <c:set var="pd" value="${publishers[idx]}" />
    <s:optionalBlock name="publisher${idx}"
                     title="${pd.displayName}"
                     checked="${it.publishers[pd]!=null}"
                     help="${pd.helpFile}">
      <c:set var="publisher" value="${it.publishers[pd]}" scope="request"  />
      <jsp:include page="${pd.configPage}"/>
    </s:optionalBlock>
  </c:forEach>
</s:section>
