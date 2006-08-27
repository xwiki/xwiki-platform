/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * @author ludovic
 * @author sdumitriu
 */

package com.xpn.xwiki.xmlrpc;

import java.io.UnsupportedEncodingException;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiURLFactory;

public class BaseRpcHandler implements RequestInitializableHandler {

    private XWikiEngineContext econtext;
    private XWikiRequest request;
    private XWikiResponse response;	
        
	public void init(Servlet servlet, ServletRequest request)
			throws XWikiException {
        this.request = new XWikiXMLRPCRequest((HttpServletRequest)request); // use the real request
        this.response = new XWikiXMLRPCResponse(new MockXWikiResponse()); // use fake response
        
        ServletContext sContext = null;
        try {
        	sContext = servlet.getServletConfig().getServletContext();
        } catch (Exception ignore) { }
        if (sContext != null) {
        	this.econtext = new XWikiXMLRPCContext(sContext);
        } else {
        	this.econtext = new XWikiXMLRPCContext(new MockXWikiServletContext());
        }
        
//        java.lang.NullPointerException
//    	at com.xpn.xwiki.XWiki.getXWiki(XWiki.java:363)
//    	at com.xpn.xwiki.xmlrpc.BaseRpcHandler.getXWikiContext(BaseRpcHandler.java:67)
//    	at com.xpn.xwiki.xmlrpc.ConfluenceRpcHandler.login(ConfluenceRpcHandler.java:59)
//    	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
//    	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
//    	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)

    }

    public XWikiContext getXWikiContext() throws XWikiException {
        XWikiContext context = Utils.prepareContext("", request, response, econtext);
        context.setDatabase("xwikitest");
        XWiki xwiki = XWiki.getXWiki(context);
        XWikiURLFactory urlf = xwiki.getURLFactoryService().createURLFactory(context.getMode(), context);
        context.setURLFactory(urlf);
        XWikiVelocityRenderer.prepareContext(context);
        return context;
    }


    protected byte[] convertToBase64( String content )
    {
        try
        {
            return content.getBytes("UTF-8");
        }
        catch( UnsupportedEncodingException e )
        {
            return content.getBytes();
        }
    }
}

