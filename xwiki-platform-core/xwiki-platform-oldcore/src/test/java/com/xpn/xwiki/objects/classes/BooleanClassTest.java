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

    /**
     * Builds a Boolean property whose display values contain characters that need to be escaped in the generated
     * HTML.
     *
     * @param object the object receiving the property instance, set to the "true" value
     * @return the Boolean meta-property to display
     */
    private BooleanClass setUpDisplayValuesNeedingEscaping(BaseObject object)
    {
        when(this.contextualLocalizationManager.getTranslationPlain("yesno_0")).thenReturn("No <b>& {{macro}}");
        when(this.contextualLocalizationManager.getTranslationPlain("yesno_1")).thenReturn("Yes '\"<em>");
        when(this.contextualLocalizationManager.getTranslationPlain("yesno_2")).thenReturn("None <img/>");

        DocumentReference classReference =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Some", "Class");
        when(this.entityReferenceSerializer.serialize(classReference)).thenReturn("Some.Class");

        BooleanClass metaProperty = new BooleanClass();
        BaseClass cls = new BaseClass();
        cls.setDocumentReference(classReference);
        metaProperty.setObject(cls);

        IntegerProperty prop = new IntegerProperty();
        prop.setValue(1);
        object.safeput("prop", prop);

        return metaProperty;
    }

    @Test
    void displayViewEscapesDisplayValue()
    {
        BaseObject object = new BaseObject();
        BooleanClass metaProperty = setUpDisplayValuesNeedingEscaping(object);

        StringBuffer buffer = new StringBuffer();
        metaProperty.displayView(buffer, "prop", "", object, this.oldcore.getXWikiContext());

        assertEquals("Yes '\"&#60;em>", buffer.toString());
    }

    @Test
    void displayRadioEditEscapesValues()
    {
        BaseObject object = new BaseObject();
        BooleanClass metaProperty = setUpDisplayValuesNeedingEscaping(object);

        // The prefix is built by XWikiDocument#display from the reference of the XClass document. Unlike the property
        // name, which is restricted to valid XML element names, the name of that document isn't restricted.
        StringBuffer buffer = new StringBuffer();
        metaProperty.displayRadioEdit(buffer, "prop", "Some.My{{macro}}Class_0_", object,
            this.oldcore.getXWikiContext());

        String escapedPrefix = "Some.My&#123;&#123;macro}}Class_0_prop";
        assertEquals("<div><label for='" + escapedPrefix + "_none'>"
            + "<input id='" + escapedPrefix + "_none' value='' name='" + escapedPrefix + "' type='radio'/>"
            + "None &#60;img/&#62;</label></div>"
            + "<div><label for='" + escapedPrefix + "'>"
            + "<input id='" + escapedPrefix + "' checked='checked' value='1' name='" + escapedPrefix
            + "' type='radio'/>"
            + "Yes &#39;&#34;&#60;em&#62;</label></div>"
            + "<div><label for='" + escapedPrefix + "_false'>"
            + "<input id='" + escapedPrefix + "_false' value='0' name='" + escapedPrefix + "' type='radio'/>"
            + "No &#60;b&#62;&#38; &#123;&#123;macro}}</label></div>", buffer.toString());
    }

    @Test
    void displaySelectEditEscapesValues()
    {
        BaseObject object = new BaseObject();
        BooleanClass metaProperty = setUpDisplayValuesNeedingEscaping(object);

        StringBuffer buffer = new StringBuffer();
        metaProperty.displaySelectEdit(buffer, "prop", "Some.My{{macro}}Class_0_", object,
            this.oldcore.getXWikiContext());

        String escapedPrefix = "Some.My&#123;&#123;macro}}Class_0_prop";
        assertEquals("<select id='" + escapedPrefix + "' name='" + escapedPrefix + "' size='1'>"
            + "<option value='' label='---'>---</option>"
            + "<option selected='selected' value='1' label='Yes &#39;&#34;&#60;em&#62;'>"
            + "Yes &#39;&#34;&#60;em&#62;</option>"
            + "<option value='0' label='No &#60;b&#62;&#38; &#123;&#123;macro}}'>"
            + "No &#60;b&#62;&#38; &#123;&#123;macro}}</option>"
            + "</select>", buffer.toString());
    }
}
