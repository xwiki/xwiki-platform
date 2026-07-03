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
package org.xwiki.panels;

import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.context.internal.concurrent.DefaultContextStoreManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.panels.internal.PanelClassDocumentInitializer;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax20ComponentList;

import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Page test for {@code Panels.Members}.
 *
 * @version $Id$
 */
@XWikiSyntax20ComponentList
@HTML50ComponentList
@ComponentList({
    XWikiUsersDocumentInitializer.class,
    PanelClassDocumentInitializer.class,
    DefaultContextStoreManager.class
})
class MembersPageTest extends PageTest
{
    private static final DocumentReference PANEL_REFERENCE = new DocumentReference("xwiki", "Panels", "Members");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "TestUser");

    private static final String USER_REFERENCE_STRING = "XWiki.TestUser";

    @Mock
    private ScriptQuery query;

    @Mock
    private QueryManagerScriptService queryManagerScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        // Initialize the XWiki.XWikiUsers class (required to create a user and to load the panel class).
        this.xwiki.initializeMandatoryDocuments(this.context);

        this.componentManager.registerComponent(ScriptService.class, "query", this.queryManagerScriptService);

        when(this.queryManagerScriptService.xwql(anyString())).thenReturn(this.query);
        when(this.query.addFilter(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.execute()).thenReturn(List.of(USER_REFERENCE_STRING));
    }

    @Test
    void memberWithWikiName() throws Exception
    {
        // Create a user with name "First]] Name".
        String firstName = "First]]";
        String lastName = "Name<a>";
        this.xwiki.createUser(USER_REFERENCE.getName(), Map.of("first_name", firstName, "last_name", lastName),
            this.context);

        // Load the sheet.
        loadPage(new DocumentReference("PanelSheet", PANEL_REFERENCE.getLastSpaceReference()));

        // Render the page.
        Document result = renderHTMLPage(PANEL_REFERENCE);

        String expectedName = firstName + " " + lastName;
        // Verify that the result table contains two rows: the header and the user.
        Elements table = result.getElementsByTag("table");
        assertEquals(1, table.size());
        Elements rows = table.get(0).getElementsByTag("tr");
        assertEquals(2, rows.size());
        // Verify that the display name is rendered correctly in a link in the second cell.
        Elements cells = rows.get(1).getElementsByTag("td");
        assertEquals(2, cells.size());
        Elements link = cells.get(1).getElementsByTag("a");
        assertEquals(1, link.size());
        assertEquals(expectedName, link.get(0).text());
    }
}
