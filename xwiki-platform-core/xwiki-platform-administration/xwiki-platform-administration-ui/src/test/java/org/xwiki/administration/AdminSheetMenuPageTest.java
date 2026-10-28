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
package org.xwiki.administration;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.administration.api.ConfigurableObjectEvaluator;
import org.xwiki.evaluation.internal.DefaultObjectEvaluator;
import org.xwiki.evaluation.internal.VelocityObjectPropertyEvaluator;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.rendering.internal.macro.message.WarningMessageMacro;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.user.UserReferenceComponentList;
import org.xwiki.user.internal.converter.DocumentUserReferenceConverter;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.DocumentReferenceConverter;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Page test of the administration menu emitters ({@code #admin_displayCategories} and
 * {@code #admin_displayCategory}) of {@code XWiki.AdminSheet}. It checks that the name and the icon reference of the
 * administration categories and sections, which can be controlled by any user through a
 * {@code XWiki.ConfigurableClass} object, are escaped before being inserted in the wiki syntax link label of the menu.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@SecurityScriptServiceComponentList
@UserReferenceComponentList
@ComponentList({
    TestNoScriptMacro.class,
    TranslationMacro.class,
    ErrorMessageMacro.class,
    WarningMessageMacro.class,
    DocumentUserReferenceConverter.class,
    DocumentReferenceConverter.class,
    DefaultObjectEvaluator.class,
    VelocityObjectPropertyEvaluator.class,
    ConfigurableObjectEvaluator.class
})
class AdminSheetMenuPageTest extends PageTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final String SPACE_NAME = "XWiki";

    private static final DocumentReference ADMIN_SHEET = new DocumentReference(WIKI_NAME, SPACE_NAME, "AdminSheet");

    private static final DocumentReference CONFIGURABLE_CLASS =
        new DocumentReference(WIKI_NAME, SPACE_NAME, "ConfigurableClass");

    private static final DocumentReference CONFIGURABLE_CLASS_MACROS =
        new DocumentReference(WIKI_NAME, SPACE_NAME, "ConfigurableClassMacros");

    private static final DocumentReference MY_SECTION = new DocumentReference(WIKI_NAME, SPACE_NAME, "MySection");

    private static final String NO_SCRIPT = "{{noscript /}}";

    /**
     * Attachment names cannot contain a slash, so the icon payload closes the image reference and injects bold wiki
     * syntax instead of a macro.
     */
    private static final String ICON_ATTACHMENT = "]]**INJECTED**.png";

    @MockComponent(classToMock = QueryManagerScriptService.class)
    @Named("query")
    private ScriptService queryService;

    @Mock
    private ScriptQuery query;

    @BeforeEach
    void setUp() throws Exception
    {
        // Load the macros page so it can be included by the administration sheet.
        loadPage(CONFIGURABLE_CLASS_MACROS);

        // Mock the query used to find the pages holding a XWiki.ConfigurableClass object.
        when(((QueryManagerScriptService) this.queryService).hql(anyString())).thenReturn(this.query);
        when(this.query.addFilter(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.bindValues(any(List.class))).thenReturn(this.query);

        // Every user can view and edit the configurable page in this test.
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("edit"), any(), any(), any())).thenReturn(true);

        // The administration sheet is normally applied to a document, which provides these variables. When rendered
        // directly they must be set so that the URLs of the menu entries (and thus the wiki links) are well-formed.
        registerVelocityTool("currentDoc", "XWiki.XWikiPreferences");
        registerVelocityTool("currentSpace", SPACE_NAME);
    }

    @Test
    void escapeCategoryNameInMenu() throws Exception
    {
        // The category name is controlled through the displayInCategory property.
        registerConfigurableSection(NO_SCRIPT, "mysection");

        // No category / section selected: the category grid (#admin_displayCategories) is rendered.
        this.request.put("viewer", "content");

        Document result = renderHTMLPage(ADMIN_SHEET);

        // The category name is rendered as the (bold) label of the menu entry link. It must be present verbatim, which
        // means it was escaped rather than executed as the noscript macro.
        Element categoryName = result.selectFirst("ul.admin-category strong");
        assertNotNull(categoryName, "No administration category was rendered. Content: " + result.text());
        assertTrue(categoryName.text().contains(NO_SCRIPT),
            "The category name should be escaped and displayed verbatim. Content: " + categoryName.text());
    }

    @Test
    void escapeSectionNameInMenu() throws Exception
    {
        // The section name is controlled through the displayInSection property. The section is added to the first
        // built-in category ("lf") so that selecting the category at index 0 renders the section grid.
        registerConfigurableSection("lf", NO_SCRIPT);

        // The category holding the section is selected: the section grid (#admin_displayCategory) is rendered.
        this.request.put("viewer", "content");
        this.request.put("category", "0");

        Document result = renderHTMLPage(ADMIN_SHEET);

        // The section name is rendered as the (bold) label of the menu entry link. It must be present verbatim, which
        // means it was escaped rather than executed as the noscript macro.
        Element sectionName = result.selectFirst("ul.admin-category strong");
        assertNotNull(sectionName, "No administration section was rendered. Content: " + result.text());
        assertTrue(sectionName.text().contains(NO_SCRIPT),
            "The section name should be escaped and displayed verbatim. Content: " + sectionName.text());
    }

    @Test
    void escapeSectionIconReferenceInMenu() throws Exception
    {
        // The icon reference is built from the name of the attachment referenced by the iconAttachment property, so it
        // is controlled by whoever can attach a file to the configurable page.
        registerConfigurableSection("lf", "mysection", ICON_ATTACHMENT);

        // The category holding the section is selected: the section grid (#admin_displayCategory) is rendered.
        this.request.put("viewer", "content");
        this.request.put("category", "0");

        Document result = renderHTMLPage(ADMIN_SHEET);

        // The icon reference is used as the reference of an image nested in the link label. It must be resolved as a
        // single attachment name instead of being interpreted as wiki syntax.
        Element icon = result.selectFirst("ul.admin-category img");
        assertNotNull(icon, "No administration section icon was rendered. Content: " + result.html());
        assertTrue(icon.attr("alt").endsWith(ICON_ATTACHMENT),
            "The icon reference should be escaped and kept verbatim. Alt: " + icon.attr("alt"));
    }

    private void registerConfigurableSection(String displayInCategory, String displayInSection) throws Exception
    {
        registerConfigurableSection(displayInCategory, displayInSection, null);
    }

    /**
     * Creates a page holding a {@code XWiki.ConfigurableClass} object registering a custom administration section, and
     * makes it the single result of the query used to build the administration menu.
     *
     * @param iconAttachment the name of an image attached to the section page and used as the section icon, or
     *     {@code null} to let the section use the default icon
     */
    private void registerConfigurableSection(String displayInCategory, String displayInSection, String iconAttachment)
        throws Exception
    {
        XWikiDocument sectionDocument = new XWikiDocument(MY_SECTION);
        BaseObject object = sectionDocument.newXObject(CONFIGURABLE_CLASS, this.context);
        object.setStringValue("displayInCategory", displayInCategory);
        object.setStringValue("displayInSection", displayInSection);
        object.set("scope", "WIKI+ALL_SPACES", this.context);
        if (iconAttachment != null) {
            object.setStringValue("iconAttachment", iconAttachment);
            XWikiAttachment attachment = sectionDocument.setAttachment(iconAttachment,
                IOUtils.toInputStream("image", StandardCharsets.UTF_8), this.context);
            attachment.setMimeType("image/png");
        }
        this.xwiki.saveDocument(sectionDocument, this.context);

        EntityReferenceSerializer<String> serializer =
            this.componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(this.query.execute()).thenReturn(List.of(serializer.serialize(MY_SECTION))).thenReturn(List.of());
    }
}
