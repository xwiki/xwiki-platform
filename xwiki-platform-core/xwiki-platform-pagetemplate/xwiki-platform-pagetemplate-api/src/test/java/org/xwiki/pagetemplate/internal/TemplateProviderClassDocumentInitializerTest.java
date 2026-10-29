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
package org.xwiki.pagetemplate.internal;

import java.util.List;

import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.PageClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemplateProviderClassDocumentInitializer}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList({
    DefaultConverterManager.class,
    ContextComponentManagerProvider.class,
    EnumConverter.class,
    ConvertUtilsConverter.class
})
class TemplateProviderClassDocumentInitializerTest
{
    private static final DocumentReference CLASS_REFERENCE =
        new DocumentReference("wiki", "XWiki", "TemplateProviderClass");

    private static final DocumentReference SHEET_REFERENCE =
        new DocumentReference("wiki", "XWiki", "TemplateProviderSheet");

    @InjectMockComponents
    private TemplateProviderClassDocumentInitializer initializer;

    @MockComponent
    @Named("class")
    private SheetBinder classSheetBinder;

    @Test
    void createClass()
    {
        BaseClass xclass = new BaseClass();

        this.initializer.createClass(xclass);

        assertEquals(10, xclass.getFieldList().size());
        assertInstanceOf(StringClass.class, xclass.getField("name"));
        assertInstanceOf(StringClass.class, xclass.getField("description"));
        assertInstanceOf(StringClass.class, xclass.getField("icon"));
        assertEquals("select",
            assertInstanceOf(BooleanClass.class, xclass.getField("terminal")).getDisplayFormType());
        assertEquals("checkbox",
            assertInstanceOf(BooleanClass.class, xclass.getField("creationRestrictionsAreSuggestions"))
                .getDisplayFormType());

        PageClass template = assertInstanceOf(PageClass.class, xclass.getField("template"));
        assertFalse(template.isMultiSelect());
        assertTrue(template.isPicker());

        StaticListClass action = assertInstanceOf(StaticListClass.class, xclass.getField("action"));
        assertEquals("edit|saveandedit|saveandview", action.getValues());
        assertEquals("select", action.getDisplayType());
    }

    @Test
    void createClassSpacesFields()
    {
        BaseClass xclass = new BaseClass();

        this.initializer.createClass(xclass);

        DBListClass spaces = assertInstanceOf(DBListClass.class, xclass.getField("spaces"));
        assertTrue(spaces.isMultiSelect());
        assertFalse(spaces.isRelationalStorage());
        assertEquals(",", spaces.getSeparators());
        assertEquals(",", spaces.getSeparator());

        for (String restrictions : new String[] { "visibilityRestrictions", "creationRestrictions" }) {
            DBListClass restrictionsClass = assertInstanceOf(DBListClass.class, xclass.getField(restrictions));
            assertTrue(restrictionsClass.isMultiSelect());
            assertTrue(restrictionsClass.isRelationalStorage());
            assertTrue(restrictionsClass.isPicker());
            assertEquals("input", restrictionsClass.getDisplayType());
            assertEquals(",", restrictionsClass.getSeparators());
            assertEquals(" ,", restrictionsClass.getSeparator());
        }
    }

    @Test
    void updateDocumentBindsSheet()
    {
        XWikiDocument document = new XWikiDocument(CLASS_REFERENCE);
        when(this.classSheetBinder.getSheets(document)).thenReturn(List.of());
        when(this.classSheetBinder.bind(document, SHEET_REFERENCE)).thenReturn(true);

        assertTrue(this.initializer.updateDocument(document));
        assertEquals(10, document.getXClass().getFieldList().size());
        verify(this.classSheetBinder).bind(document, SHEET_REFERENCE);
    }

    @Test
    void updateDocumentKeepsExistingSheet()
    {
        XWikiDocument document = new XWikiDocument(CLASS_REFERENCE);
        when(this.classSheetBinder.getSheets(document)).thenReturn(List.of(SHEET_REFERENCE));

        this.initializer.updateDocument(document);

        verify(this.classSheetBinder, never()).bind(document, SHEET_REFERENCE);
    }
}
