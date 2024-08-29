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
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.template.internal.macro.TemplateMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.syntax.Syntax.PLAIN_1_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;
import static org.xwiki.security.authorization.Right.EDIT;
import static org.xwiki.security.authorization.Right.VIEW;

/**
 * Page test for the {@code hierachy_macro} velocity template.
 *
 * @version $Id$
 * @since 15.9RC1
 * @since 15.5.4
 * @since 14.10.19
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@SecurityScriptServiceComponentList
@ComponentList({
    TemplateMacro.class,
    ModelScriptService.class
})
class HierarchyMacrosPageTest extends PageTest
{
    @BeforeEach
    void setUp()
    {
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(eq(VIEW), any())).thenReturn(true);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(eq(EDIT), any())).thenReturn(true);
    }

    @Test
    void getHierarchyPathData_urlObjectProperty() throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(new DocumentReference("xwiki", "Test", "Page"), this.context);
        document.setSyntax(XWIKI_2_1);
        document.setContent("{{template name='hierarchy_macros.vm' output='false'/}}\n"
            + "{{velocity}}\n"
            + "#set ($entityReference = $services.model."
            + "resolveObjectProperty('xwiki:Panels.ObjectEditorWelcome^Panels.PanelClass[0].content'))\n"
            + "#getHierarchyPathData_url($entityReference)\n"
            + "$url\n"
            + "{{/velocity}}");
        assertEquals("/xwiki/bin/edit/Panels/ObjectEditorWelcome"
                + "?editor=object&classname=Panels.PanelClass&object=0",
            document.getRenderedContent(PLAIN_1_0, this.context).trim());
    }

    @Test
    void getHierarchyPathData_urlObject() throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(new DocumentReference("xwiki", "Test", "Page"), this.context);
        document.setSyntax(XWIKI_2_1);
        document.setContent("{{template name='hierarchy_macros.vm' output='false'/}}\n"
            + "{{velocity}}\n"
            + "#set ($entityReference = $services.model."
            + "resolveObject('xwiki:AppWithinMinutes.LiveTableEditSheet^XWiki.RequiredRightClass[0]'))\n"
            + "#getHierarchyPathData_url($entityReference)\n"
            + "$url\n"
            + "{{/velocity}}");
        assertEquals("/xwiki/bin/edit/AppWithinMinutes/LiveTableEditSheet"
                + "?editor=object&classname=XWiki.RequiredRightClass&object=0",
            document.getRenderedContent(PLAIN_1_0, this.context).trim());
    }

    @Test
    void getHierarchyPathData_urlDocument() throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(new DocumentReference("xwiki", "Test", "Page"), this.context);
        document.setSyntax(XWIKI_2_1);
        document.setContent("{{template name='hierarchy_macros.vm' output='false'/}}\n"
            + "{{velocity}}\n"
            + "#set ($entityReference = $services.model.resolveDocument('xwiki:Tour.WebHome'))\n"
            + "#getHierarchyPathData_url($entityReference)\n"
            + "$url\n"
            + "{{/velocity}}");
        assertEquals("/xwiki/bin/view/Tour/", document.getRenderedContent(PLAIN_1_0, this.context).trim());
    }
}
