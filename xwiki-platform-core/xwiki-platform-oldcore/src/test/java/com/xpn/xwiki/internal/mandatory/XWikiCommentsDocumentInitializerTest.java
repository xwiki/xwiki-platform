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
package com.xpn.xwiki.internal.mandatory;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link XWikiCommentsDocumentInitializer}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class XWikiCommentsDocumentInitializerTest
{
    private static final String HIGHLIGHT = "highlight";

    @InjectMockComponents
    private XWikiCommentsDocumentInitializer initializer;

    /**
     * Converts the {@code null} editor that the initializer sets on the comment property.
     */
    @MockComponent
    private ConverterManager converterManager;

    @Test
    void updateDocumentSetsPureTextOnExistingHighlightProperty()
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("xwiki", "XWiki", "XWikiComments"));

        BaseClass xclass = document.getXClass();
        // An existing highlight property with the default text area settings: a non-restricted wiki syntax text area.
        xclass.addTextAreaField(HIGHLIGHT, "Highlighted Text", 40, 2);

        assertTrue(this.initializer.updateDocument(document));

        TextAreaClass highlight = (TextAreaClass) xclass.get(HIGHLIGHT);
        assertEquals("puretext", highlight.getContentType());
        assertEquals("puretext", highlight.getEditor());
        assertFalse(highlight.isWikiContent());
        assertTrue(highlight.isRestricted());

        // Nothing left to update.
        assertFalse(this.initializer.updateDocument(document));
    }
}
