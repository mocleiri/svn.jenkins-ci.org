<s:section title="Maven">
  <s:entry title="Maven installation"
    description="List of Maven installations on this system. Both maven 1 and 2 are supported.">
    <s:repeatable var="inst" items="${descriptor.installations}">
      <table width="100%">
        <s:entry title="name">
          <input class="setting-input" name="maven_name"
            type="text" value="${inst.name}">
        </s:entry>

        <c:set var="status" value="${null}" />
        <c:if test="${inst!=null && !inst.exists && inst.name!=''}">
          <c:set var="status" value="<span class=error>No such installation exists</span>" />
        </c:if>
        <s:entry title="MAVEN_HOME"
          description="${status}">
          <input class="setting-input" name="maven_home"
            type="text" value="${inst.mavenHome}">
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
