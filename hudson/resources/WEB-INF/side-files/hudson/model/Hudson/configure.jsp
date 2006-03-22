<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>
<%--
  Config page
--%>
<st:include page="sidepanel.jsp" />

<l:main-panel>
  <s:form method="post" action="configSubmit">
    <s:entry title="Home directory">
      ${it.rootDir}
    </s:entry>
    <s:entry title="# of executors">
      <input type="text" name="numExecutors" class="setting-input"
        value="${it.numExecutors}"/>
    </s:entry>
    <s:entry title="Quiet period"
      description="
        A newly scheduled build wait for this many seconds before actually built.
        This is useful for collapsing multiple CVS change notification e-mails to one.
      ">
      <input class="setting-input" name="quiet_period"
        type="text" value="${it.quietPeriod}">
    </s:entry>
    <s:entry title="Enable security"
      description="If enabled, you have to log in before changing the configuration or running a new build.">
      <input type="checkbox" name="use_security"
        <c:if test="${it.useSecurity}">checked</c:if>>
    </s:entry>


    <s:section title="Maser/Slave Support">
      <s:entry title="Slaves"
        description="
          List of slave nodes register to this master Hudson. Jobs can be configured to
          execute on slave nodes to handle a large number of jobs.
        ">
        <s:repeatable var="inst" items="${it.slaves}">
          <table width="100%">
            <s:entry title="name" description="
                Name that uniquely identifies a slave
              ">
              <input class="setting-input" name="slave_name"
                type="text" value="${inst.name}">
            </s:entry>

            <s:entry title="connect command" description="
                Command used to execute a program on this slave, such as
                'ssh slave1' or 'rsh slave2', but it can be a custom program as well.
                Hudson will append the actual command and arguments.
              ">
              <input class="setting-input" name="slave_command"
                type="text" value="${inst.command}">
            </s:entry>

            <s:entry title="local FS root" description="
                Slave needs to have a directory dedicated for Hudson, and
                it needs to be visible from this master Hudson. Specify
                the path (from the viewpoint of the master Hudson) to this
                work directory, such as '/net/slave1/hudson'
              ">
              <input class="setting-input" name="slave_localFS"
                type="text" value="${inst.localFS}">
            </s:entry>

            <s:entry title="remote FS root" description="
                Specify the same path you specified above in 'remote FS root',
                but this time from the viewpoint of the slave node, such
                as '/hudson'
              ">
              <input class="setting-input" name="slave_remoteFS"
                type="text" value="${inst.remoteFS}">
            </s:entry>

            <s:entry title="">
              <div align="right">
                <s:repeatableDeleteButton />
              </div>
            </s:entry>
          </table>
        </s:repeatable>
      </s:entry>
    </s:section>

    <s:section title="JDKs">
      <s:entry title="JDK installations"
               description="List of JDK installations on this system">
        <s:repeatable var="inst" items="${it.JDKs}">
          <table width="100%">
            <s:entry title="name">
              <input class="setting-input" name="jdk_name"
                type="text" value="${inst.name}">
            </s:entry>

            <c:set var="status" value="${null}" />
            <c:if test="${inst!=null && !inst.exists && inst.name!=''}">
              <c:set var="status" value="<span class=error>No such JDK exists</span>" />
            </c:if>
            <s:entry title="JAVA_HOME" description="${status}">
              <input class="setting-input" name="jdk_home"
                type="text" value="${inst.javaHome}">
            </s:entry>
            <s:entry title="">
              <div align="right">
                <s:repeatableDeleteButton />
              </div>
            </s:entry>
          </table>
        </s:repeatable>
      </s:entry>
    </s:section>

    <%-- build config pane --%>
    <c:set var="builds" value="<%= hudson.tasks.BuildStep.BUILDERS %>" />
    <c:forEach var="idx" begin="0" end="${f:length(builds)-1}">
      <c:set var="descriptor" value="${builds[idx]}" scope="request" />
      <jsp:include page="${descriptor.globalConfigPage}"/>
    </c:forEach>

    <%-- SCM config pane --%>
    <c:set var="scms" value="<%= hudson.scm.SCMManager.getSupportedSCMs() %>" />
    <c:forEach var="idx" begin="0" end="${f:length(scms)-1}">
      <c:set var="descriptor" value="${scms[idx]}" scope="request" />
      <jsp:include page="${descriptor.globalConfigPage}"/>
    </c:forEach>

    <%-- build config pane --%>
    <c:set var="pubs" value="<%= hudson.tasks.BuildStep.PUBLISHERS %>" />
    <c:forEach var="idx" begin="0" end="${f:length(pubs)-1}">
      <c:set var="descriptor" value="${pubs[idx]}" scope="request" />
      <jsp:include page="${descriptor.globalConfigPage}"/>
    </c:forEach>

    <s:block>
      <input type="submit" name="Submit" value="OK" />
    </s:block>
  </s:form>
</l:main-panel>
<l:footer/>
