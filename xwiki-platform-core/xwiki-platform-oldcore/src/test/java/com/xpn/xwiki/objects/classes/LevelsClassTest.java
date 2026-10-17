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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.XWikiRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LevelsClass}
 *
 * @version $Id$
 * @since 13.3RC1
 * @since 12.10.7
 */
@OldcoreTest
class LevelsClassTest
{
    private static final List<String> DEFAULT_LIST = Arrays.asList(
        "admin",
        "programming",
        "edit",
        "view",
        "comment",
        "script",
        "delete"
    );

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    private XWikiContext context;
    private XWikiRequest xWikiRequest;

    @BeforeEach
    void setup() throws XWikiException
    {
        this.context = this.oldcore.getXWikiContext();
        XWiki xWiki = mock(XWiki.class);
        this.context.setWiki(xWiki);
        XWikiRightService xWikiRightService = mock(XWikiRightService.class);
        when(xWiki.getRightService()).thenReturn(xWikiRightService);
        when(xWikiRightService.listAllLevels(context)).thenReturn(DEFAULT_LIST);
        this.xWikiRequest = mock(XWikiRequest.class);
        this.context.setRequest(this.xWikiRequest);
    }

    @Test
    void displayEdit()
    {
        String name = "rights";
        String prefix = "authorization";
        BaseCollection baseCollection = mock(BaseCollection.class);
        BaseProperty baseProperty = mock(BaseProperty.class);
        when(baseCollection.safeget(name)).thenReturn(baseProperty);
        when(baseProperty.getValue()).thenReturn("view,eDiT, Comment");
        StringBuffer stringBuffer = new StringBuffer();

        LevelsClass levelsClass = new LevelsClass();
        levelsClass.displayEdit(stringBuffer, name, prefix, baseCollection, this.context);

        // View and edit should be selected even despite the weird case in edit right
        // Comment should not be selected since there's a space before it
        String actual = stringBuffer.toString();
        int selectOpeningTagEnd = actual.indexOf('>') + 1;

        // org.apache.ecs.xhtml.select stores its attributes in a java.util.Hashtable, whose iteration order shifts
        // whenever an attribute is added or removed. Assert the opening tag's attributes are present rather than
        // relying on a specific order, so the next attribute added here doesn't break this test again.
        assertThat(actual.substring(0, selectOpeningTagEnd), allOf(
            startsWith("<select "),
            endsWith(">"),
            containsString(" size='6' "),
            containsString(" id='authorizationrights' "),
            containsString(" aria-label='Levels List' "),
            containsString(" name='authorizationrights'")));

        String expectedRest = "<option value='admin' label='admin'>admin</option>"
            + "<option value='programming' label='programming'>programming</option>"
            + "<option selected='selected' value='edit' label='edit'>edit</option>"
            + "<option selected='selected' value='view' label='view'>view</option>"
            + "<option value='comment' label='comment'>comment</option>"
            + "<option value='script' label='script'>script</option>"
            + "<option value='delete' label='delete'>delete</option>"
            + "</select><input name='authorizationrights' type='hidden'/>";
        assertEquals(expectedRest, actual.substring(selectOpeningTagEnd));
    }

    @Test
    void fromList()
    {
        BaseProperty baseProperty = mock(LargeStringProperty.class);
        List<String> list = Arrays.asList("admin", null, "view", "");
        LevelsClass levelsClass = new LevelsClass();
        levelsClass.setMultiSelect(true);
        levelsClass.fromList(baseProperty, list);
        verify(baseProperty).setValue("admin,view");
    }

    @Test
    void fromStringArray()
    {
        String[] array = new String[] {"admin", null, "view", ""};
        LevelsClass levelsClass = new LevelsClass();
        levelsClass.setMultiSelect(true);
        StringProperty expectedProperty = new StringProperty();
        expectedProperty.setValue("admin,view");
        expectedProperty.setName("levelslist");
        assertEquals(expectedProperty, levelsClass.fromStringArray(array));

        array = new String[] { "edit,script" };
        expectedProperty.setValue("edit,script");
        assertEquals(expectedProperty, levelsClass.fromStringArray(array));
    }
}
