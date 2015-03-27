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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.test.MockitoOldcoreRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link BooleanClass} class.
 * 
 * @version $Id$
 */
public class BooleanClassTest
{
    @Rule
    public MockitoOldcoreRule mocker = new MockitoOldcoreRule();

    /** Test localization. */
    @Test
    public void testLocalization() throws Exception
    {
        // Setup
        ContextualLocalizationManager contextualLocalizationManager =
            this.mocker.getMocker().registerMockComponent(ContextualLocalizationManager.class);
        when(contextualLocalizationManager.getTranslationPlain("Some.Class_prop_0")).thenReturn("Nay");
        when(contextualLocalizationManager.getTranslationPlain("Some.Class_prop_1")).thenReturn("Aye");
        when(contextualLocalizationManager.getTranslationPlain("Some.Class_prop_2")).thenReturn("Dunno");
        when(contextualLocalizationManager.getTranslationPlain("yesno_0")).thenReturn("No");
        when(contextualLocalizationManager.getTranslationPlain("yesno_1")).thenReturn("Yes");
        when(contextualLocalizationManager.getTranslationPlain("truefalse_0")).thenReturn("False");
        when(contextualLocalizationManager.getTranslationPlain("truefalse_1")).thenReturn("True");
        when(contextualLocalizationManager.getTranslationPlain("active_0")).thenReturn("Inactive");
        when(contextualLocalizationManager.getTranslationPlain("active_1")).thenReturn("Active");
        when(contextualLocalizationManager.getTranslationPlain("allow_0")).thenReturn("Deny");
        when(contextualLocalizationManager.getTranslationPlain("allow_1")).thenReturn("Allow");
        

        DocumentReference classReference =
            new DocumentReference(this.mocker.getXWikiContext().getWikiId(), "Some", "Class");

        EntityReferenceSerializer<String> entityReferenceSerializer =
            this.mocker.getMocker().registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        when(entityReferenceSerializer.serialize(classReference)).thenReturn("Some.Class");

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
        metaProperty.displayView(out, "prop", "", obj, this.mocker.getXWikiContext());
        assertEquals("No", out.toString());

        out.setLength(0);
        prop.setValue(1);
        metaProperty.displayView(out, "prop", "", obj, this.mocker.getXWikiContext());
        assertEquals("Yes", out.toString());

        out.setLength(0);
        prop.setValue(2);
        metaProperty.displayView(out, "prop", "", obj, this.mocker.getXWikiContext());
        assertEquals("---", out.toString());

        // Test translations when display type is "active"
        metaProperty.setDisplayType("active");
        out.setLength(0);
        prop.setValue(0);
        metaProperty.displayView(out, "prop", "", obj, this.mocker.getXWikiContext());
        assertEquals("Inactive", out.toString());

        out.setLength(0);
        prop.setValue(1);
        metaProperty.displayView(out, "prop", "", obj, this.mocker.getXWikiContext());
        assertEquals("Active", out.toString());

        out.setLength(0);
        prop.setValue(2);
        metaProperty.displayView(out, "prop", "", obj, this.mocker.getXWikiContext());
        assertEquals("---", out.toString());

        // Test translations when display type is the non-existing "blacktive"
        metaProperty.setDisplayType("blacktive");
        out.setLength(0);
        prop.setValue(0);
        metaProperty.displayView(out, "prop", "", obj, this.mocker.getXWikiContext());
        assertEquals("0", out.toString());

        out.setLength(0);
        prop.setValue(1);
        metaProperty.displayView(out, "prop", "", obj, this.mocker.getXWikiContext());
        assertEquals("1", out.toString());

        out.setLength(0);
        prop.setValue(2);
        metaProperty.displayView(out, "prop", "", obj, this.mocker.getXWikiContext());
        assertEquals("---", out.toString());

        // Test translations with the full classname_prop_value format
        metaProperty.setName("prop");
        out.setLength(0);
        prop.setValue(0);
        metaProperty.displayView(out, "prop", "", obj, this.mocker.getXWikiContext());
        assertEquals("Nay", out.toString());

        out.setLength(0);
        prop.setValue(1);
        metaProperty.displayView(out, "prop", "", obj, this.mocker.getXWikiContext());
        assertEquals("Aye", out.toString());

        out.setLength(0);
        prop.setValue(2);
        metaProperty.displayView(out, "prop", "", obj, this.mocker.getXWikiContext());
        assertEquals("Dunno", out.toString());
    }
}
