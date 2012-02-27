/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */
package com.xpn.xwiki.web;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.pdf.impl.PdfURLFactory;
import com.xpn.xwiki.xmlrpc.XWikiXmlRpcURLFactory;

public class XWikiURLFactoryServiceImpl implements XWikiURLFactoryService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiURLFactoryService.class);

    private Map factoryMap;

    public XWikiURLFactoryServiceImpl(XWiki xwiki)
    {
        init(xwiki);
    }

    private void init(XWiki xwiki)
    {
        factoryMap = new HashMap();
        register(xwiki, XWikiContext.MODE_XMLRPC, XWikiXmlRpcURLFactory.class, "xwiki.urlfactory.xmlrpcclass");
        register(xwiki, XWikiContext.MODE_SERVLET, XWikiServletURLFactory.class, "xwiki.urlfactory.servletclass");
        register(xwiki, XWikiContext.MODE_PORTLET, XWikiPortletURLFactory.class, "xwiki.urlfactory.portletclass");
        register(xwiki, XWikiContext.MODE_PDF, PdfURLFactory.class, "xwiki.urlfactory.pdfclass");
        register(xwiki, XWikiContext.MODE_GWT, XWikiServletURLFactory.class, "xwiki.urlfactory.servletclass");
        register(xwiki, XWikiContext.MODE_GWT_DEBUG, XWikiDebugGWTURLFactory.class, "xwiki.urlfactory.servletclass");
    }

    protected void register(XWiki xwiki, int mode, Class defaultImpl, String propertyName)
    {
        Integer factoryMode = new Integer(mode);
        factoryMap.put(factoryMode, defaultImpl);
        String urlFactoryClassName = xwiki.Param(propertyName);
        if (urlFactoryClassName != null) {
            try {
                LOGGER.debug("Using custom url factory [" + urlFactoryClassName + "]");
                Class urlFactoryClass = Class.forName(urlFactoryClassName);
                factoryMap.put(factoryMode, urlFactoryClass);
            } catch (Exception e) {
                LOGGER.error("Failed to load custom url factory class [" + urlFactoryClassName + "]");
            }
        }
    }

    @Override
    public XWikiURLFactory createURLFactory(int mode, XWikiContext context)
    {
        XWikiURLFactory urlf = null;
        try {
            Class urlFactoryClass = (Class) factoryMap.get(new Integer(mode));
            urlf = (XWikiURLFactory) urlFactoryClass.newInstance();
            urlf.init(context);
        } catch (Exception e) {
            LOGGER.error("Failed to create url factory", e);
        }
        return urlf;
    }
}
