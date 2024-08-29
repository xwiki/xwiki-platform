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
package org.xwiki.wiki;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.xwiki.icon.IconManager;
import org.xwiki.livedata.internal.macro.LiveDataMacroComponentList;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.plugin.skinx.SkinExtensionPluginApi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of the WikiManager WebHome Document ({@code WikiManager.WebHome}).
 *
 * @version $Id$
 * @since 13.7RC1
 * @since 13.6.1
 * @since 13.4.3
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
@LiveDataMacroComponentList
@ComponentList({
    TemplateScriptService.class
})
class WikiManagerPageTest extends PageTest
{
    /**
     * Verify the resolved configuration of the Live Data macro (stored in the {@code data-config} attribute of the
     * {@code <div/>} produced by the macro.
     */
    @Test
    void verifyLiveDataMacroConfiguration() throws Exception
    {
        // Spy the jsfx plugin used during the macro rendering to return a mock of its API when required. 
        when(this.oldcore.getSpyXWiki().getPluginApi("jsfx", this.context))
            .thenReturn(mock(SkinExtensionPluginApi.class));

        // Return minimal icons metadata since this is not what we want to test in this method.
        IconManager iconManager = this.componentManager.registerMockComponent(IconManager.class);
        doReturn(new HashMap<>()).when(iconManager).getMetaData(anyString());

        Document htmlDocument = renderHTMLPage(new DocumentReference("xwiki", "WikiManager", "WebHome"));
        JSONArray propertyDescriptors =
            new JSONObject(htmlDocument.getElementById("wikis").attr("data-config"))
                .getJSONObject("meta")
                .getJSONArray("propertyDescriptors");
        Optional<JSONObject> ownerPropertyDescriptor = findObjectById(propertyDescriptors, "owner");
        assertFalse(ownerPropertyDescriptor.get().getBoolean("editable"));

        Optional<JSONObject> membershipTypePropertyDescriptor = findObjectById(propertyDescriptors, "membershipType");
        assertFalse(membershipTypePropertyDescriptor.get().getBoolean("editable"));
    }

    /**
     * Search for a {@link JSONObject} with the given id in a {@link JSONArray}. Elements of the json array that are not
     * json objects are ignored.
     *
     * @param jsonArray the json array to inspect
     * @param id the required identifier
     * @return {@link Optional#empty()} if no object with the expected identifier is found, an {@link Optional} object
     *     initialized with the found {@link JSONObject} otherwise
     */
    private Optional<JSONObject> findObjectById(JSONArray jsonArray, String id)
    {
        Optional<JSONObject> propertyDescriptor = Optional.empty();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object element = jsonArray.get(i);
            if (!(element instanceof JSONObject)) {
                continue;
            }
            JSONObject jsonObject = (JSONObject) element;
            if (Objects.equals(jsonObject.get("id"), id)) {
                propertyDescriptor = Optional.of(jsonObject);
            }
        }
        return propertyDescriptor;
    }
}
