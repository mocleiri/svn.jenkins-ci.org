<%--
  Use AJAX to load text data progressively.
  This is used to achieve the effect of "tail -f"
  without relying on full page reload.
--%>
<%@attribute name="href" required="true" description="URL that returns text data" %>
<%@attribute name="idref" required="true" description="ID of the HTML element in which the result is displayed" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  script code
--%>
<script>
  <c:if test="${requestScope.progressiveTextScript==null}">
    <c:set scope="request" var="progressiveTextScript" value="initialized" />
    <%-- fetches the latest update from the server --%>
    function fetchNext(e,href) {
      new Ajax.Request(href,{
          method: "get",
          parameters: "start="+e.fetchedBytes,
          onComplete: function(rsp,_) {
            e.appendChild(document.createTextNode(rsp.responseText));
            e.fetchedBytes = rsp.getResponseHeader("X-Text-Size");
            if(rsp.getResponseHeader("X-More-Data")=="true")
              setTimeout(function(){fetchNext(e,href);},1000);
          }
      });
    }
  </c:if>
  $("${idref}").fetchedBytes = 0;
  fetchNext($("${idref}"),"${href}");

  
</script>
