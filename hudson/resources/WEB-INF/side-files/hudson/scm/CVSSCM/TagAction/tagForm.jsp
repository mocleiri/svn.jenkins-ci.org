<%--
  Displays the form to choose the tag name.

  This belongs to a build view.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

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