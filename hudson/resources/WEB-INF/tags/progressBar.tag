<%--
  Progress bar. @pos (0-100) specifies the current position
--%>
<%@attribute name="pos" required="true" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<table class="progress-bar">
  <tbody><tr>
    <td class="progress-bar-done"><div style="width:${pos}px;"></div></td>
    <td class="progress-bar-left"><div style="width:${100-pos}px"></div></td>
  </tr>
</tbody></table>
