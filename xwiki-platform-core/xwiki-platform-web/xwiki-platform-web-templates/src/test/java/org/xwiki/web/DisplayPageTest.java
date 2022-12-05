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
package org.xwiki.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin;
import com.xpn.xwiki.plugin.skinx.JsSkinFileExtensionPlugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Tests the {@code display.vm} template.
 *
 * @since 14.0RC1
 * @version $Id$
 */
@SecurityScriptServiceComponentList
@ComponentList({
    ModelScriptService.class,
    TemplateScriptService.class
})
class DisplayPageTest extends PageTest
{
    private static final String DISPLAY_VM = "display.vm";

    private static final String FIELD_NAME = "testField";

    private static final String FIELD_PRETTY_NAME = "Test Field";

    private static final String DEFAULT_LABEL = "space.page_testField_";

    private static final String VALUE_1 = "value1";

    private static final String DEFAULT_SELECT =
        "<select id='space.page_0_testField' name='space.page_0_testField' size='1'>"
        + "<option selected='selected' value='' label='space.page_testField_'>space.page_testField_</option>"
        + "<option value='value1' label='space.page_testField_value1'>space.page_testField_value1</option></select>"
        + "<input name='space.page_0_testField' type='hidden' value=''/>";

    private TemplateManager templateManager;

    private String serializedDocumentReference;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.componentManager.getInstance(TemplateManager.class);
        // Enable the ssfx/jsfx plugins
        this.oldcore.getSpyXWiki().getPluginManager()
            .addPlugin("ssfx", CssSkinFileExtensionPlugin.class.getName(), this.oldcore.getXWikiContext());
        this.oldcore.getSpyXWiki().getPluginManager()
            .addPlugin("jsfx", JsSkinFileExtensionPlugin.class.getName(), this.oldcore.getXWikiContext());

        // Set up the current doc in the context so that $doc is bound in scripts
        DocumentReference documentReference = new DocumentReference("xwiki", "space", "page");
        XWikiDocument document = new XWikiDocument(documentReference);
        document.setSyntax(Syntax.XWIKI_2_1);
        BaseClass xClass = document.getXClass();
        xClass.addStaticListField(FIELD_NAME, FIELD_PRETTY_NAME, String.join("|", "=" + DEFAULT_LABEL, VALUE_1));
        this.xwiki.saveDocument(document, this.context);
        this.context.setDoc(document);

        EntityReferenceSerializer<String> localReferenceSerializer =
            this.componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");

        this.serializedDocumentReference = localReferenceSerializer.serialize(document.getDocumentReference());

        request.put("xpage", "display");
        request.put("type", "object");
    }

    @Test
    void displayNonExistingProperty() throws Exception
    {
        request.put("property", this.serializedDocumentReference + "." + FIELD_NAME);
        String result = this.templateManager.render(DISPLAY_VM);
        assertEquals("", result.trim());
    }

    @Test
    void displayNonExistingPropertyWithUpdateOrCreate() throws Exception
    {
        request.put("property", this.serializedDocumentReference + "." + FIELD_NAME);
        request.put("objectPolicy", "updateOrCreate");

        String result = this.templateManager.render(DISPLAY_VM);
        assertEquals(DEFAULT_LABEL, result.trim());
    }

    @Test
    void displayNonExistingPropertyWithUpdateOrCreateEdit() throws Exception
    {
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.EDIT)).thenReturn(true);

        request.put("property", this.serializedDocumentReference + "." + FIELD_NAME);
        request.put("objectPolicy", "updateOrCreate");
        request.put("mode", "edit");

        String result = this.templateManager.render(DISPLAY_VM);
        assertEquals(DEFAULT_SELECT, result.trim());
    }

    @Test
    void displayExistingPropertyEditing() throws Exception
    {
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.EDIT)).thenReturn(true);

        XWikiDocument document = this.context.getDoc();
        BaseObject xObject = document.newXObject(document.getDocumentReference(), this.context);
        BaseClass xclass = xObject.getXClass(this.context);
        xObject.safeput(FIELD_NAME, ((PropertyClass) xclass.getField(FIELD_NAME)).newProperty());
        this.xwiki.saveDocument(document, this.context);

        request.put("property", this.serializedDocumentReference + "." + FIELD_NAME);
        request.put("mode", "edit");

        String result = this.templateManager.render(DISPLAY_VM);
        assertEquals(DEFAULT_SELECT, result.trim());
    }

    @Test
    void displayNonExistingPropertyWithNumberEditing() throws Exception
    {
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.EDIT)).thenReturn(true);

        request.put("property", this.serializedDocumentReference + "[3]." + FIELD_NAME);
        request.put("objectPolicy", "updateOrCreate");
        request.put("mode", "edit");

        String result = this.templateManager.render(DISPLAY_VM);
        assertEquals(DEFAULT_SELECT.replaceAll("_0_", "_3_"), result.trim());
    }
}
