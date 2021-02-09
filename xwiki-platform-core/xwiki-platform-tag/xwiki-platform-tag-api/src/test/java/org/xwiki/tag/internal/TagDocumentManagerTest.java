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
package org.xwiki.tag.internal;

import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.tag.TagOperationResult;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ListProperty;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.tag.internal.TagDocumentManager.TAGS_FIELD_NAME;

/**
 * Test of {@link TagDocumentManager}.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@ComponentTest
class TagDocumentManagerTest
{
    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "Document");

    @InjectMockComponents
    private TagDocumentManager tagDocumentManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private TagQueryManager tagQueryManager;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private XWiki xWiki;

    @Mock
    private XWikiDocument xWikiDocument;

    @BeforeEach
    void setUp()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xWikiContext);
        when(this.xWikiContext.getWikiId()).thenReturn("xwiki");
        when(this.xWikiContext.getWiki()).thenReturn(this.xWiki);
        when(this.xWikiContext.getUserReference()).thenReturn(AUTHOR_REFERENCE);
    }

    @Test
    void getTagsFromDocument() throws Exception
    {
        BaseObject tagObject = mock(BaseObject.class);
        ListProperty tagsProperty = mock(ListProperty.class);
        List<String> tags = asList("t1", "t2");

        when(this.xWiki.getDocument(DOCUMENT_REFERENCE, this.xWikiContext)).thenReturn(this.xWikiDocument);
        when(this.xWikiDocument
            .getXObject(new DocumentReference("xwiki", "XWiki", "TagClass"), false, this.xWikiContext))
            .thenReturn(tagObject);
        when(tagObject.get(TAGS_FIELD_NAME)).thenReturn(tagsProperty);
        when(tagsProperty.getValue()).thenReturn(tags);

        List<String> tagsFromDocument = this.tagDocumentManager.getTagsFromDocument(DOCUMENT_REFERENCE);

        assertEquals(tags, tagsFromDocument);
        // It is important that the returned list is not the same instance as the list of the tags field, otherwise we 
        // are at risk to see it modified (persistently) by the caller (e.g., a velocity template).
        assertNotSame(tags, tagsFromDocument);
    }

    @Test
    void addTagsToDocument() throws Exception
    {
        BaseObject tagObject = mock(BaseObject.class);
        ListProperty tagsProperty = mock(ListProperty.class);
        List<String> tags = asList("T1", "t3");

        when(this.xWiki.getDocument(DOCUMENT_REFERENCE, this.xWikiContext)).thenReturn(this.xWikiDocument);
        when(this.xWikiDocument
            .getXObject(eq(new DocumentReference("xwiki", "XWiki", "TagClass")), anyBoolean(), eq(this.xWikiContext)))
            .thenReturn(tagObject);
        when(tagObject.get(TAGS_FIELD_NAME)).thenReturn(tagsProperty);
        when(tagsProperty.getValue()).thenReturn(tags);
        when(this.contextualLocalizationManager.getTranslationPlain("plugin.tag.editcomment.added", "T1, t3, t2"))
            .thenReturn("commit message");

        TagOperationResult tagOperationResult =
            this.tagDocumentManager.addTagsToDocument(asList("t1", "t2"), DOCUMENT_REFERENCE);

        assertEquals(TagOperationResult.OK, tagOperationResult);
        verify(tagsProperty).setValue(asList("T1", "t3", "t2"));
        verify(this.xWikiDocument).setAuthorReference(AUTHOR_REFERENCE);
        verify(this.xWiki).saveDocument(this.xWikiDocument, "commit message", true, this.xWikiContext);
    }

    @Test
    void addTagsToDocumentNoEffect() throws Exception
    {
        BaseObject tagObject = mock(BaseObject.class);
        ListProperty tagsProperty = mock(ListProperty.class);
        List<String> tags = asList("T1", "t2");

        when(this.xWiki.getDocument(DOCUMENT_REFERENCE, this.xWikiContext)).thenReturn(this.xWikiDocument);
        when(this.xWikiDocument
            .getXObject(eq(new DocumentReference("xwiki", "XWiki", "TagClass")), anyBoolean(), eq(this.xWikiContext)))
            .thenReturn(tagObject);
        when(tagObject.get(TAGS_FIELD_NAME)).thenReturn(tagsProperty);
        when(tagsProperty.getValue()).thenReturn(tags);

        TagOperationResult tagOperationResult =
            this.tagDocumentManager.addTagsToDocument(asList("t1", "t2"), DOCUMENT_REFERENCE);

        assertEquals(TagOperationResult.NO_EFFECT, tagOperationResult);
        verify(tagsProperty, never()).setValue(any());
        verify(this.xWikiDocument, never()).setAuthorReference(any());
        verify(this.xWiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
    }

    @Test
    void removeTagFromDocument() throws Exception
    {
        BaseObject tagObject = mock(BaseObject.class);
        ListProperty tagsProperty = mock(ListProperty.class);
        List<String> tags = asList("T1", "t2");

        when(this.xWiki.getDocument(DOCUMENT_REFERENCE, this.xWikiContext)).thenReturn(this.xWikiDocument);
        when(this.xWikiDocument
            .getXObject(eq(new DocumentReference("xwiki", "XWiki", "TagClass")), anyBoolean(), eq(this.xWikiContext)))
            .thenReturn(tagObject);
        when(tagObject.get(TAGS_FIELD_NAME)).thenReturn(tagsProperty);
        when(tagsProperty.getValue()).thenReturn(tags);
        when(this.contextualLocalizationManager.getTranslationPlain("plugin.tag.editcomment.removed", "t1"))
            .thenReturn("commit message");

        TagOperationResult tagOperationResult = this.tagDocumentManager.removeTagFromDocument("t1", DOCUMENT_REFERENCE);
        assertEquals(TagOperationResult.OK, tagOperationResult);
        verify(tagsProperty).setValue(asList("t2"));
        verify(this.xWikiDocument).setAuthorReference(AUTHOR_REFERENCE);
        verify(this.xWiki).saveDocument(this.xWikiDocument, "commit message", true, this.xWikiContext);
    }

    @Test
    void removeTagFromDocumentNoEffect() throws Exception
    {
        BaseObject tagObject = mock(BaseObject.class);
        ListProperty tagsProperty = mock(ListProperty.class);
        List<String> tags = asList("T1", "t2");

        when(this.xWiki.getDocument(DOCUMENT_REFERENCE, this.xWikiContext)).thenReturn(this.xWikiDocument);
        when(this.xWikiDocument
            .getXObject(eq(new DocumentReference("xwiki", "XWiki", "TagClass")), anyBoolean(), eq(this.xWikiContext)))
            .thenReturn(tagObject);
        when(tagObject.get(TAGS_FIELD_NAME)).thenReturn(tagsProperty);
        when(tagsProperty.getValue()).thenReturn(tags);

        TagOperationResult tagOperationResult = this.tagDocumentManager.removeTagFromDocument("t3", DOCUMENT_REFERENCE);
        assertEquals(TagOperationResult.NO_EFFECT, tagOperationResult);
        verify(tagsProperty, never()).setValue(any());
        verify(this.xWikiDocument, never()).setAuthorReference(any());
        verify(this.xWiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
    }

    @Test
    void deleteTag() throws Exception
    {
        DocumentReference dr1 = new DocumentReference("xwiki", "XWiki", "d1");
        DocumentReference dr2 = new DocumentReference("xwiki", "XWiki", "d2");

        XWikiDocument xWikiDocument1 = mock(XWikiDocument.class);
        BaseObject tagObject1 = mock(BaseObject.class);
        ListProperty tagsProperty1 = mock(ListProperty.class);
        List<String> tags1 = asList("T1", "t2");

        XWikiDocument xWikiDocument2 = mock(XWikiDocument.class);
        BaseObject tagObject2 = mock(BaseObject.class);
        ListProperty tagsProperty2 = mock(ListProperty.class);
        List<String> tags2 = asList("T1", "t3");

        when(this.tagQueryManager.getDocumentsWithTag("t1", true)).thenReturn(asList("d1", "d2"));
        when(this.documentReferenceResolver.resolve("d1")).thenReturn(dr1);
        when(this.documentReferenceResolver.resolve("d2")).thenReturn(dr2);

        when(this.xWiki.getDocument(dr1, this.xWikiContext)).thenReturn(xWikiDocument1);
        when(xWikiDocument1
            .getXObject(eq(new DocumentReference("xwiki", "XWiki", "TagClass")), anyBoolean(), eq(this.xWikiContext)))
            .thenReturn(tagObject1);
        when(tagObject1.get(TAGS_FIELD_NAME)).thenReturn(tagsProperty1);
        when(tagsProperty1.getValue()).thenReturn(tags1);

        when(this.xWiki.getDocument(dr2, this.xWikiContext)).thenReturn(xWikiDocument2);
        when(xWikiDocument2
            .getXObject(eq(new DocumentReference("xwiki", "XWiki", "TagClass")), anyBoolean(), eq(this.xWikiContext)))
            .thenReturn(tagObject2);
        when(tagObject2.get(TAGS_FIELD_NAME)).thenReturn(tagsProperty2);
        when(tagsProperty2.getValue()).thenReturn(tags2);

        when(this.contextualLocalizationManager.getTranslationPlain("plugin.tag.editcomment.removed", "t1"))
            .thenReturn("commit message 1", "commit message 2");

        TagOperationResult tagOperationResult = this.tagDocumentManager.deleteTag("t1");
        assertEquals(TagOperationResult.OK, tagOperationResult);

        verify(tagsProperty1).setValue(asList("t2"));
        verify(xWikiDocument1).setAuthorReference(AUTHOR_REFERENCE);
        verify(this.xWiki).saveDocument(xWikiDocument1, "commit message 1", true, this.xWikiContext);

        verify(tagsProperty2).setValue(asList("t3"));
        verify(xWikiDocument2).setAuthorReference(AUTHOR_REFERENCE);
        verify(this.xWiki).saveDocument(xWikiDocument2, "commit message 2", true, this.xWikiContext);
    }

    @Test
    void deleteTagNoEffect() throws Exception
    {
        when(this.tagQueryManager.getDocumentsWithTag("t1", true)).thenReturn(emptyList());
        TagOperationResult tagOperationResult = this.tagDocumentManager.deleteTag("t1");
        assertEquals(TagOperationResult.NO_EFFECT, tagOperationResult);
        verify(this.xWiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
    }

    @Test
    void renameTag() throws Exception
    {
        DocumentReference dr1 = new DocumentReference("xwiki", "XWiki", "d1");
        DocumentReference dr2 = new DocumentReference("xwiki", "XWiki", "d2");

        XWikiDocument xWikiDocument1 = mock(XWikiDocument.class);
        BaseObject tagObject1 = mock(BaseObject.class);
        ListProperty tagsProperty1 = mock(ListProperty.class);
        List<String> tags1 = asList("T1", "t2");

        XWikiDocument xWikiDocument2 = mock(XWikiDocument.class);
        BaseObject tagObject2 = mock(BaseObject.class);
        ListProperty tagsProperty2 = mock(ListProperty.class);
        List<String> tags2 = asList("T1", "t3");

        when(this.tagQueryManager.getDocumentsWithTag("t1", true)).thenReturn(asList("d1", "d2"));
        when(this.documentReferenceResolver.resolve("d1")).thenReturn(dr1);
        when(this.documentReferenceResolver.resolve("d2")).thenReturn(dr2);

        when(this.xWiki.getDocument(dr1, this.xWikiContext)).thenReturn(xWikiDocument1);
        when(xWikiDocument1
            .getXObject(eq(new DocumentReference("xwiki", "XWiki", "TagClass")), anyBoolean(), eq(this.xWikiContext)))
            .thenReturn(tagObject1);
        when(tagObject1.get(TAGS_FIELD_NAME)).thenReturn(tagsProperty1);
        when(tagsProperty1.getValue()).thenReturn(tags1);

        when(this.xWiki.getDocument(dr2, this.xWikiContext)).thenReturn(xWikiDocument2);
        when(xWikiDocument2
            .getXObject(eq(new DocumentReference("xwiki", "XWiki", "TagClass")), anyBoolean(), eq(this.xWikiContext)))
            .thenReturn(tagObject2);
        when(tagObject2.get(TAGS_FIELD_NAME)).thenReturn(tagsProperty2);
        when(tagsProperty2.getValue()).thenReturn(tags2);

        when(this.contextualLocalizationManager.getTranslationPlain("plugin.tag.editcomment.removed", "t1"))
            .thenReturn("commit message 1");
        when(this.contextualLocalizationManager.getTranslationPlain("plugin.tag.editcomment.renamed", "t1", "T2"))
            .thenReturn("commit message 2");

        TagOperationResult tagOperationResult = this.tagDocumentManager.renameTag("t1", "T2");
        assertEquals(TagOperationResult.OK, tagOperationResult);

        verify(tagsProperty1).setValue(asList("t2"));
        verify(xWikiDocument1).setAuthorReference(AUTHOR_REFERENCE);
        verify(this.xWiki).saveDocument(xWikiDocument1, "commit message 1", true, this.xWikiContext);

        verify(tagsProperty2).setValue(asList("T2", "t3"));
        verify(xWikiDocument2).setAuthorReference(AUTHOR_REFERENCE);
        verify(this.xWiki).saveDocument(xWikiDocument2, "commit message 2", true, this.xWikiContext);
    }

    @Test
    void renameTagNoEffect() throws Exception
    {
        when(this.tagQueryManager.getDocumentsWithTag("t1", true)).thenReturn(emptyList());
        TagOperationResult tagOperationResult = this.tagDocumentManager.renameTag("t1", "t2");
        assertEquals(TagOperationResult.NO_EFFECT, tagOperationResult);
        verify(this.xWiki, never()).saveDocument(any(), anyString(), anyBoolean(), any());
    }
}
