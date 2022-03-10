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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.skinx.SkinExtensionPluginApi;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Set up Skin Extension plugins for Page Tests.
 *
 * @version $Id$
 * @since 8.3M2
 */
public final class SkinExtensionSetup
{
    private SkinExtensionSetup()
    {
        // Utility class and thus no public constructor.
    }

    /**
     * Sets up the SSX/JSX plugins to provide a noop implementation (i.e. do nothing).
     *
     * @param xwiki the stubbed XWiki instance
     * @param context the stubbed XWikiContext instance
     * @throws Exception when a setup error occurs
     */
    public static void setUp(XWiki xwiki, XWikiContext context) throws Exception
    {
        SkinExtensionPluginApi voidPluginApi = mock(SkinExtensionPluginApi.class);
        doReturn(voidPluginApi).when(xwiki).getPluginApi("jsx", context);
        doReturn(voidPluginApi).when(xwiki).getPluginApi("ssx", context);
    }
}
