<%--
  Config page
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>

<s:entry title="Disable" help="/help/project-config/disable.html">
  <input type="checkbox" name="disable" <c:if test="${it.disabled}">checked</c:if>>
  (No new builds will be executed until the project is re-enabled.)
</s:entry>


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
    <s:optionalBlock name="hasCustomQuietPeriod" title="Quiet period" checked="${it.hasCustomQuietPeriod}"
        help="/help/project-config/quietPeriod.html">
      <s:entry title="Quiet period"
        description="Number of seconds">
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


<%-- build triggers config pane --%>
<s:descriptorList title="Build Triggers"
                  descriptors="<%= hudson.triggers.Trigger.TRIGGERS %>"
                  instances="${it.triggers}"
                  varName="trigger" />


<%-- build config pane --%>
<s:descriptorList title="Build"
                  descriptors="<%= hudson.tasks.BuildStep.BUILDERS %>"
                  instances="${it.builders}"
                  varName="builder" />


<%-- publisher config pane --%>
<s:descriptorList title="Post-build Actions"
                  descriptors="<%= hudson.tasks.BuildStep.PUBLISHERS %>"
                  instances="${it.publishers}"
                  varName="publisher" />
