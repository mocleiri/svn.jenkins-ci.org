<%--
  Displays the edit type icon.
--%>
<%@ attribute name="type" type="hudson.scm.EditType" required="true" %>
<img width="16" height="16"
  src="${rootURL}/images/16x16/document_${type.name}.gif"
  title="${type.description}">
