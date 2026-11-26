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
package org.xwiki.icon;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.velocity.tools.JSONTool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Page test of {@code IconThemesCode.IconPicker}.
 *
 * @version $Id$
 */
@XWikiSyntax21ComponentList
class IconPickerPageTest extends PageTest
{
    private static final DocumentReference ICON_PICKER =
        new DocumentReference("xwiki", "IconThemesCode", "IconPicker");

    private static final String ICON_THEME = "FontAwesome";

    @MockComponent(classToMock = IconManagerScriptService.class)
    @Named("icon")
    private ScriptService iconScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        setOutputSyntax(Syntax.PLAIN_1_0);
        this.context.setAction("get");
    }

    @Test
    void dataIconsLeavesOutDeprecatedIcons() throws Exception
    {
        IconManagerScriptService iconService = (IconManagerScriptService) this.iconScriptService;
        when(iconService.getIconNames(ICON_THEME)).thenReturn(List.of("table-sort", "table_sort"));
        when(iconService.getIconNames(ICON_THEME, true)).thenReturn(List.of("table-sort", "table_sort"));
        when(iconService.getIconNames(ICON_THEME, false)).thenReturn(List.of("table-sort"));

        this.request.put("action", "data_icons");
        this.request.put("iconTheme", ICON_THEME);
        this.request.put("query", "table");

        JsonNode icons = renderJSON();

        List<String> iconNames = new ArrayList<>();
        icons.forEach(icon -> iconNames.add(icon.get("name").asText()));
        assertEquals(List.of("table-sort"), iconNames);
    }

    private JsonNode renderJSON() throws Exception
    {
        JSONTool jsonTool = mock(JSONTool.class);
        registerVelocityTool("jsontool", jsonTool);

        renderPage(ICON_PICKER);

        ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);
        verify(jsonTool).serialize(argument.capture());
        return new ObjectMapper().valueToTree(argument.getValue());
    }
}
