<%-- report a login error --%>
<st:include page="sidepanel.jsp" />
<l:main-panel>
  <div style="margin: 2em;">
    <%-- login form --%>
    <form action="j_security_check" method="post" style="text-size:smaller">
      <table>
        <tr>
          <td>User:</td>
          <td><input type="text" name="j_username" /></td>
        </tr>
        <tr>
          <td>Password</td>
          <td><input type="password" name="j_password" /></td>
        </tr>
      </table>
      <input type="submit" value="login" />
    </form>
  </div>
</l:main-panel>
<l:footer/>
