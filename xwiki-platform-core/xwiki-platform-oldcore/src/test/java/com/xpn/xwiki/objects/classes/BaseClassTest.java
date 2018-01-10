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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link BaseClass} class.
 *
 * @version $Id$
 */
@ReferenceComponentList
public class BaseClassTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    @Test
    public void setDocumentReference() throws Exception
    {
        BaseClass baseClass = new BaseClass();

        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        baseClass.setDocumentReference(reference);

        assertEquals(reference, baseClass.getDocumentReference());
    }

    @Test
    public void setNameSetWiki() throws Exception
    {
        String database = this.oldcore.getXWikiContext().getWikiId();
        BaseClass baseClass = new BaseClass();

        baseClass.setName("space.page");

        assertEquals(database, baseClass.getDocumentReference().getWikiReference().getName());
        assertEquals("space", baseClass.getDocumentReference().getLastSpaceReference().getName());
        assertEquals("page", baseClass.getDocumentReference().getName());
    }

    @Test
    public void setNameAloneWithChangingContext() throws Exception
    {
        String database = this.oldcore.getXWikiContext().getWikiId();
        BaseClass baseClass = new BaseClass();

        baseClass.setName("space.page");

        try {
            this.oldcore.getXWikiContext().setWikiId("otherwiki");

            assertEquals(database, baseClass.getDocumentReference().getWikiReference().getName());
            assertEquals("space", baseClass.getDocumentReference().getLastSpaceReference().getName());
            assertEquals("page", baseClass.getDocumentReference().getName());

            baseClass.setName("otherspace.otherpage");
        } finally {
            this.oldcore.getXWikiContext().setWikiId(database);
        }

        assertEquals(database, baseClass.getDocumentReference().getWikiReference().getName());
        assertEquals("otherspace", baseClass.getDocumentReference().getLastSpaceReference().getName());
        assertEquals("otherpage", baseClass.getDocumentReference().getName());

        baseClass = new BaseClass();
        try {
            this.oldcore.getXWikiContext().setWikiId("otherwiki");
            baseClass.setName("space.page");
        } finally {
            this.oldcore.getXWikiContext().setWikiId(database);
        }

        assertEquals("otherwiki", baseClass.getDocumentReference().getWikiReference().getName());
        assertEquals("space", baseClass.getDocumentReference().getLastSpaceReference().getName());
        assertEquals("page", baseClass.getDocumentReference().getName());

        baseClass.setName("otherspace.otherpage");

        assertEquals("otherwiki", baseClass.getDocumentReference().getWikiReference().getName());
        assertEquals("otherspace", baseClass.getDocumentReference().getLastSpaceReference().getName());
        assertEquals("otherpage", baseClass.getDocumentReference().getName());
    }

    @Test
    public void addTextAreaFieldWhenNullContentType() throws Exception
    {
        BaseClass baseClass = new BaseClass();

        TextAreaClass textAreaClass = new TextAreaClass();
        textAreaClass.setName("field");
        textAreaClass.setPrettyName("pretty name");
        textAreaClass.setSize(55);
        textAreaClass.setRows(33);
        baseClass.put("field", textAreaClass);

        assertFalse(baseClass.addTextAreaField("field", "pretty name", 55, 33));
    }

    @Test
    public void addTextAreaFieldWhenExistingNumberField() throws Exception
    {
        BaseClass baseClass = new BaseClass();

        baseClass.addNumberField("field", "int pretty name", 30, "int");

        assertTrue(baseClass.addTextAreaField("field", "pretty name", 55, 33));
    }

    @Test
    public void testHashCode()
    {
        String customClass = "test";
        String customMapping = "test";
        String defaultViewSheet = "test";
        String defaultEditSheet = "test";
        String defaultWeb = "test";
        String validationScript = "test";
        String nameField = "test";

        BaseClass c1 = new BaseClass();
        BaseClass c2 = new BaseClass();

        c1.setCustomClass(customClass);
        c1.setCustomMapping(customMapping);
        c1.setDefaultViewSheet(defaultViewSheet);
        c1.setDefaultEditSheet(defaultEditSheet);
        c1.setDefaultWeb(defaultWeb);
        c1.setValidationScript(validationScript);
        c1.setNameField(nameField);

        c2.setCustomClass(customClass);
        c2.setCustomMapping(customMapping);
        c2.setDefaultViewSheet(defaultViewSheet);
        c2.setDefaultEditSheet(defaultEditSheet);
        c2.setDefaultWeb(defaultWeb);
        c2.setValidationScript(validationScript);
        c2.setNameField(nameField);

        Assert.assertEquals(c1.hashCode(), c2.hashCode());
    }
}
