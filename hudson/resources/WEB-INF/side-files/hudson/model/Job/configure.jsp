<%--
  Config page. derived class specific entries should go to configure-entries.jsp
--%>
<st:include page="sidepanel.jsp" />
<l:main-panel>
  <s:form method="post" action="configSubmit">
    <s:entry title="Project name">
      ${it.name}
    </s:entry>
    <s:entry title="Description" help="/help/project-config/description.html">
      <textarea class="setting-input" name="description"
        rows="5" style="width:100%">${it.description}</textarea>
    </s:entry>

    <%-- log rotator --%>
    <s:optionalBlock name="logrotate"
      help="/help/project-config/log-rotation.html"
      title="Log Rotation" checked="${it.logRotator!=null}">
      <jsp:include page="../../tasks/LogRotator/config.jsp"/>
    </s:optionalBlock>

    <%-- additional entries from derived classes --%>
    <st:include page="configure-entries.jsp" />

    <s:block>
      <input type="submit" name="Submit" value="Save" />
    </s:block>
  </s:form>
</l:main-panel>
<l:footer/>

