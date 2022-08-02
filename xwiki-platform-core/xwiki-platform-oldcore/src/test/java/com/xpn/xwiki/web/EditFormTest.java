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
package com.xpn.xwiki.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EditForm}.
 *
 * @since 12.4RC1
 * @version $Id$
 */
class EditFormTest
{
    private HttpServletRequest httpRequest;

    private EditForm editForm;

    @BeforeEach
    public void setup()
    {
        this.editForm = new EditForm();
        this.httpRequest = mock(HttpServletRequest.class);
        this.editForm.setRequest(httpRequest);
    }

    @Test
    public void getContent()
    {
        when(this.httpRequest.getParameter("content")).thenReturn("My new content");
        this.editForm.readRequest();
        assertEquals("My new content", this.editForm.getContent());
    }

    @Test
    public void getObjectPolicy()
    {
        when(this.httpRequest.getParameter("objectPolicy")).thenReturn("updateOrCreate");
        this.editForm.readRequest();
        assertEquals(ObjectPolicyType.UPDATE_OR_CREATE, this.editForm.getObjectPolicy());

        when(this.httpRequest.getParameter("objectPolicy")).thenReturn("");
        this.editForm.readRequest();
        assertNull(this.editForm.getObjectPolicy());

        when(this.httpRequest.getParameter("objectPolicy")).thenReturn("foobar");
        this.editForm.readRequest();
        assertNull(this.editForm.getObjectPolicy());

        when(this.httpRequest.getParameter("objectPolicy")).thenReturn("UPDATE");
        this.editForm.readRequest();
        assertNull(this.editForm.getObjectPolicy());

        when(this.httpRequest.getParameter("objectPolicy")).thenReturn("update");
        this.editForm.readRequest();
        assertEquals(ObjectPolicyType.UPDATE, this.editForm.getObjectPolicy());
    }

    @Test
    public void getUpdateOrCreateMap()
    {
        when(this.httpRequest.getParameter("objectPolicy")).thenReturn("");
        Map<String, String[]> parameterMap = new HashMap<>();

        String[] paramValue1 = { "value1", "value2" };
        String[] paramValue2 = { "value1" };
        String[] paramValue3 = { "value2" };
        String[] emptyValue = {""};

        parameterMap.put("someParam", paramValue2);
        parameterMap.put("XWiki.XWikiRights_42_baz", paramValue1);
        parameterMap.put("XWiki.XWikiRights_0_foo", paramValue1);
        parameterMap.put("XWiki.XWikiRights_1_foo", paramValue2);
        parameterMap.put("XWiki.XWikiRights_1_bar", paramValue3);
        parameterMap.put("EditIT.saveActionValidatesWhenXValidateIsPresent.WebHome_0_prop", paramValue1);
        parameterMap.put("Space.Some-Class_12_prop-erty", paramValue3);
        parameterMap.put("foobar_18_buz", paramValue1);
        parameterMap.put("foo_48bar_buz", paramValue1);
        parameterMap.put("A class.With Some spaces_42_myProp", paramValue1);
        parameterMap.put("XWiki.XWikiRights_2_foo",  emptyValue);

        // Note that this one might be ambiguous, page name could be WebHome or WebHome_0_Something
        // This should be fixed as part of https://jira.xwiki.org/browse/XWIKI-17302
        parameterMap.put("MyClass_18_param.WebHome_0_Something_18_name", paramValue1);
        when(this.httpRequest.getParameterMap()).thenReturn(parameterMap);
        this.editForm.readRequest();
        assertEquals(Collections.emptyMap(), this.editForm.getUpdateOrCreateMap());

        when(this.httpRequest.getParameter("objectPolicy")).thenReturn("updateOrCreate");
        this.editForm.readRequest();

        Map<String, SortedMap<Integer, Map<String, String[]>>> expectedMap = new HashMap<>();
        TreeMap<Integer, Map<String, String[]>> treeMap = new TreeMap<>();
        treeMap.put(0, Collections.singletonMap("foo", paramValue1));
        treeMap.put(2, Collections.singletonMap("foo", emptyValue));
        treeMap.put(42, Collections.singletonMap("baz", paramValue1));
        Map<String, String[]> parametersValues = new HashMap<>();
        parametersValues.put("foo", paramValue2);
        parametersValues.put("bar", paramValue3);
        treeMap.put(1, parametersValues);
        expectedMap.put("XWiki.XWikiRights", treeMap);

        treeMap = new TreeMap<>();
        treeMap.put(0, Collections.singletonMap("Something_18_name", paramValue1));
        expectedMap.put("MyClass_18_param.WebHome", treeMap);

        treeMap = new TreeMap<>();
        treeMap.put(0, Collections.singletonMap("prop", paramValue1));
        expectedMap.put("EditIT.saveActionValidatesWhenXValidateIsPresent.WebHome", treeMap);

        treeMap = new TreeMap<>();
        treeMap.put(12, Collections.singletonMap("prop-erty", paramValue3));
        expectedMap.put("Space.Some-Class", treeMap);

        treeMap = new TreeMap<>();
        treeMap.put(42, Collections.singletonMap("myProp", paramValue1));
        expectedMap.put("A class.With Some spaces", treeMap);

        assertEquals(expectedMap, this.editForm.getUpdateOrCreateMap());
    }

    @Test
    public void getObjectsToAdd()
    {
        when(this.httpRequest.getParameterValues("addedObjects")).thenReturn(null);
        this.editForm.readRequest();
        assertEquals(Collections.emptyMap(), this.editForm.getObjectsToAdd());

        when(this.httpRequest.getParameterValues("addedObjects")).thenReturn(new String[] { "" });
        this.editForm.readRequest();
        assertEquals(Collections.emptyMap(), this.editForm.getObjectsToAdd());

        when(this.httpRequest.getParameterValues("addedObjects")).thenReturn(new String[] {
            "XWiki.XWikiRights_42",
            "XWiki.XWikiRights_02",
            "XWiki.XWikiRights_foo",
            "",
            "MyClass_18_param.WebHome_0_Something_18",
            "EditIT.saveActionValidatesWhenXValidateIsPresent.WebHome_0",
            "XWiki.MyClass_1",
            "XWiki.XWikiRights_23",
            "AnotherClass_010",
            "foo_18_22",
            "_29",
            "A class.With Some spaces_42"
        });
        this.editForm.readRequest();

        Map<String, List<Integer>> expectedMap = new HashMap<>();
        expectedMap.put("XWiki.XWikiRights", Arrays.asList(42, 2, 23));
        expectedMap.put("XWiki.MyClass", Collections.singletonList(1));
        expectedMap.put("MyClass_18_param.WebHome_0_Something", Collections.singletonList(18));
        expectedMap.put("EditIT.saveActionValidatesWhenXValidateIsPresent.WebHome", Collections.singletonList(0));
        expectedMap.put("A class.With Some spaces", Collections.singletonList(42));
        assertEquals(expectedMap, this.editForm.getObjectsToAdd());
    }

    @Test
    public void getObjectsToRemove()
    {
        when(this.httpRequest.getParameterValues("deletedObjects")).thenReturn(null);
        this.editForm.readRequest();
        assertEquals(Collections.emptyMap(), this.editForm.getObjectsToRemove());

        when(this.httpRequest.getParameterValues("deletedObjects")).thenReturn(new String[] { "" });
        this.editForm.readRequest();
        assertEquals(Collections.emptyMap(), this.editForm.getObjectsToRemove());

        when(this.httpRequest.getParameterValues("deletedObjects")).thenReturn(new String[] {
            "XWiki.XWikiRights_42",
            "XWiki.XWikiRights_02",
            "XWiki.XWikiRights_foo",
            "",
            "MyClass_18_param.WebHome_0_Something_18",
            "EditIT.saveActionValidatesWhenXValidateIsPresent.WebHome_0",
            "XWiki.MyClass_1",
            "XWiki.XWikiRights_23",
            "AnotherClass_010",
            "foo_18_22",
            "_29",
            "A class.With Some spaces_42"
        });
        this.editForm.readRequest();

        Map<String, List<Integer>> expectedMap = new HashMap<>();
        expectedMap.put("XWiki.XWikiRights", Arrays.asList(42, 2, 23));
        expectedMap.put("XWiki.MyClass", Collections.singletonList(1));
        expectedMap.put("MyClass_18_param.WebHome_0_Something", Collections.singletonList(18));
        expectedMap.put("EditIT.saveActionValidatesWhenXValidateIsPresent.WebHome", Collections.singletonList(0));
        expectedMap.put("A class.With Some spaces", Collections.singletonList(42));
        assertEquals(expectedMap, this.editForm.getObjectsToRemove());
    }
}