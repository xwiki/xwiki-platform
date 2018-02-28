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
package org.xwiki.wysiwyg.internal.importer;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OfficeMacroImporter}.
 *
 * @version $Id$
 * @since 9.8
 */
public class OfficeMacroImporterTest
{
    @Rule
    public MockitoComponentMockingRule<OfficeMacroImporter> mocker =
        new MockitoComponentMockingRule<>(OfficeMacroImporter.class);

    @Test
    public void buildXDOM() throws Exception
    {
        DocumentAccessBridge documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        XWikiDocument xwikiDocument = mock(XWikiDocument.class);
        when(documentAccessBridge.getTranslatedDocumentInstance(documentReference)).thenReturn(xwikiDocument);
        when(xwikiDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        XDOM xdom =
            this.mocker.getComponentUnderTest().buildXDOM(new AttachmentReference("file", documentReference), true);

        assertNotNull(xdom);

        // Verify we have the proper METADATA set
        assertEquals("xwiki/2.1", ((Syntax) xdom.getMetaData().getMetaData(MetaData.SYNTAX)).toIdString());
    }
}
