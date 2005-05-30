<%--
  Radiobox with foldable details
--%>
<%@attribute name="name" required="true" %>
<%@attribute name="title" required="true" %>
<%@attribute name="checked" required="true" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="f" tagdir="/WEB-INF/tags/form" %>
<c:set var="rb_id" value="${rb_id+1}" scope="request" />

<tr id="rb_s${rb_id}"><%-- this ID marks the beginning --%>
  <td colspan="2">
    <script>
        function toggleRb${rb_id}() {
          var tbl = document.getElementById('rb_s${rb_id}').parentNode;
          var i = false;
          var o = false;

          for( j=0; tbl.rows[j]; j++ ) {
            n = tbl.rows[j];

            if(n.id=="rb_e${rb_id}")
              o = true;

            if( i && !o ) {
              if( !Rb${rb_id}.checked )
                n.style.display = "none";
              else
                n.style.display = "";
            }

            if(n.id=="rb_s${rb_id}")
              i = true;
          }
        }

        function updateRb${rb_id}() {
          // update other radios
          col = document.getElementById('Rb${rb_id}').form.${name};
          for(c=0;c<col.length;c++)
            eval("toggle"+col.item(c).id+"()");
        }
    </script>
    <input type="radio" name="${name}" onchange="javascript:updateRb${rb_id}()" id="Rb${rb_id}"
      <c:if test="${checked=='true'}">checked</c:if>>
    ${title}
  </td>
</tr>
<jsp:doBody />
<%-- end marker --%>
<tr id="rb_e${rb_id}" style="display:none">
  <c:if test="${checked=='false'}">
    <script>
      toggleRb${rb_id}();
    </script>
  </c:if>
</tr>