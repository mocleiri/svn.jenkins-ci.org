<%--
  Generate config pages from a list of Descriptors into a section.
--%>
<%@ attribute name="title" required="true" description="caption for the section" %>
<%@ attribute name="descriptors" required="true" type="hudson.model.Descriptor[]" description="descriptors" %>
<%@ attribute name="instances" required="true" type="java.util.Map" description="instances keyed by their descriptors" %>
<%@ attribute name="varName" required="true" type="java.lang.String" description="used as a variable name as well as block name" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<s:section title="${title}">
  <c:forEach var="d" items="${descriptors}"  varStatus="loop">
    <s:optionalBlock name="${varName}${loop.index}" help="${d.helpFile}"
      title="${d.displayName}" checked="${instances[d]!=null}">

      <c:set var="descriptor" value="${d}" scope="request"  />
      <c:set var="instance" value="${instances[d]}" scope="request"  />
      <jsp:include page="${d.configPage}"/>
    </s:optionalBlock>
  </c:forEach>
  <jsp:doBody />
</s:section>
