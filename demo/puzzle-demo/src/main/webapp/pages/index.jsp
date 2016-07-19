<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Locale" %>
<%@ page import="io.anyway.puzzle.demo.domain.DemoEntity" %>
<%
	String path = request.getContextPath();
	DemoEntity entity= new DemoEntity(123,"name");
%>

<html>
	<head>
		<meta charset="utf-8">
	</head>
	<body>
    	welcome to visit demo jsp, context path: <%=path %> <%=entity.toString() %>
	</body>
</html>