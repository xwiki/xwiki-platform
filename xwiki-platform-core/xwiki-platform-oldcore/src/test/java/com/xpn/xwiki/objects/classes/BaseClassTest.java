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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.diff.internal.DefaultDiffManager;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.store.merge.MergeManagerResult;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link BaseClass} class.
 *
 * @version $Id$
 */
@OldcoreTest
// @formatter:off
@ComponentList({
    DefaultDiffManager.class
})
// @formatter:on
@ReferenceComponentList
class BaseClassTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private MergeManager mergeManager;

    @Test
    void setDocumentReference()
    {
        BaseClass baseClass = new BaseClass();

        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        baseClass.setDocumentReference(reference);

        assertEquals(reference, baseClass.getDocumentReference());
    }

    @Test
    void setNameSetWiki()
    {
        String database = this.oldcore.getXWikiContext().getWikiId();
        BaseClass baseClass = new BaseClass();

        baseClass.setName("space.page");

        assertEquals(database, baseClass.getDocumentReference().getWikiReference().getName());
        assertEquals("space", baseClass.getDocumentReference().getLastSpaceReference().getName());
        assertEquals("page", baseClass.getDocumentReference().getName());
    }

    @Test
    void setNameAloneWithChangingContext()
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
    void addTextAreaFieldWhenNullContentType()
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
    void addTextAreaFieldWhenExistingNumberField()
    {
        BaseClass baseClass = new BaseClass();

        baseClass.addNumberField("field", "int pretty name", 30, "int");

        assertTrue(baseClass.addTextAreaField("field", "pretty name", 55, 33));
    }

    @Test
    void merge()
    {
        BaseClass previousClass = new BaseClass();
        previousClass.setPrettyName("previous");
        previousClass.setValidationScript("my validation script");
        previousClass.setDefaultEditSheet("A previous edit sheet");

        BaseClass currentClass = new BaseClass();
        currentClass.setPrettyName("current");
        currentClass.setValidationScript("my validation script");
        currentClass.setDefaultEditSheet("An edit sheet");

        BaseClass newClass = new BaseClass();
        newClass.setPrettyName("new");
        newClass.setValidationScript("my new validation script");
        newClass.setDefaultEditSheet("A previous edit sheet");
        when(mergeManager.mergeObject(any(), any(), any(), any())).thenReturn(new MergeManagerResult<>());

        MergeManagerResult<String, Character> prettyNameManagerResult = new MergeManagerResult<>();
        prettyNameManagerResult.setMergeResult("current");
        prettyNameManagerResult.setModified(false);
        prettyNameManagerResult.getLog()
            .error("Failed to merge objects: previous=[previous] new=[new] current=[current]");
        when(mergeManager.mergeCharacters(eq("previous"), eq("new"), eq("current"), any()))
            .thenReturn(prettyNameManagerResult);

        MergeManagerResult<String,  String> mergeManagerResult = new MergeManagerResult<>();
        mergeManagerResult.setMergeResult("my new validation script");
        mergeManagerResult.setModified(true);
        when(mergeManager
            .mergeObject(eq("my validation script"), eq("my new validation script"), eq("my validation script"), any()))
            .thenReturn(mergeManagerResult);

        mergeManagerResult = new MergeManagerResult<>();
        mergeManagerResult.setMergeResult("An edit sheet");
        mergeManagerResult.setModified(false);
        when(mergeManager
            .mergeObject(eq("A previous edit sheet"), eq("A previous edit sheet"), eq("An edit sheet"), any()))
            .thenReturn(mergeManagerResult);

        MergeResult mergeResult = new MergeResult();
        currentClass.merge(previousClass, newClass, new MergeConfiguration(), oldcore.getXWikiContext(), mergeResult);

        List<LogEvent> errors = mergeResult.getLog().getLogsFrom(LogLevel.ERROR);
        assertEquals(1, errors.size());
        assertEquals("ERROR:Failed to merge objects: previous=[previous] new=[new] current=[current]",
            errors.get(0).toString());

        assertTrue(mergeResult.isModified());
        assertEquals("current", currentClass.getPrettyName());
        assertEquals("my new validation script", currentClass.getValidationScript());
        assertEquals("An edit sheet", currentClass.getDefaultEditSheet());
    }

    @Test
    void addStaticListField()
    {
        BaseClass baseClass = new BaseClass();

        StaticListClass slc = baseClass.addStaticListField("field");
        slc.setPrettyName("Custom Field");

        assertNotNull(slc);
        assertNotNull(slc.getObject());
        assertNotNull(baseClass.get("field"));
        assertEquals("Custom Field", ((StaticListClass) baseClass.get("field")).getPrettyName());
    }
}
