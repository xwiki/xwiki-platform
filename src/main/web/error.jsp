<%@ page isErrorPage="true" %>
<%@ page language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="java.io.PrintWriter"%>
<%
    Object statusCode = request.getAttribute("javax.servlet.error.status_code");
    Object exceptionType = request.getAttribute("javax.servlet.error.exception_type");
    Object message = request.getAttribute("javax.servlet.error.message");
%>

<html>
<head>
<title>XWiki Initialization Error</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<script type="text/javascript">
<!--
function showhide(divname) {
 var style = document.getElementById(divname).style;
 if ((style.display=='block')||(style.display=='')) {
  style.display='none';
  }
 else {
  style.display='block';
  }
}
// -->
</script>
</head>
<body bgcolor="#FFFFFF">
<H2>XWiki Initialization Error</H2>
<p>
An error occured. Please contact the administrator of this server.
</p>
<a href="" onclick="showhide('details'); return false;">Show details</a>
<br />
<div id="details" style="display:none;">
<TABLE CELLPADDING="2" CELLSPACING="2" BORDER="1" WIDTH="100%">
    <TR>
	<TD WIDTH="20%"><B>Status Code</B></TD>
	<TD WIDTH="80%"><%= statusCode %></TD>
    </TR>
    <TR>
	<TD WIDTH="20%"><B>Exception Type</B></TD>
	<TD WIDTH="80%"><%= exceptionType %></TD>
    </TR>
    <TR>
	<TD WIDTH="20%"><B>Message</B></TD>
	<TD WIDTH="80%"><%= message %></TD>
    </TR>
    <TR>
	<TD WIDTH="20%"><B>Exception</B></TD>
	<TD WIDTH="80%">
	    <%
		if( exception != null )
		{
		    out.print("<PRE>");
		    exception.printStackTrace(new PrintWriter(out));
		    out.print("</PRE>");
		}
	    %>
	</TD>
    </TR>
    <TR>
	<TD WIDTH="20%"><B>Root Cause</B></TD>
	<TD>
	    <%
		if( (exception != null) && (exception instanceof ServletException) )
		{
		    Throwable cause = ((ServletException)exception).getRootCause();
		    if( cause != null )
		    {
			out.print("<PRE>");
			cause.printStackTrace(new PrintWriter(out));
			out.print("</PRE>");
		    }
		}
	    %>
	</TD>
    </TR>
</TABLE>
</div>
</body>
</html>