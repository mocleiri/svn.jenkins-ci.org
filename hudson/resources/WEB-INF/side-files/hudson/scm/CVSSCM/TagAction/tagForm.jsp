<%--
  Displays the form to choose the tag name.

  This belongs to a build view.
--%>
<st:include it="${it.build}" page="sidepanel.jsp" />
<l:main-panel>
  <h1>Build #${it.build.number}</h1>
  <form action="submit" method="get">
    <p>
      Choose the CVS tag name for this build:
      <input type="text" name="name" value="hudson-${it.build.number}" />
      <input type="submit" />
    </p>
  </form>
</l:main-panel>
<l:footer/>