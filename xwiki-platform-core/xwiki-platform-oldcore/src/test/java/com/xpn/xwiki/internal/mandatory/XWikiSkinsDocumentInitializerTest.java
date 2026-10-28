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

import java.util.List;

import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.objects.classes.TextAreaClass.ContentType;
import com.xpn.xwiki.objects.classes.TextAreaClass.EditorType;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiSkinsDocumentInitializer}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class XWikiSkinsDocumentInitializerTest
{
    @InjectMockComponents
    private XWikiSkinsDocumentInitializer initializer;

    @MockComponent
    @Named("class")
    private SheetBinder classSheetBinder;

    @MockComponent
    @Named("document")
    private SheetBinder documentSheetBinder;

    @Test
    void updateDocumentSetsPureTextContentTypeOnSkinFileProperties()
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("xwiki", "XWiki", "XWikiSkins"));
        when(this.classSheetBinder.getSheets(document))
            .thenReturn(List.of(new DocumentReference("xwiki", "SkinsCode", "XWikiSkinsSheet")));

        BaseClass xclass = document.getXClass();
        // A skin file property added to the class through the class editor: no content type, i.e. wiki content.
        xclass.addTextAreaField("htmlheader.vm", "HTML Header", 80, 15, EditorType.WYSIWYG.toString(), (String) null);
        // A property with an explicitly chosen content type.
        xclass.addTextAreaField("notes", "Notes", 80, 15);
        ((TextAreaClass) xclass.get("notes")).setContentType(ContentType.WIKI_TEXT.toString());

        this.initializer.updateDocument(document);

        // Standard skin file properties.
        assertPureText(xclass, "style.css");
        assertPureText(xclass, "header.vm");
        assertPureText(xclass, "edit.vm");
        // The custom skin file property gets the pure text content type and a compatible editor.
        assertPureText(xclass, "htmlheader.vm");
        // The explicitly chosen content type is kept.
        TextAreaClass notes = (TextAreaClass) xclass.get("notes");
        assertEquals("fullyrenderedtext", notes.getContentType());
        assertTrue(notes.isWikiContent());

        // Nothing left to update.
        assertFalse(this.initializer.updateDocument(document));
    }

    private void assertPureText(BaseClass xclass, String propertyName)
    {
        TextAreaClass property = (TextAreaClass) xclass.get(propertyName);
        assertEquals("puretext", property.getContentType());
        assertEquals("puretext", property.getEditor());
        assertFalse(property.isWikiContent());
    }
}
