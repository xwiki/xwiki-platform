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
package org.xwiki.annotation.io.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.annotation.io.IOTargetService;
import org.xwiki.annotation.reference.internal.DefaultTypedStringEntityReferenceResolver;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_0;

/**
 * Tests the default implementation of {@link IOTargetService}, and integration with target resolvers, up to the
 * document access bridge access.
 *
 * @version $Id$
 * @since 2.3M1
 */
@ComponentTest
@ComponentList({ DefaultTypedStringEntityReferenceResolver.class })
@ReferenceComponentList
class DefaultIOTargetServiceTest
{
    @InjectMockComponents
    private DefaultIOTargetService ioTargetService;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private DocumentModelBridge documentModelBridge;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Test
    void gettersWhenTargetIsTypedDocument() throws Exception
    {
        String reference = "DOCUMENT://wiki:Space.Page";

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.documentAccessBridge.getTranslatedDocumentInstance(documentReference))
            .thenReturn(this.documentModelBridge);
        when(this.documentModelBridge.getContent()).thenReturn("defcontent");
        when(this.documentModelBridge.getSyntax()).thenReturn(XWIKI_2_0);

        assertEquals("defcontent", this.ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", this.ioTargetService.getSourceSyntax(reference));
    }

    @Test
    void gettersWhenTargetIsNonTypedDocument() throws Exception
    {
        String reference = "wiki:Space.Page";

        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.documentAccessBridge.getTranslatedDocumentInstance(documentReference))
            .thenReturn(this.documentModelBridge);
        when(this.documentModelBridge.getContent()).thenReturn("defcontent");
        when(this.documentModelBridge.getSyntax()).thenReturn(XWIKI_2_0);

        assertEquals("defcontent", this.ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", this.ioTargetService.getSourceSyntax(reference));
    }

    @Test
    void gettersWhenTargetIsNonTypedRelativeDocument() throws Exception
    {
        String reference = "Space.Page";

        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Page");
        when(this.documentAccessBridge.getTranslatedDocumentInstance(documentReference))
            .thenReturn(this.documentModelBridge);
        when(this.documentModelBridge.getContent()).thenReturn("defcontent");
        when(this.documentModelBridge.getSyntax()).thenReturn(XWIKI_2_0);

        assertEquals("defcontent", this.ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", this.ioTargetService.getSourceSyntax(reference));
    }

    @Test
    void gettersWhenTargetIsTypedRelativeDocument() throws Exception
    {
        String reference = "DOCUMENT://Space.Page";

        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Page");
        when(this.documentAccessBridge.getTranslatedDocumentInstance(documentReference))
            .thenReturn(this.documentModelBridge);
        when(this.documentModelBridge.getContent()).thenReturn("defcontent");
        when(this.documentModelBridge.getSyntax()).thenReturn(XWIKI_2_0);

        assertEquals("defcontent", this.ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", this.ioTargetService.getSourceSyntax(reference));
    }

    @Test
    void gettersWhenTargetIsTypedSpace() throws Exception
    {
        // expect source ref to be used as is, as it doesn't parse to something acceptable
        String reference = "SPACE://wiki:Space";

        when(this.documentAccessBridge.getDocumentContent(reference)).thenReturn("defcontent");
        when(this.documentAccessBridge.getDocumentSyntaxId(reference)).thenReturn("xwiki/2.0");
        when(this.documentModelBridge.getContent()).thenReturn("defcontent");
        when(this.documentModelBridge.getSyntax()).thenReturn(XWIKI_2_0);

        assertEquals("defcontent", this.ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", this.ioTargetService.getSourceSyntax(reference));
    }

    @Test
    void gettersWhenTargetIsEmptyString() throws Exception
    {
        // expect source ref to be used as is, as it doesn't parse to something acceptable
        String reference = "";

        DocumentReference documentReference = new DocumentReference("xwiki", "Main", "WebHome");
        when(this.documentAccessBridge.getTranslatedDocumentInstance(documentReference))
            .thenReturn(this.documentModelBridge);
        when(this.documentModelBridge.getContent()).thenReturn("defcontent");
        when(this.documentModelBridge.getSyntax()).thenReturn(XWIKI_2_0);
        assertEquals("defcontent", this.ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", this.ioTargetService.getSourceSyntax(reference));
    }

    @Test
    void getterWhenTargetIsTypedIndexedObjectProperty() throws Exception
    {
        Utils.setComponentManager(this.componentManager);
        when(this.documentAccessBridge.getProperty(
            new DocumentReference("wiki", "Space", "Page"),
            new DocumentReference("wiki", "XWiki", "Class"), 1, "property"))
            .thenReturn("defcontent");
        when(this.documentAccessBridge.getTranslatedDocumentInstance(new DocumentReference("wiki", "Space", "Page")))
            .thenReturn(this.documentModelBridge);
        when(this.documentModelBridge.getSyntax()).thenReturn(XWIKI_2_0);

        String reference = "OBJECT_PROPERTY://wiki:Space.Page^XWiki.Class[1].property";
        assertEquals("defcontent", this.ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", this.ioTargetService.getSourceSyntax(reference));
    }

    @Test
    void getterWhenTargetIsTypedDefaultObjectProperty() throws Exception
    {
        Utils.setComponentManager(this.componentManager);
        when(this.documentAccessBridge.getProperty(
            new DocumentReference("wiki", "Space", "Page"),
            new DocumentReference("wiki", "XWiki", "Class"), "property"))
            .thenReturn("defcontent");
        when(this.documentAccessBridge.getTranslatedDocumentInstance(new DocumentReference("wiki", "Space", "Page")))
            .thenReturn(this.documentModelBridge);
        when(this.documentModelBridge.getSyntax()).thenReturn(XWIKI_2_0);

        String reference = "OBJECT_PROPERTY://wiki:Space.Page^XWiki.Class.property";
        assertEquals("defcontent", this.ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", this.ioTargetService.getSourceSyntax(reference));
    }

    @Test
    void getterWhenTargetIsTypedObjectPropertyInRelativeDocument() throws Exception
    {
        Utils.setComponentManager(this.componentManager);
        when(this.documentAccessBridge.getProperty(
            new DocumentReference("xwiki", "Main", "Page"),
            new DocumentReference("xwiki", "XWiki", "Class"), "property"))
            .thenReturn("defcontent");
        when(this.documentAccessBridge.getTranslatedDocumentInstance(new DocumentReference("xwiki", "Main", "Page")))
            .thenReturn(this.documentModelBridge);
        when(this.documentModelBridge.getSyntax()).thenReturn(XWIKI_2_0);

        String reference = "OBJECT_PROPERTY://Page^XWiki.Class.property";
        assertEquals("defcontent", this.ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", this.ioTargetService.getSourceSyntax(reference));
    }

    @Test
    void getterWhenTargetIsNonTypedObjectProperty() throws Exception
    {
        Utils.setComponentManager(this.componentManager);
        when(this.documentAccessBridge.getTranslatedDocumentInstance(
            new DocumentReference("wiki", "Space.Page^XWiki.Class", "property")))
            .thenReturn(this.documentModelBridge);
        when(this.documentModelBridge.getContent()).thenReturn("defcontent");
        when(this.documentModelBridge.getSyntax()).thenReturn(XWIKI_2_0);

        String reference = "wiki:Space\\.Page^XWiki\\.Class.property";
        assertEquals("defcontent", this.ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", this.ioTargetService.getSourceSyntax(reference));
    }

    @Test
    void getterWhenTargetIsTypedIndexedRelativeObjectProperty() throws Exception
    {
        Utils.setComponentManager(this.componentManager);
        when(this.documentAccessBridge.getProperty(new DocumentReference("xwiki", "Main", "WebHome"),
            new DocumentReference("xwiki", "Classes", "Class"), 3, "property"))
            .thenReturn("defcontent");
        when(this.documentAccessBridge.getTranslatedDocumentInstance(new DocumentReference("xwiki", "Main", "WebHome")))
            .thenReturn(this.documentModelBridge);
        when(this.documentModelBridge.getSyntax()).thenReturn(XWIKI_2_0);

        String reference = "OBJECT_PROPERTY://Classes.Class[3].property";
        assertEquals("defcontent", this.ioTargetService.getSource(reference));
        assertEquals("xwiki/2.0", this.ioTargetService.getSourceSyntax(reference));
    }
}
