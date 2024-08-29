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
package org.xwiki.skinx.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.skinx.SkinExtensionPluginApi;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link JsResourceSkinExtension}.
 *
 * @version $Id$
 * @since 13.10RC1
 */
@ComponentTest
class JsResourceSkinExtensionTest
{
    private static final String NAME = "jsrx";

    @InjectMockComponents
    private JsResourceSkinExtension skinExtension;

    @MockComponent
    private Execution execution;

    private SkinExtensionPluginApi skinExtensionPluginApi;

    @BeforeEach
    void setup()
    {
        this.skinExtensionPluginApi = mock(SkinExtensionPluginApi.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        XWikiContext xWikiContext = mock(XWikiContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xWikiContext);
        XWiki xWiki = mock(XWiki.class);
        when(xWikiContext.getWiki()).thenReturn(xWiki);
        when(xWiki.getPluginApi(NAME, xWikiContext)).thenReturn(this.skinExtensionPluginApi);
    }

    @Test
    void use()
    {
        this.skinExtension.use("foo");
        verify(this.skinExtensionPluginApi).use("foo");
    }
}
