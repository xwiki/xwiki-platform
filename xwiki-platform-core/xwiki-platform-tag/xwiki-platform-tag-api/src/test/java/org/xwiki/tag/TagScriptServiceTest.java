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
package org.xwiki.tag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.tag.internal.TagDocumentManager;
import org.xwiki.tag.internal.TagQueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import ch.qos.logback.classic.Level;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Test of {@link TagScriptService}.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@ComponentTest
class TagScriptServiceTest
{
    @InjectMockComponents
    private TagScriptService tagScriptService;

    @MockComponent
    private TagQueryManager tagQueryManager;

    @MockComponent
    private TagDocumentManager tagDocumentManager;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorization;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void getTagCount() throws Exception
    {
        Map<String, Integer> expected = new HashMap<>();
        SpaceReference space = new SpaceReference("wikiTest", "testSpace");

        when(this.entityReferenceSerializer.serialize(space)).thenReturn("xwikiTest:testSpace");
        when(this.tagQueryManager.getTagCountForQuery("", "(doc.space = ?1 OR doc.space LIKE ?2)",
            asList("xwikiTest:testSpace", "xwikiTest:testSpace.%"))).thenReturn(expected);

        Map<String, Integer> actual = this.tagScriptService.getTagCount(space);

        assertSame(expected, actual);
    }

    @Test
    void getTagCountNull() throws Exception
    {
        Map<String, Integer> expected = new HashMap<>();
        when(this.tagQueryManager.getTagCountForQuery(null, null, (Map<String, ?>) null)).thenReturn(expected);
        Map<String, Integer> actual = this.tagScriptService.getTagCount(null);
        assertSame(expected, actual);
    }

    @Test
    void getTagCountForSpaces() throws Exception
    {
        Map<String, Integer> expected = new HashMap<>();
        SpaceReference s1 = new SpaceReference("wikiTest", "S1");
        SpaceReference s2 = new SpaceReference("wikiTest", "S2");

        when(this.tagQueryManager
            .getTagCountForQuery("", "(doc.space = ?1 OR doc.space LIKE ?2 OR doc.space = ?3 OR doc.space LIKE ?4)",
                asList(
                    "wikiTest:S1",
                    "wikiTest:S1.%",
                    "wikiTest:S2",
                    "wikiTest:S2.%"
                ))).thenReturn(expected);
        when(this.entityReferenceSerializer.serialize(s1)).thenReturn("wikiTest:S1");
        when(this.entityReferenceSerializer.serialize(s2)).thenReturn("wikiTest:S2");

        Map<String, Integer> actual = this.tagScriptService.getTagCountForSpaces(asList(s1, s2));

        assertSame(expected, actual);
    }

    @Test
    void getTagCountForSpacesSingleElement() throws Exception
    {
        Map<String, Integer> expected = new HashMap<>();
        SpaceReference s1 = new SpaceReference("wikiTest", "S1");

        when(this.tagQueryManager
            .getTagCountForQuery("", "(doc.space = ?1 OR doc.space LIKE ?2)",
                asList(
                    "wikiTest:S1",
                    "wikiTest:S1.%"
                ))).thenReturn(expected);
        when(this.entityReferenceSerializer.serialize(s1)).thenReturn("wikiTest:S1");

        Map<String, Integer> actual = this.tagScriptService.getTagCountForSpaces(asList(s1));

        assertSame(expected, actual);
    }

    @Test
    void getTagCountForSpacesNull() throws Exception
    {
        Map<String, Integer> expected = new HashMap<>();
        when(this.tagQueryManager.getTagCountForQuery("", "", asList())).thenReturn(expected);
        Map<String, Integer> actual = this.tagScriptService.getTagCountForSpaces(null);
        assertSame(expected, actual);
    }

    @Test
    void getTagCountForSpacesEmpty() throws Exception
    {
        Map<String, Integer> expected = new HashMap<>();
        when(this.tagQueryManager.getTagCountForQuery("", "", asList())).thenReturn(expected);
        Map<String, Integer> actual = this.tagScriptService.getTagCountForSpaces(asList());
        assertSame(expected, actual);
    }

    @Test
    void addTagsToDocumentNotAllowed()
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        when(this.contextualAuthorization.hasAccess(Right.EDIT, documentReference)).thenReturn(false);
        TagOperationResult tagOperationResult =
            this.tagScriptService.addTagsToDocument(asList("t1", "t2"), documentReference);
        assertEquals(TagOperationResult.NOT_ALLOWED, tagOperationResult);
    }

    @Test
    void addTagsToDocumentFails() throws TagException
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        List<String> tags = asList("t1", "t2");

        when(this.contextualAuthorization.hasAccess(Right.EDIT, documentReference)).thenReturn(true);
        when(this.tagDocumentManager.addTagsToDocument(tags, documentReference)).thenThrow(TagException.class);

        TagOperationResult tagOperationResult =
            this.tagScriptService.addTagsToDocument(tags, documentReference);
        assertEquals(TagOperationResult.FAILED, tagOperationResult);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Failed to add tag [[t1, t2]] to document [xwiki:XWiki.Doc]. Cause: [TagException: ].",
            this.logCapture.getMessage(0));
    }

    @Test
    void addTagsToDocument() throws TagException
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        List<String> tags = asList("t1", "t2");

        when(this.contextualAuthorization.hasAccess(Right.EDIT, documentReference)).thenReturn(true);
        when(this.tagDocumentManager.addTagsToDocument(tags, documentReference)).thenReturn(TagOperationResult.OK);

        TagOperationResult tagOperationResult = this.tagScriptService.addTagsToDocument(tags, documentReference);
        assertEquals(TagOperationResult.OK, tagOperationResult);
    }

    @Test
    void removeTagFromDocumentNotAllowed()
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        when(this.contextualAuthorization.hasAccess(Right.EDIT, documentReference)).thenReturn(false);
        TagOperationResult tagOperationResult = this.tagScriptService.removeTagFromDocument("t1", documentReference);
        assertEquals(TagOperationResult.NOT_ALLOWED, tagOperationResult);
    }

    @Test
    void removeTagFromDocumentFails() throws TagException
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        when(this.contextualAuthorization.hasAccess(Right.EDIT, documentReference)).thenReturn(true);
        when(this.tagDocumentManager.removeTagFromDocument("t1", documentReference)).thenThrow(TagException.class);
        TagOperationResult tagOperationResult = this.tagScriptService.removeTagFromDocument("t1", documentReference);
        assertEquals(TagOperationResult.FAILED, tagOperationResult);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Failed to remove tag [t1] to document [xwiki:XWiki.Doc]. Cause: [TagException: ].",
            this.logCapture.getMessage(0));
    }

    @Test
    void removeTagFromDocument() throws TagException
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        when(this.contextualAuthorization.hasAccess(Right.EDIT, documentReference)).thenReturn(true);
        when(this.tagDocumentManager.removeTagFromDocument("t1", documentReference))
            .thenReturn(TagOperationResult.NO_EFFECT);
        TagOperationResult tagOperationResult = this.tagScriptService.removeTagFromDocument("t1", documentReference);
        assertEquals(TagOperationResult.NO_EFFECT, tagOperationResult);
    }

    @Test
    void renameTagNotAllowed()
    {
        when(this.contextualAuthorization.hasAccess(Right.ADMIN)).thenReturn(false);
        TagOperationResult tagOperationResult = this.tagScriptService.renameTag("t1", "t2");
        assertEquals(TagOperationResult.NOT_ALLOWED, tagOperationResult);
    }

    @Test
    void renameTagFails() throws TagException
    {
        when(this.contextualAuthorization.hasAccess(Right.ADMIN)).thenReturn(true);
        when(this.tagDocumentManager.renameTag("t1", "t2")).thenThrow(TagException.class);
        TagOperationResult tagOperationResult = this.tagScriptService.renameTag("t1", "t2");
        assertEquals(TagOperationResult.FAILED, tagOperationResult);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Failed to rename tag [t1] to [t2]. Cause: [TagException: ].",
            this.logCapture.getMessage(0));
    }

    @Test
    void renameTag() throws TagException
    {
        when(this.contextualAuthorization.hasAccess(Right.ADMIN)).thenReturn(true);
        when(this.tagDocumentManager.renameTag("t1", "t2")).thenReturn(TagOperationResult.OK);
        TagOperationResult tagOperationResult = this.tagScriptService.renameTag("t1", "t2");
        assertEquals(TagOperationResult.OK, tagOperationResult);
    }

    @Test
    void deleteTagNotAllowed()
    {
        when(this.contextualAuthorization.hasAccess(Right.ADMIN)).thenReturn(false);
        TagOperationResult tagOperationResult = this.tagScriptService.deleteTag("t1");
        assertEquals(TagOperationResult.NOT_ALLOWED, tagOperationResult);
    }

    @Test
    void deleteTagFails() throws TagException
    {
        when(this.contextualAuthorization.hasAccess(Right.ADMIN)).thenReturn(true);
        when(this.tagDocumentManager.deleteTag("t1")).thenThrow(TagException.class);
        TagOperationResult tagOperationResult = this.tagScriptService.deleteTag("t1");
        assertEquals(TagOperationResult.FAILED, tagOperationResult);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Failed to remove tag [t1]. Cause: [TagException: ].",
            this.logCapture.getMessage(0));
    }

    @Test
    void deleteTag() throws TagException
    {
        when(this.contextualAuthorization.hasAccess(Right.ADMIN)).thenReturn(true);
        when(this.tagDocumentManager.deleteTag("t1")).thenReturn(TagOperationResult.OK);
        TagOperationResult tagOperationResult = this.tagScriptService.deleteTag("t1");
        assertEquals(TagOperationResult.OK, tagOperationResult);
    }
}