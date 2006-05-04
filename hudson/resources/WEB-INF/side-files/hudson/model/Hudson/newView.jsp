<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--
  New View page
--%>
<st:include page="sidepanel.jsp" />

<l:main-panel>
  <s:form method="post" action="createView">
    <s:entry title="View name">
      <input type="text" name="name" class="setting-input" />
    </s:entry>

    <s:block>
      <input type="submit" name="Submit" value="OK" />
    </s:block>
  </s:form>
</l:main-panel>
<l:footer/>
