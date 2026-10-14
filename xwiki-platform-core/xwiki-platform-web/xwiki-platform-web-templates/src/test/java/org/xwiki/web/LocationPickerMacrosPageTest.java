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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.template.internal.macro.TemplateMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xwiki.rendering.syntax.Syntax.PLAIN_1_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Tests the {@code locationPicker_macros.vm} template.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    TemplateMacro.class,
    ModelScriptService.class
})
class LocationPickerMacrosPageTest extends PageTest
{
    @Test
    void serializeLocationsWithSpaceReferences() throws Exception
    {
        assertEquals("[A.B, other:C, D]", serializeLocations(
            "[$services.model.resolveSpace('xwiki:A.B'), $services.model.resolveSpace('other:C'), 'D']"));
    }

    @Test
    void serializeLocationsWithSingleSpaceReference() throws Exception
    {
        assertEquals("[A.B]", serializeLocations("$services.model.resolveSpace('xwiki:A.B')"));
    }

    @Test
    void serializeLocationsWithString() throws Exception
    {
        assertEquals("A.B,C", serializeLocations("'A.B,C'"));
    }

    private String serializeLocations(String locations) throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(new DocumentReference("xwiki", "Test", "Page"), this.context);
        document.setSyntax(XWIKI_2_1);
        document.setContent("{{template name='locationPicker_macros.vm' output='false'/}}\n"
            + "{{velocity}}\n"
            + "#set ($locations = " + locations + ")\n"
            + "#_serializeLocations($locations $serializedLocations)\n"
            + "$serializedLocations\n"
            + "{{/velocity}}");
        return document.getRenderedContent(PLAIN_1_0, this.context).trim();
    }
}
