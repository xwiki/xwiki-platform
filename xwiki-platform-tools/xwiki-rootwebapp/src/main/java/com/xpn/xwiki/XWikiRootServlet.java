package com.xpn.xwiki;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class XWikiRootServlet extends HttpServlet
{
    private static final long serialVersionUID = -4907199005755391420L;    

    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {        
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("MS-Author-Via", "DAV");
        response.setHeader("Content-Language", "en");
        response.setHeader("DAV", "1,2");
        response.setHeader("Allow", "OPTIONS, GET, HEAD, PROPFIND, LOCK, UNLOCK");
        response.setHeader("Content-Length", "0");
        response.flushBuffer();                
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {        
        resp.sendRedirect("/xwiki/");
    }
        
}
