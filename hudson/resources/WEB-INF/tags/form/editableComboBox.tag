<%@attribute name="id" required="true" %>
<%@attribute name="clazz" required="false" %>
<%@attribute name="name" required="false" %>
<%@attribute name="value" required="false" %>
<%@attribute name="items" type="java.lang.Object" description="Optional list of possible values" %>
<%-- Tomcat doesn't like us using the attribute called 'class' --%>

<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<c:if test="${editableComboBox_source_loaded==null}">
  <script type="text/javascript" src="${pageContext.request.contextPath}/scripts/utilities.js"></script>
  <script type="text/javascript" src="${pageContext.request.contextPath}/scripts/combobox.js"></script>
  <c:set var="editableComboBox_source_loaded" scope="request" value="true" />
</c:if>

<input id="${id}" autocomplete="off"
    <c:if test="${clazz!=null}">class="${clazz}"</c:if>
    <c:if test="${name!=null}"  >name="${name}"</c:if>
    <c:if test="${value!=null}">value="${value}"</c:if>
    />
<c:set var="editableComboBox" scope="request" value="${id}" />
<script type="text/javascript">
  var ${id}_values = new Array();

  <%-- fill in values --%>
  <c:if test="${items!=null}">
    <c:forEach var="v" items="${items}">
      <s:editableComboBoxValue value="${v}" />
    </c:forEach>
  </c:if>
  <jsp:doBody />

  function ${id}_Callback(value /*, comboBox*/) {
    var items = new Array();
    var candidates = ${id}_values;
    if (value.length > 0) { // if no value, we'll not provide anything
      value = value.toLowerCase();
      for (var i = 0; i < Math.min(candidates.length, 20); i++) {
        if (candidates[i].toLowerCase().indexOf(value) >= 0) {
          items.push(candidates[i]);
        }
      }
    }
    return items; // equiv to: comboBox.setItems(items);
  }

  <%-- IE doesn't like a combobox to be created before the page is fully loaded. --%>
  var oldOnLoadFor${id} = window.onload;
  window.onload = function() { if(oldOnLoadFor${id}) oldOnLoadFor${id}();
    new ComboBox("${id}", ${id}_Callback);
  }
</script>
