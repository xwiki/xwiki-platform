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
package org.xwiki.store.merge.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.CacheManager;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.diff.internal.DefaultConflictDecision;
import org.xwiki.diff.internal.DefaultDiffManager;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.store.merge.MergeConflictDecisionsManager;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManagerResult;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentList({ DefaultDiffManager.class })
@ComponentTest
public class DefaultMergeManagerTest
{
    @InjectMockComponents
    private DefaultMergeManager mergeManager;

    @MockComponent
    private MergeConflictDecisionsManager conflictDecisionsManager;

    private DocumentReference documentReference;
    private EntityReference userReference;
    private List<ConflictDecision> conflictDecisionList;

    @BeforeEach
    public void setup()
    {
        this.documentReference = new DocumentReference("xwiki", "Space", "Page");
        this.userReference = new DocumentReference("xwiki", "XWiki", "User");
        this.conflictDecisionList = new ArrayList<>();

        when(conflictDecisionsManager.getConflictDecisionList(documentReference, userReference))
            .thenReturn(conflictDecisionList);
    }

    @Test
    public void mergeWhenDifferences()
    {
        MergeManagerResult<String, String> result =
            mergeManager.mergeLines("content", "content\n", "content", new MergeConfiguration());
        assertEquals("content\n", result.getMergeResult());
        assertTrue(result.isModified());
        assertFalse(result.hasConflicts());

        MergeConfiguration mergeConfiguration = new MergeConfiguration();
        mergeConfiguration.setConcernedDocument(documentReference);
        mergeConfiguration.setUserReference(userReference);

        result =
            mergeManager.mergeLines("content", "content\n", "content", mergeConfiguration);
        assertEquals("content\n", result.getMergeResult());
        assertTrue(result.isModified());
        assertFalse(result.hasConflicts());
        verify(conflictDecisionsManager, times(1)).getConflictDecisionList(documentReference, userReference);
    }

    @Test
    public void mergeWhenCurrentStringDoesntEndWithNewLine()
    {
        MergeManagerResult<String, String> result = mergeManager.mergeLines("content", "content", "content",
            new MergeConfiguration());
        assertEquals("content", result.getMergeResult());
        assertFalse(result.isModified());
        assertFalse(result.hasConflicts());
    }

    @Test
    public void mergeWhenCurrentStringEndsWithNewLine()
    {
        MergeManagerResult<String, String> result = mergeManager.mergeLines("content\n", "content\n", "content\n",
            new MergeConfiguration());
        assertEquals("content\n", result.getMergeResult());
        assertFalse(result.isModified());
        assertFalse(result.hasConflicts());
    }

    @Test
    public void mergeObjectSimple()
    {
        MergeManagerResult<String, String> result = mergeManager.mergeObject("old", "new", "old",
            new MergeConfiguration());
        assertEquals("new", result.getMergeResult());
        assertTrue(result.isModified());
        assertFalse(result.hasConflicts());
    }

    @Test
    public void mergeObjectAlreadyDone()
    {
        MergeManagerResult<String, String> result = mergeManager.mergeObject("old", "new", "new",
            new MergeConfiguration());
        assertEquals("new", result.getMergeResult());
        assertFalse(result.isModified());
        assertFalse(result.hasConflicts());
    }

    @Test
    public void mergeObjectWhileModified()
    {
        MergeManagerResult<String, String> result = mergeManager.mergeObject("old", "new", "old modified",
            new MergeConfiguration());
        assertEquals("old modified", result.getMergeResult());
        assertFalse(result.isModified());
        // conflicts are flagged as errors in the log
        assertFalse(result.getLog().getLogs(LogLevel.ERROR).isEmpty());
        assertTrue(result.hasConflicts());

    }

    @Test
    public void mergeListSimple()
    {
        List<String> current = new ArrayList<String>(Arrays.asList("old1", "old2"));
        MergeManagerResult<List<String>, String> result = mergeManager.mergeList(Arrays.asList("old1", "old2"),
            Arrays.asList("new1", "new2"), current, new MergeConfiguration());
        assertEquals(Arrays.asList("new1", "new2"), result.getMergeResult());
        assertTrue(result.isModified());
        assertFalse(result.hasConflicts());
    }

    @Test
    public void mergeListAlreadyDone()
    {
        List<String> current = new ArrayList<String>(Arrays.asList("new1", "new2"));
        MergeManagerResult<List<String>, String> result = mergeManager.mergeList(Arrays.asList("old1", "old2"),
            Arrays.asList("new1", "new2"), current, new MergeConfiguration());
        assertEquals(Arrays.asList("new1", "new2"), result.getMergeResult());
        assertEquals(Arrays.asList("new1", "new2"), current);
        assertFalse(result.isModified());
        assertFalse(result.hasConflicts());
    }

    @Test
    public void mergeListWhileModified()
    {
        List<String> current = new ArrayList<String>(Arrays.asList("new1", "old modified2"));
        MergeManagerResult<List<String>, String> result = mergeManager.mergeList(Arrays.asList("new1", "old2"),
            Arrays.asList("new1", "new2"), current, new MergeConfiguration());
        assertEquals(Arrays.asList("new1", "old modified2"), result.getMergeResult());
        assertFalse(result.isModified());
        // conflicts are flagged as errors in the log
        assertFalse(result.getLog().getLogs(LogLevel.ERROR).isEmpty());
        assertTrue(result.hasConflicts());

        // Apply a decision to fix the conflict
        ConflictDecision<String> conflictDecision = new DefaultConflictDecision<>(result.getConflicts().get(0));
        conflictDecision.setCustom(Arrays.asList("Foo", "Bar", "Thing"));
        this.conflictDecisionList.add(conflictDecision);

        MergeConfiguration mergeConfiguration = new MergeConfiguration();
        mergeConfiguration.setUserReference(this.userReference);
        mergeConfiguration.setConcernedDocument(this.documentReference);

        result = mergeManager.mergeList(Arrays.asList("new1", "old2"),
            Arrays.asList("new1", "new2"), current, mergeConfiguration);
        assertEquals(Arrays.asList("new1", "old modified2"), current);
        assertEquals(Arrays.asList("new1", "Foo", "Bar", "Thing"), result.getMergeResult());
        assertTrue(result.isModified());
        assertTrue(result.getLog().getLogs(LogLevel.ERROR).isEmpty());
        assertFalse(result.hasConflicts());
    }

    @Test
    public void mergeCharactersSimple()
    {
        MergeManagerResult<String, Character> result =
            mergeManager.mergeCharacters("ab", "aib", "abc", new MergeConfiguration());
        assertEquals("aibc", result.getMergeResult());
        assertTrue(result.isModified());
        assertFalse(result.hasConflicts());
    }

    @Test
    public void mergeCharactersNull()
    {
        MergeManagerResult<String, Character> result =
            mergeManager.mergeCharacters(null, null, null, new MergeConfiguration());
        assertNull(result.getMergeResult());
        assertFalse(result.isModified());
        assertFalse(result.hasConflicts());
    }

    @Test
    public void mergeCharactersEmpty()
    {
        MergeManagerResult<String, Character> result =
            mergeManager.mergeCharacters("", "", "", new MergeConfiguration());
        assertEquals("", result.getMergeResult());
        assertFalse(result.isModified());
        assertFalse(result.hasConflicts());
    }

    @Test
    public void mergeCharactersWhileModified()
    {
        MergeManagerResult<String, Character> result =
            mergeManager.mergeCharacters("ab", "aib", "ajb", new MergeConfiguration());
        assertEquals("ajb", result.getMergeResult());
        assertFalse(result.isModified());
        assertTrue(result.hasConflicts());
    }

    @Test
    public void mergeCharactersNoChanges()
    {
        MergeManagerResult<String, Character> result =
            mergeManager.mergeCharacters("ab", "ab", "ab", new MergeConfiguration());
        assertEquals("ab", result.getMergeResult());
        assertFalse(result.isModified());
        assertFalse(result.hasConflicts());
    }

    @Nested
    @DisplayName("Merge Documents tests")
    @ComponentList(value = {
        DefaultDiffManager.class,
        DefaultMergeManager.class,
        DefaultMergeConflictDecisionsManager.class
    })
    @ReferenceComponentList
    @OldcoreTest
    public class MergeManagerDocumentsTest
    {
        @InjectMockitoOldcore
        private MockitoOldcore oldcore;

        @InjectMockComponents
        private DefaultMergeManager mergeManager;

        @MockComponent
        private CacheManager cacheManager;

        private XWikiDocument currentDocument;

        private XWikiDocument previousDocument;

        private XWikiDocument nextDocument;

        private BaseObject xobject;

        private BaseClass xclass;

        private MergeConfiguration configuration;

        @BeforeEach
        public void before() throws Exception
        {
            this.currentDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
            this.previousDocument = this.currentDocument.clone();
            this.nextDocument = this.currentDocument.clone();

            this.xclass = new BaseClass();
            this.xclass.setDocumentReference(new DocumentReference("wiki", "classspace", "class"));
            this.xclass.addTextField("string", "String", 30);
            this.xclass.addTextAreaField("area", "Area", 10, 10);
            this.xclass.addTextAreaField("puretextarea", "Pure text area", 10, 10);
            // set the text areas an non interpreted content
            ((TextAreaClass) this.xclass.getField("puretextarea")).setContentType("puretext");
            this.xclass.addPasswordField("passwd", "Password", 30);
            this.xclass.addBooleanField("boolean", "Boolean", "yesno");
            this.xclass.addNumberField("int", "Int", 10, "integer");
            this.xclass.addStaticListField("stringlist", "StringList", "value1, value2");

            this.xobject = new BaseObject();
            this.xobject.setXClassReference(this.xclass.getDocumentReference());
            this.xobject.setStringValue("string", "string");
            this.xobject.setLargeStringValue("area", "area");
            this.xobject.setStringValue("passwd", "passwd");
            this.xobject.setIntValue("boolean", 1);
            this.xobject.setIntValue("int", 42);
            this.xobject.setStringListValue("stringlist", Arrays.asList("VALUE1", "VALUE2"));

            this.configuration = new MergeConfiguration();
        }

        private MergeDocumentResult merge() throws Exception
        {
            MergeDocumentResult result = this.mergeManager.mergeDocument(this.previousDocument, this.nextDocument,
                this.currentDocument, this.configuration);

            List<LogEvent> exception = result.getLog().getLogs(LogLevel.ERROR);
            if (!exception.isEmpty()) {
                throw new MergeException(exception.get(0).getFormattedMessage(), exception.get(0).getThrowable());
            }

            return result;
        }

        // Tests

        // #merge

        @Test
        public void testMergeContent() throws Exception
        {
            this.previousDocument.setContent("some content");
            this.nextDocument.setContent("some new content");
            this.currentDocument.setContent("some content");

            merge();

            assertEquals("some new content", this.currentDocument.getContent());
        }

        @Test
        public void testMergeDefaultLocale() throws Exception
        {
            this.previousDocument.setDefaultLocale(Locale.ENGLISH);
            this.nextDocument.setDefaultLocale(Locale.FRENCH);
            this.currentDocument.setDefaultLocale(Locale.ENGLISH);

            merge();

            assertEquals(Locale.FRENCH, this.currentDocument.getDefaultLocale());
        }

        @Test
        public void testMergeContentModified() throws Exception
        {
            this.previousDocument.setContent("some content");
            this.nextDocument.setContent("some content\nafter");
            this.currentDocument.setContent("before\nsome content");

            merge();

            assertEquals("before\nsome content\nafter", this.currentDocument.getContent());

            this.previousDocument.setContent("some content");
            this.nextDocument.setContent("some content\nafter");
            this.currentDocument.setContent("some content");

            merge();

            assertEquals("some content\nafter", this.currentDocument.getContent());
        }

        @Test
        public void testMergeNewObjectAdded() throws Exception
        {
            this.nextDocument.addXObject(this.xobject);

            merge();

            assertEquals(this.xobject, this.currentDocument.getXObject(this.xclass.getReference(), 0));
        }

        @Test
        public void testMergeNewObjectRemoved() throws Exception
        {
            this.previousDocument.addXObject(this.xobject);
            this.currentDocument.addXObject(this.xobject.clone());

            merge();

            assertNull(this.currentDocument.getXObject(this.xclass.getReference(), 0));
        }

        @Test
        public void testMergeObjectModified() throws Exception
        {
            BaseObject previousobj = this.xobject;
            previousobj.setStringValue("test", "test1");
            this.previousDocument.addXObject(previousobj);

            BaseObject obj = this.xobject.clone();
            obj.setStringValue("test", "test1");
            this.currentDocument.addXObject(obj);

            BaseObject newobj = this.xobject.clone();
            newobj.setStringValue("test", "test2");
            this.nextDocument.addXObject(newobj);

            merge();

            BaseObject mergedobj = this.currentDocument.getXObject(this.xclass.getReference(), 0);

            assertNotNull(mergedobj);
            assertEquals("test2", mergedobj.getStringValue("test"));
        }

        @Test
        public void testMergeCurrentObjectRemoved() throws Exception
        {
            this.xobject.setStringValue("test", "");
            this.xobject.setStringValue("previoustest", "previoustest");
            this.previousDocument.addXObject(this.xobject);

            BaseObject newobj = this.xobject.clone();
            newobj.setStringValue("test", "test2");
            newobj.setStringValue("newtest", "newtest");
            this.nextDocument.addXObject(newobj);

            merge();

            BaseObject mergedobj = this.currentDocument.getXObject(this.xclass.getReference(), 0);

            assertNull(mergedobj);
        }

        @Test
        public void testMergeAttachmentEquals() throws Exception
        {
            XWikiAttachment attachment = new XWikiAttachment();

            attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
            attachment.setLongSize(10);
            attachment.setFilename("file");

            this.previousDocument.addAttachment(attachment);
            this.nextDocument.addAttachment(attachment.clone());
            this.currentDocument.addAttachment(attachment.clone());

            MergeDocumentResult result = merge();

            assertFalse(result.isModified());
        }

        @Test
        public void testMergeAttachmentEqualsDeletedCurrent() throws Exception
        {
            XWikiAttachment attachment = new XWikiAttachment();

            attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
            attachment.setLongSize(10);
            attachment.setFilename("file");

            this.previousDocument.addAttachment(attachment);
            this.nextDocument.addAttachment(attachment.clone());

            MergeDocumentResult result = merge();

            assertFalse(result.isModified());
        }

        @Test
        public void testMergeAttachmentEqualsAddedCurrent() throws Exception
        {
            XWikiAttachment attachment = new XWikiAttachment();

            attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
            attachment.setLongSize(10);
            attachment.setFilename("file");

            this.currentDocument.addAttachment(attachment);

            MergeDocumentResult result = merge();

            assertFalse(result.isModified());
        }

        @Test
        public void testMergeAttachmentEqualsModifiedCurrent() throws Exception
        {
            XWikiAttachment attachment = new XWikiAttachment();

            attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
            attachment.setLongSize(10);
            attachment.setFilename("file");

            this.previousDocument.addAttachment(attachment);
            this.nextDocument.addAttachment(attachment.clone());

            attachment = attachment.clone();
            attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 });
            attachment.setLongSize(9);

            this.currentDocument.addAttachment(attachment);

            MergeDocumentResult result = merge();

            assertFalse(result.isModified());
        }

        @Test
        public void testMergeAttachmentNew() throws Exception
        {
            XWikiAttachment attachment = new XWikiAttachment();

            attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
            attachment.setLongSize(10);
            attachment.setFilename("file");

            this.nextDocument.addAttachment(attachment);

            MergeDocumentResult result = merge();

            assertTrue(result.isModified());

            XWikiAttachment newAttachment = this.currentDocument.getAttachment("file");

            assertNotNull(newAttachment);
            assertEquals(10, newAttachment.getLongSize());
            assertArrayEquals(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, newAttachment.getContent(null));
        }

        @Test
        public void mergeAttachmentNewButAddedInCurrent() throws Exception
        {
            XWikiAttachment attachment = new XWikiAttachment();

            attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
            attachment.setLongSize(10);
            attachment.setFilename("file");

            this.currentDocument.addAttachment(attachment);
            this.nextDocument.addAttachment(attachment);

            MergeDocumentResult result = merge();

            assertFalse(result.isModified());
            List<LogEvent> logs = result.getLog().getLogs(LogLevel.WARN);
            assertEquals(1, logs.size());
            assertEquals("Attachment [Attachment wiki:space.page@file] already added",
                logs.get(0).getFormattedMessage());

            XWikiAttachment newAttachment = this.currentDocument.getAttachment("file");
            assertSame(attachment, newAttachment);
        }

        @Test
        public void testMergeAttachmentDeleted() throws Exception
        {
            XWikiAttachment attachment = new XWikiAttachment();

            attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
            attachment.setLongSize(10);
            attachment.setFilename("file");

            this.currentDocument.addAttachment(attachment);
            this.previousDocument.addAttachment(attachment.clone());

            MergeDocumentResult result = merge();

            assertTrue(result.isModified());

            XWikiAttachment newAttachment = this.currentDocument.getAttachment("file");

            assertNull(newAttachment);
        }

        @Test
        public void testMergeAttachmentModified() throws Exception
        {
            XWikiAttachment attachment = new XWikiAttachment();

            attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
            attachment.setLongSize(10);
            attachment.setFilename("file");

            this.currentDocument.addAttachment(attachment);
            this.previousDocument.addAttachment(attachment.clone());

            attachment = attachment.clone();
            attachment.setContent(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 });
            attachment.setLongSize(9);

            this.nextDocument.addAttachment(attachment);

            MergeDocumentResult result = merge();

            assertTrue(result.isModified());

            XWikiAttachment newAttachment = this.currentDocument.getAttachment("file");

            assertNotNull(newAttachment);
            assertEquals(9, newAttachment.getLongSize());
            assertArrayEquals(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 }, newAttachment.getContent(null));
        }

        // #apply

        @Test
        public void testApplyWithUnmodifiedObject()
        {
            this.previousDocument.addXObject(this.xobject);
            this.currentDocument.addXObject(this.xobject.clone());

            assertFalse(this.previousDocument.apply(this.currentDocument, true));
        }

        @Test
        public void testApplyWithModifiedObjectAndClean()
        {
            this.previousDocument.addXObject(this.xobject);
            BaseObject modifiedObject = this.xobject.clone();
            modifiedObject.setStringValue("string", "string2");
            this.currentDocument.addXObject(modifiedObject);

            assertTrue(this.previousDocument.apply(this.currentDocument, true));
            assertEquals("string2", this.xobject.getStringValue("string"));
        }

        @Test
        public void testMergeWithAddedSameObject() throws Exception
        {
            this.currentDocument.addXObject(this.xobject);
            this.nextDocument.addXObject(this.xobject.clone());

            MergeDocumentResult result = merge();
            assertFalse(result.isModified());
            assertTrue(result.getLog().getLogs(LogLevel.ERROR).isEmpty());
        }

        @Test
        public void testMergeWithAddedSameProperty() throws Exception
        {
            this.previousDocument.addXObject(xobject);
            BaseObject xobj = this.xobject.clone();
            xobj.setStringValue("another prop", "foo");
            this.currentDocument.addXObject(xobj);
            this.nextDocument.addXObject(xobj.clone());

            MergeDocumentResult result = merge();
            assertFalse(result.isModified());
            assertTrue(result.getLog().getLogs(LogLevel.ERROR).isEmpty());
        }
    }
}
