<%-- show the icon legend --%>
<st:include page="sidepanel.jsp" />
<l:main-panel>
  <table cellpadding=5>
    <tr><td>
      <img src="images/48x48/grey.gif">
      The project has never been built before.
    </td></tr>
    <tr><td>
      <img src="images/48x48/grey_anime.gif">
      The first build of the project is in progress.
    </td></tr>
    <tr><td>
      <img src="images/48x48/blue.gif">
      The last build was successful.
    </td></tr>
    <tr><td>
      <img src="images/48x48/blue_anime.gif">
      The last build was successful. A new build is in progress.
    </td></tr>
    <tr><td>
      <img src="images/48x48/yellow.gif">
      The last build was successful but unstable.
      This is primarily used to represent test failures.
    </td></tr>
    <tr><td>
      <img src="images/48x48/yellow_anime.gif">
      The last build was successful but unstable. A new build is in progress.
    </td></tr>
    <tr><td>
      <img src="images/48x48/red.gif">
      The last build fatally failed.
    </td></tr>
    <tr><td>
      <img src="images/48x48/red_anime.gif">
      The last build fatally failed. A new build is in progress.
    </td></tr>
  </table>

</l:main-panel>
<l:footer/>
