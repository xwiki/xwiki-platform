// ===========================================================================
// Copyright (c) 1996-2003 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id: DemoFilter.java,v 1.1 2004/08/13 10:28:30 gregwilkins Exp $
// ---------------------------------------------------------------------------

package com.acme;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


/* ------------------------------------------------------------ */
public  class DemoFilter implements Filter
{
    private FilterConfig config;
	
    /* ------------------------------------------------------------ */
    public void init(FilterConfig filterConfig)
        throws ServletException
    {
        config=filterConfig;
    }

    /* ------------------------------------------------------------ */
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
	throws IOException, ServletException
    {
        // Do your funky filter thang here ...
        chain.doFilter(request, response);   
    }

    /* ------------------------------------------------------------ */
    public void destroy()
    {
    }
}

