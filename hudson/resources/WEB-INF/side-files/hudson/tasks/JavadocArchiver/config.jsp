<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Javadoc directory"
  description="Directory relative to the root of the javadoc, such as 'myproject/build/javadoc'">
  <input class="setting-input" name="javadoc_dir"
    type="text" value="${publisher.javadocDir}">
</s:entry>
