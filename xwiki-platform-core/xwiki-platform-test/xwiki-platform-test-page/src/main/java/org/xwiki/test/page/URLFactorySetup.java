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
package org.xwiki.test.page;

import java.net.URL;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiServletURLFactory;

import static org.mockito.Mockito.doReturn;

/**
 * Set up URL Factory for Page Tests.
 *
 * @version $Id$
 * @since 8.3M2
 */
public final class URLFactorySetup
{
    private static final String XWIKI = "xwiki";

    private URLFactorySetup()
    {
        // Utility class and thus no public constructor.
    }

    /**
     * Sets up a fake base URL and a fake Servlet URL Factory.
     *
     * @param xwiki the stubbed XWiki instance
     * @param context the stubbed XWikiContext instance
     * @throws Exception when a setup error occurs
     */
    public static void setUp(XWiki xwiki, XWikiContext context) throws Exception
    {
        doReturn(XWIKI).when(xwiki).getWebAppPath(context);
        context.setURL(new URL("http://localhost:8080/xwiki/bin/Main/WebHome"));
        doReturn(true).when(xwiki).showViewAction(context);
        context.setURLFactory(new XWikiServletURLFactory(context));
        doReturn("/bin/").when(xwiki).getServletPath(XWIKI, context);
    }
}
