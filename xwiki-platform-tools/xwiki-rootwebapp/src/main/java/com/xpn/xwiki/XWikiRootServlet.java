package com.xpn.xwiki;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The root servlet for XWiki. The purpose of this servlet is to respond to WebDAV requests correctly and to redirect
 * get requests on server root appropriately.
 * 
 * @version $Id$
 */
public class XWikiRootServlet extends HttpServlet
{
    /** Class version identifier. Needed for serialization. */
    private static final long serialVersionUID = -4907199005755391420L;

    /**
     * The address to redirect to (the name of the XWiki webapp). Configured using the <code>redirectTo</code>
     * initialization parameter in <code>web.xml</code>.
     */
    private String xwiki = "xwiki";

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.GenericServlet#init()
     */
    @Override
    public void init() throws ServletException
    {
        super.init();
        String redirectParameter = getInitParameter("redirectTo");
        if (redirectParameter != null) {
            this.xwiki = redirectParameter;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Responds to an OPTIONS requests on / with appropriate headers.
     * </p>
     */
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException,
        IOException
    {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("MS-Author-Via", "DAV");
        response.setHeader("Content-Language", "en");
        response.setHeader("DAV", "1,2");
        response.setHeader("Allow", "OPTIONS, GET, HEAD, PROPFIND, LOCK, UNLOCK");
        response.setHeader("Content-Length", "0");
        response.flushBuffer();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Redirects GET requests on <code>/</code> (the server root) to the XWiki application context. That is usually
     * <code>/xwiki/</code>, but can be configured from <code>web.xml</code> using the <code>redirectTo</code>
     * initialization parameter for this servlet.
     * </p>
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.sendRedirect(response.encodeRedirectURL(this.xwiki));
    }
}
