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
package org.xwiki.wysiwyg.server.internal.plugin.importer;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.xwiki.wysiwyg.server.internal.plugin.importer.OfficeMacroImporter}.
 *
 * @version $Id$
 * @since 6.2M1
 */
public class OfficeMacroImporterTest
{
    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    @Test
    public void buildXDOM() throws Exception
    {
        componentManager.registerMockComponent(RenderingContext.class);
        componentManager.registerMockComponent(Transformation.class, "macro");
        componentManager.registerMockComponent(BlockRenderer.class, "annotatedxhtml/1.0");
        componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING);

        DocumentAccessBridge documentAccessBridge = componentManager.registerMockComponent(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        XWikiDocument xwikiDocument = mock(XWikiDocument.class);
        when(documentAccessBridge.getDocument(documentReference)).thenReturn(xwikiDocument);
        when(xwikiDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        OfficeMacroImporter importer = new OfficeMacroImporter(componentManager);
        XDOM xdom = importer.buildXDOM(new AttachmentReference("file", documentReference), true);

        assertNotNull(xdom);

        // Verify we have the proper METADATA set
        assertEquals("xwiki/2.1", ((Syntax) xdom.getMetaData().getMetaData(MetaData.SYNTAX)).toIdString());
    }
}
