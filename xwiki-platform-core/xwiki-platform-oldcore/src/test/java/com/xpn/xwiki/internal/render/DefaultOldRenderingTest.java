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
package com.xpn.xwiki.internal.render;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultOldRendering}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultOldRenderingTest
{
    @InjectMockComponents
    private DefaultOldRendering oldRendering;

    @Test
    void renderTextKeepsCallerSecureDocument()
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        XWikiDocument callerDocument = mock(XWikiDocument.class, "caller");
        when(xcontext.getSecureDocument()).thenReturn(callerDocument);

        XWikiDocument targetDocument = mock(XWikiDocument.class, "target");
        when(targetDocument.getSyntaxId()).thenReturn("xwiki/2.1");
        when(targetDocument.getRenderedContent("text", "xwiki/2.1", false, callerDocument, xcontext))
            .thenReturn("rendered with the caller rights");

        assertEquals("rendered with the caller rights", this.oldRendering.renderText("text", targetDocument, xcontext));
    }
}
