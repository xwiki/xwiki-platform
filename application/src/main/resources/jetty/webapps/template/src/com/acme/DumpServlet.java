package com.acme;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DumpServlet extends HttpServlet
{
    
    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        doGet(req, resp);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException
    {
        if (request.getParameter("redirect")!=null)
        {
            response.sendRedirect(request.getParameter("redirect"));
            return;
        }
        
        String furi = (String) request.getAttribute("javax.servlet.forward.request_uri");
        if (furi == null) furi = request.getRequestURI();
        String url = request.getScheme() + "://" + request.getServerName() + ":"
                + request.getServerPort() + furi;

        String uri = (String) request.getAttribute("javax.servlet.forward.request_uri");
        if (uri == null) uri = request.getRequestURI();

        String servlet = (String) request.getAttribute("javax.servlet.include.servlet_path");
        if (servlet == null) servlet = request.getServletPath();

        String info = (String) request.getAttribute("javax.servlet.include.path_info");
        if (info == null) info = request.getPathInfo();

        
        PrintWriter out = null;
        try
        {
            out = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));
        }
        catch (IllegalStateException e)
        {
            out = response.getWriter();
        }

        String referer = request.getHeader("referer");
        response.setContentType("text/html");
        

        out.write("<html><body>");
        out.write("<h1>Dump from " + getServletContext().getServerInfo() + "</h1>");

        /* Some helpful debug
        out.write("<pre>");
        out.write("*** Thread.currentThread().getContextClassLoader()="+Thread.currentThread().getContextClassLoader());
        out.write("\n");
        out.write("*** this.getClass().getClassLoader()="+this.getClass().getClassLoader());
        out.write("\n");
        out.write("*** LogFactory.class.getClassLoader()="+LogFactory.class.getClassLoader());
        out.write("\n");
        try
        {
            Log log = LogFactory.getLog(DumpServlet.class);
            out.write("*** log.getclass().getClassLoader() "+log.getClass().getClassLoader());
            out.write("\n");
	    log.info("some information");
        }
        catch(Throwable th)
        {
            th.printStackTrace(out);
        }
        out.write("\n");
        out.write("</pre>");
	    */
        
        out.write("<h3>Logical Values</h3>");
        out.write("<table border=\"1\"><tr><th>Name</th><th>Value</th><th>Expected Value</th></tr>");
        out.write("<tr><td>Client URL</td><td>" + url
                + "</td><td>Same as browser location bar</td></tr>");
        out.write("<tr><td>Client URI</td><td>" + uri
                + "</td><td>Tail of browser location bar</td></tr>");
        out.write("<tr><td>This Servlet</td><td>" + servlet + "</td><td>/dump</td></tr>");
        out.write("<tr><td>This Info</td><td>" + info + "</td><td>/info</td></tr>");
        out.write("</table>");

        // loop over all wrappers
        ServletRequest sr = request;

        while (sr != null)
        {
            out.write("<h3>" + sr.getClass() + ":" + sr.hashCode() + "</h3>");

            out.write("<table border=\"1\"><tr><th>Name</th><th>Value</th></tr>");
            out.write("<tr><td>getScheme()</td><td>" + sr.getScheme() + "</td></tr>");
            out.write("<tr><td>getServerName()</td><td>" + sr.getServerName() + "</td></tr>");
            out.write("<tr><td>getServlerPort()</td><td>" + sr.getServerPort() + "</td></tr>");

            if (sr instanceof HttpServletRequest)
            {
                HttpServletRequest hsr = (HttpServletRequest) sr;

                out.write("<tr><td>getRequestURL()</td><td>" + hsr.getRequestURL() + "</td></tr>");
                out.write("<tr><td>getRequestURI()</td><td>" + hsr.getRequestURI() + "</td></tr>");
                out.write("<tr><td>getContextPath()</td><td>" + hsr.getContextPath()
                                + "</td></tr>");
                out.write("<tr><td>getServletPath()</td><td>" + hsr.getServletPath()
                                + "</td></tr>");
                out.write("<tr><td>getPathInfo()</td><td>" + hsr.getPathInfo() + "</td></tr>");
            }

            Enumeration e = sr.getAttributeNames();
            while (e.hasMoreElements())
            {
                String name = (String) e.nextElement();
                out.write("<tr><td>getAttribute(\"" + name + "\")</td><td>" + sr.getAttribute(name)
                        + "</td></tr>");
            }
            out.write("</table>");

            if (sr instanceof ServletRequestWrapper)
                sr = ((ServletRequestWrapper) sr).getRequest();
            else
                sr = null;
        }

        if (referer != null) out.write("<p><a href=\"" + referer + "\">BACK</a></p>");
        
        out.write("<h3>Content:</h3><pre>");
        Reader in = request.getReader();
        int len=0;
        char[] buf=new char[8096];
        while ((len=in.read(buf))>=0)
            out.write(buf, 0, len);

        out.write("</pre></body></html>");
        out.flush();
    }
}
