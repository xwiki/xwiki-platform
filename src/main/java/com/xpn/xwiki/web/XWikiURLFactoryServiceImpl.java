/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Matthew Conway
 * Date: 9 Sep 2005
 */
package com.xpn.xwiki.web;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.pdf.impl.PdfURLFactory;
import com.xpn.xwiki.xmlrpc.XWikiXMLRPCURLFactory;

public class XWikiURLFactoryServiceImpl implements XWikiURLFactoryService
{
    private static final Log log = LogFactory.getLog(XWikiURLFactoryService.class);

    private Map              factoryMap;

    public XWikiURLFactoryServiceImpl()
    {
    }

    public void init(XWiki xwiki)
    {
        factoryMap = new HashMap();
        register(xwiki, XWikiContext.MODE_XMLRPC, XWikiXMLRPCURLFactory.class, "xwiki.urlfactory.xmlrpcclass");
        register(xwiki, XWikiContext.MODE_SERVLET, XWikiServletURLFactory.class, "xwiki.urlfactory.servletclass");
        register(xwiki, XWikiContext.MODE_PORTLET, XWikiPortletURLFactory.class, "xwiki.urlfactory.portletclass");
        register(xwiki, XWikiContext.MODE_PDF, PdfURLFactory.class, "xwiki.urlfactory.pdfclass");
    }

    protected void register(XWiki xwiki, int mode, Class defaultImpl, String propertyName)
    {
        Integer factoryMode = new Integer(mode);
        factoryMap.put(factoryMode, defaultImpl);
        String urlFactoryClassName = xwiki.Param(propertyName);
        if (urlFactoryClassName != null)
        {
            try
            {
                log.debug("Using custom url factory: " + urlFactoryClassName);
                Class urlFactoryClass = Class.forName(urlFactoryClassName);
                factoryMap.put(factoryMode, urlFactoryClass);
            }
            catch (Exception e)
            {
                log.error("Faiiled to load custom url factory class: " + urlFactoryClassName);
            }
        }
    }

    public XWikiURLFactory createURLFactory(int mode, XWikiContext context)
    {
        XWikiURLFactory urlf = null;
        try
        {
            Class urlFactoryClass = (Class) factoryMap.get(new Integer(mode));
            urlf = (XWikiURLFactory) urlFactoryClass.newInstance();
            urlf.init(context);
        }
        catch (Exception e)
        {
            log.error("Failed to get construct url factory", e);
        }
        return urlf;
    }
}
