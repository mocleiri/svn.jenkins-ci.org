<%--
    displays a link with a large icon. Used in the project top page
--%>
<%@ attribute name="icon" required="true" %>
<tr>
  <td><img src="${rootURL}/images/48x48/${icon}" width="48" height="48" style="margin-right:1em" /></td>
  <td style="vertical-align:middle"><jsp:doBody /></td>
</tr>
