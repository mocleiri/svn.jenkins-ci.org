<s:section title="Ant">
  <s:entry title="Ant installation"
    description="List of Ant installations on this system">
    <s:repeatable var="inst" items="${descriptor.installations}">
      <table width="100%">
        <s:entry title="name">
          <input class="setting-input" name="ant_name"
            type="text" value="${inst.name}">
        </s:entry>

        <c:set var="status" value="${null}" />
        <c:if test="${inst!=null && !inst.exists && inst.name!=''}">
          <c:set var="status" value="<span class=error>No such installation exists</span>" />
        </c:if>
        <s:entry title="ANT_HOME"
          description="${status}">
          <input class="setting-input" name="ant_home"
            type="text" value="${inst.antHome}">
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
