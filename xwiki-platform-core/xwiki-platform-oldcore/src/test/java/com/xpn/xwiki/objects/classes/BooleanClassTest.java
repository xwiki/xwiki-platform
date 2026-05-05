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
package com.xpn.xwiki.objects.classes;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link BooleanClass} class.
 * 
 * @version $Id$
 */
@OldcoreTest
class BooleanClassTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /** Test localization. */
    @Test
    void localization()
    {
        // Setup
        when(this.contextualLocalizationManager.getTranslationPlain("Some.Class_prop_0")).thenReturn("Nay");
        when(this.contextualLocalizationManager.getTranslationPlain("Some.Class_prop_1")).thenReturn("Aye");
        when(this.contextualLocalizationManager.getTranslationPlain("Some.Class_prop_2")).thenReturn("Dunno");
        when(this.contextualLocalizationManager.getTranslationPlain("yesno_0")).thenReturn("No");
        when(this.contextualLocalizationManager.getTranslationPlain("yesno_1")).thenReturn("Yes");
        when(this.contextualLocalizationManager.getTranslationPlain("truefalse_0")).thenReturn("False");
        when(this.contextualLocalizationManager.getTranslationPlain("truefalse_1")).thenReturn("True");
        when(this.contextualLocalizationManager.getTranslationPlain("active_0")).thenReturn("Inactive");
        when(this.contextualLocalizationManager.getTranslationPlain("active_1")).thenReturn("Active");
        when(this.contextualLocalizationManager.getTranslationPlain("allow_0")).thenReturn("Deny");
        when(this.contextualLocalizationManager.getTranslationPlain("allow_1")).thenReturn("Allow");
        

        DocumentReference classReference =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Some", "Class");

        when(this.entityReferenceSerializer.serialize(classReference)).thenReturn("Some.Class");

        // Create a Boolean meta-property and an associated object with a property instance
        BooleanClass metaProperty = new BooleanClass();
        BaseClass cls = new BaseClass();
        BaseObject obj = new BaseObject();
        IntegerProperty prop = new IntegerProperty();
        prop.setValue(0);
        obj.safeput("prop", prop);
        cls.setDocumentReference(classReference);
        metaProperty.setObject(cls);

        StringBuffer out = new StringBuffer();

        // Test the default translations, should be the default "yesno" display type
        metaProperty.displayView(out, "prop", "", obj, this.oldcore.getXWikiContext());
        assertEquals("No", out.toString());

        out.setLength(0);
        prop.setValue(1);
        metaProperty.displayView(out, "prop", "", obj, this.oldcore.getXWikiContext());
        assertEquals("Yes", out.toString());

        out.setLength(0);
        prop.setValue(2);
        metaProperty.displayView(out, "prop", "", obj, this.oldcore.getXWikiContext());
        assertEquals("---", out.toString());

        // Test translations when display type is "active"
        metaProperty.setDisplayType("active");
        out.setLength(0);
        prop.setValue(0);
        metaProperty.displayView(out, "prop", "", obj, this.oldcore.getXWikiContext());
        assertEquals("Inactive", out.toString());

        out.setLength(0);
        prop.setValue(1);
        metaProperty.displayView(out, "prop", "", obj, this.oldcore.getXWikiContext());
        assertEquals("Active", out.toString());

        out.setLength(0);
        prop.setValue(2);
        metaProperty.displayView(out, "prop", "", obj, this.oldcore.getXWikiContext());
        assertEquals("---", out.toString());

        // Test translations when display type is the non-existing "blacktive"
        metaProperty.setDisplayType("blacktive");
        out.setLength(0);
        prop.setValue(0);
        metaProperty.displayView(out, "prop", "", obj, this.oldcore.getXWikiContext());
        assertEquals("0", out.toString());

        out.setLength(0);
        prop.setValue(1);
        metaProperty.displayView(out, "prop", "", obj, this.oldcore.getXWikiContext());
        assertEquals("1", out.toString());

        out.setLength(0);
        prop.setValue(2);
        metaProperty.displayView(out, "prop", "", obj, this.oldcore.getXWikiContext());
        assertEquals("---", out.toString());

        // Test translations with the full classname_prop_value format
        metaProperty.setName("prop");
        out.setLength(0);
        prop.setValue(0);
        metaProperty.displayView(out, "prop", "", obj, this.oldcore.getXWikiContext());
        assertEquals("Nay", out.toString());

        out.setLength(0);
        prop.setValue(1);
        metaProperty.displayView(out, "prop", "", obj, this.oldcore.getXWikiContext());
        assertEquals("Aye", out.toString());

        out.setLength(0);
        prop.setValue(2);
        metaProperty.displayView(out, "prop", "", obj, this.oldcore.getXWikiContext());
        assertEquals("Dunno", out.toString());
    }
}
