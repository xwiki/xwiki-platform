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
package org.xwiki.export.pdf.internal;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.skinx.SkinExtensionPluginApi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultRequiredSkinExtensionsRecorder}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultRequiredSkinExtensionsRecorderTest
{
    @InjectMockComponents
    private DefaultRequiredSkinExtensionsRecorder recorder;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Test
    void startStop()
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(xcontext);

        XWiki xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        SkinExtensionPluginApi jsx = mock(SkinExtensionPluginApi.class, "jsx");
        when(xwiki.getPluginApi("jsx", xcontext)).thenReturn(jsx);
        when(jsx.getImportString()).thenReturn("before", "before after");

        this.recorder.start();

        assertEquals("after", this.recorder.stop());
    }
}
