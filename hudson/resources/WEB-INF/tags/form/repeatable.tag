<%--
  Repetable blocks where duplicates can be added/removed at client.
--%>
<%@attribute name="var" required="true" rtexprvalue="false" %>
<%@attribute name="varStatus" required="false" rtexprvalue="false" %>
<%@attribute name="items" required="true" rtexprvalue="true" type="java.lang.Object" %>

<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="f" tagdir="/WEB-INF/tags/form" %>
<c:set var="repeatBlockId" value="${repeatBlockId+1}" scope="request" />
<script>
  function expand${repeatBlockId}() {
    ip = document.getElementById("block${repeatBlockId}-insertion-point");
    <%--
      importNode isn't supported in IE.
      nc = document.importNode(node,true);
    --%>
    nc = document.createElement("div");
    nc.className = "repeated-chunk";
    nc.innerHTML = block${repeatBlockId}HTML;
    ip.parentNode.insertBefore(nc,ip);
  }

  function delete${repeatBlockId}(ev) {
    t = ev.target;	// DOM Lv2
    if(t==null)
      t = ev.srcElement;	// for IE

    while(t.className!="repeated-chunk")
      t = t.parentNode;
    t.parentNode.removeChild(t);
  }
</script>
<%-- this is the master copy --%>
<div id="block${repeatBlockId}-master" class="repeated-chunk" style="display:none">
  <jsp:doBody />
</div>
<%-- then populate them for each item --%>
<c:forEach var="loop" varStatus="loopStatus" items="${items}">
  <div class="repeated-chunk">
    <%-- how do I do this without using Java code? --%>
    <%
      request.setAttribute((String)var,getJspContext().getAttribute("loop"));
    %>
    <jsp:doBody />
  </div>
</c:forEach>
<script>
  <%-- master needs to be deleted from the form, or its entry will be sent. --%>
  var master${repeatBlockId} = document.getElementById("block${repeatBlockId}-master");
  var block${repeatBlockId}HTML = master${repeatBlockId}.innerHTML;
  master${repeatBlockId}.parentNode.removeChild(master${repeatBlockId});
</script>
<div id="block${repeatBlockId}-insertion-point" />
<input type=button value="Add" onclick="expand${repeatBlockId}()" />
