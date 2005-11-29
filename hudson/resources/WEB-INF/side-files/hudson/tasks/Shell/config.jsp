<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Command"
         description="See <a href='${rootURL}/env-vars.html' target=_new>the list of available environment variables</a>">
  <textarea class="setting-input" name="shell"
    rows="5" style="width:100%">${builder.command}</textarea>
</s:entry>
