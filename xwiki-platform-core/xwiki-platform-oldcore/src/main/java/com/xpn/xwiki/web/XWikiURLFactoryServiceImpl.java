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

public class XWikiURLFactoryServiceImpl implements XWikiURLFactoryService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiURLFactoryService.class);

    private Map<Integer, Class<? extends XWikiURLFactory>> factoryMap;

    public XWikiURLFactoryServiceImpl(XWiki xwiki)
    {
        init(xwiki);
    }

    private void init(XWiki xwiki)
    {
        this.factoryMap = new HashMap<>();
        // TODO: This mode is still used by the REST module (in web.xml). We need to get rid of it.
        register(xwiki, XWikiContext.MODE_XMLRPC, ExternalServletURLFactory.class, "xwiki.urlfactory.xmlrpcclass");
        register(xwiki, XWikiContext.MODE_SERVLET, XWikiServletURLFactory.class, "xwiki.urlfactory.servletclass");
        // TODO: This mode is not used anymore by the server-side PDF export. We need to move it to legacy or decide to
        //  remove it altogether.
        register(xwiki, XWikiContext.MODE_PDF, PdfURLFactory.class, "xwiki.urlfactory.pdfclass");
    }

    protected void register(XWiki xwiki, int mode, Class<? extends XWikiURLFactory> defaultImpl, String propertyName)
    {
        this.factoryMap.put(mode, defaultImpl);
        String urlFactoryClassName = xwiki.Param(propertyName);
        if (urlFactoryClassName != null) {
            try {
                LOGGER.debug("Using custom url factory [" + urlFactoryClassName + "]");
                @SuppressWarnings("unchecked")
                Class<? extends XWikiURLFactory> urlFactoryClass =
                    (Class<? extends XWikiURLFactory>) Class.forName(urlFactoryClassName);
                this.factoryMap.put(mode, urlFactoryClass);
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
            Class<? extends XWikiURLFactory> urlFactoryClass = this.factoryMap.get(mode);
            urlf = urlFactoryClass.newInstance();
            urlf.init(context);
        } catch (Exception e) {
            LOGGER.error("Failed to create url factory", e);
        }
        return urlf;
    }
}
