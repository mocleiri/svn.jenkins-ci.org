<%--
  Displays the console output as plain/text
--%><%@ page contentType="text/plain;charset=UTF-8" language="java"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><c:out value="${it.log}" escapeXml="false" />