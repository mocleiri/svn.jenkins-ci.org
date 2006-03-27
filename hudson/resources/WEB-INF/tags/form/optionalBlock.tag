<%--
  Outer-most tag that forms a form tag.
--%>
<%@attribute name="name" required="true" %>
<%@attribute name="title" required="true" %>
<%@attribute name="checked" required="true" %>
<%@attribute name="help" %><%-- if present, URL to the help screen --%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="f" tagdir="/WEB-INF/tags/form" %>
<c:set var="oe_id" value="${oe_id+1}" scope="request" />
<c:if test="${empty(help)}">
  <c:set var="help" value="${null}" />
</c:if>
<tr id="oe_s${oe_id}"><%-- this ID marks the beginning --%>
  <td colspan="3">
    <script>
        function toggle${oe_id}() {
          var tbl = document.getElementById('oe_s${oe_id}').parentNode;
          var i = false;
          var o = false;

          for( j=0; tbl.rows[j]; j++ ) {
            n = tbl.rows[j];

            if(n.id=="oe_e${oe_id}")
              o = true;

            if( i && !o ) {
              if( n.style.display!="none" )
                n.style.display = "none";
              else
                n.style.display = "";
            }

            if(n.id=="oe_s${oe_id}") {
              <c:if test="${help!=null}">
                j++;
              </c:if>
              i = true;
            }
          }
        }
    </script>
    <input type="checkbox" name="${name}" onclick="javascript:toggle${oe_id}()"
      <c:if test="${checked=='true'}">checked</c:if>>
    ${title}
  </td>
  <c:if test="${help!=null}">
    <td>
      <img src="${rootURL}/images/16x16/help.gif" alt="[?]" class="help-button" helpURL="${rootURL}${help}">
    </td>
  </c:if>
</tr>
<c:if test="${help!=null}">
  <tr><td></td><td colspan="2"><div class="help">Loading...</div></td><td></td></tr>
</c:if>
<jsp:doBody />
<%-- end marker --%>
<tr id="oe_e${oe_id}" style="display:none">
  <c:if test="${checked=='false'}">
    <script>
      toggle${oe_id}();
    </script>
  </c:if>
</tr>