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
package org.xwiki.search.solr.internal.metadata;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import junit.framework.Assert;

import org.apache.solr.common.SolrInputDocument;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.search.solr.internal.api.Fields;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceValueProvider;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Tests for document metadata extraction.
 * 
 * @version $Id$
 */
@ComponentList({DefaultStringEntityReferenceSerializer.class, LocalStringEntityReferenceSerializer.class,
RelativeStringEntityReferenceResolver.class, CurrentReferenceDocumentReferenceResolver.class,
CurrentReferenceEntityReferenceResolver.class, CurrentEntityReferenceValueProvider.class})
public class DocumentSolrMetadataExtractorTest
{
    @Rule
    public final MockitoComponentMockingRule<SolrMetadataExtractor> mocker = new MockitoComponentMockingRule(
        DocumentSolrMetadataExtractor.class, SolrMetadataExtractor.class, "document", Arrays.asList(
            EntityReferenceSerializer.class, DocumentReferenceResolver.TYPE_REFERENCE,
            EntityReferenceResolver.TYPE_REFERENCE, EntityReferenceValueProvider.class));

    private XWikiContext mockContext;

    private XWikiDocument mockDocument;

    private XWiki mockXWiki;

    private DocumentReference documentReference;

    private String documentReferenceString;

    private String documentReferenceLocalString;

    private String language;

    private String renderedContent;

    private String title;

    private String version;

    private boolean hidden;

    private Date date;

    private Date creationDate;

    private DocumentReference authorReference;

    private String authorString;

    private String authorDisplay;

    private DocumentReference creatorReference;

    private String creatorString;

    private String creatorDisplay;

    @Before
    public void setUp() throws Exception
    {
        EntityReferenceSerializer<String> localSerializer =
            mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        EntityReferenceSerializer<String> serializer =
            mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "default");

        // No locale provided.
        documentReference = new DocumentReference("wiki", "space", "name");
        documentReferenceString = serializer.serialize(documentReference);
        documentReferenceLocalString = localSerializer.serialize(documentReference);

        language = "en";
        renderedContent = "content";
        title = "title";
        version = "1.1";
        hidden = false;
        date = new Date();
        creationDate = new Date();

        authorReference = new DocumentReference("wiki", "space", "author");
        authorString = serializer.serialize(authorReference);
        authorDisplay = "Au Thor";

        creatorReference = new DocumentReference("wiki", "space", "Creator");
        creatorString = serializer.serialize(creatorReference);
        creatorDisplay = "Crea Tor";

        // Mock

        mockContext = mock(XWikiContext.class);
        Execution mockExecution = mocker.getInstance(Execution.class);
        ExecutionContext mockExecutionContext = new ExecutionContext();
        mockExecutionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, mockContext);

        when(mockExecution.getContext()).thenReturn(mockExecutionContext);

        mockXWiki = mock(XWiki.class);
        mockDocument = mock(XWikiDocument.class);

        when(mockContext.getWiki()).thenReturn(mockXWiki);
        when(mockXWiki.getDocument(documentReference, mockContext)).thenReturn(mockDocument);

        when(mockDocument.getRealLanguage()).thenReturn(language);
        when(mockDocument.getTranslatedDocument(any(String.class), eq(mockContext))).thenReturn(mockDocument);

        DocumentAccessBridge mockDab = mocker.getInstance(DocumentAccessBridge.class);
        when(mockDab.getDocument(documentReference)).thenReturn(mockDocument);

        BlockRenderer mockPlainRenderer = mocker.getInstance(BlockRenderer.class, "plain/1.0");
        doAnswer(new Answer<Object>()
        {
            public Object answer(InvocationOnMock invocation)
            {
                Object[] args = invocation.getArguments();

                WikiPrinter printer = (WikiPrinter) args[1];
                printer.print(renderedContent);

                return null;
            }
        }).when(mockPlainRenderer).render(any(Block.class), any(WikiPrinter.class));

        when(mockDocument.getRenderedTitle(any(Syntax.class), eq(mockContext))).thenReturn(title);

        when(mockDocument.getVersion()).thenReturn(version);

        when(mockDocument.getAuthorReference()).thenReturn(authorReference);
        when(mockXWiki.getUserName(authorString, null, false, mockContext)).thenReturn(authorDisplay);

        when(mockDocument.getCreatorReference()).thenReturn(creatorReference);
        when(mockXWiki.getUserName(creatorString, null, false, mockContext)).thenReturn(creatorDisplay);

        when(mockDocument.getCreationDate()).thenReturn(creationDate);
        when(mockDocument.getContentUpdateDate()).thenReturn(date);

        when(mockDocument.isHidden()).thenReturn(hidden);
    }

    @Test
    public void testGetIdLanguageInLocale() throws Exception
    {
        DocumentSolrMetadataExtractor extractor = (DocumentSolrMetadataExtractor) mocker.getComponentUnderTest();

        // Locale provided in the reference
        DocumentReference reference = new DocumentReference("wiki", "space", "name", new Locale("en"));

        // Call
        String id = extractor.getId(reference);

        // Assert
        Assert.assertEquals("wiki:space.name_en", id);
    }

    @Test
    public void testGetIdLanguageInDatabase() throws Exception
    {
        DocumentSolrMetadataExtractor extractor = (DocumentSolrMetadataExtractor) mocker.getComponentUnderTest();

        // No locale provided.
        DocumentReference reference = new DocumentReference("wiki", "space", "name");

        // Mock

        XWikiDocument mockDocument = mock(XWikiDocument.class);
        when(mockDocument.getRealLanguage()).thenReturn("en");

        DocumentAccessBridge mockDab = mocker.getInstance(DocumentAccessBridge.class);
        when(mockDab.getDocument(reference)).thenReturn(mockDocument);

        // Call
        String id = extractor.getId(reference);

        // Assert and verify
        Assert.assertEquals("wiki:space.name_en", id);
        verify(mockDab, atMost(2)).getDocument(reference);
        verify(mockDocument, atMost(2)).getRealLanguage();
    }

    @Test
    public void testGetIdLanguageNotAvailable() throws Exception
    {
        DocumentSolrMetadataExtractor extractor = (DocumentSolrMetadataExtractor) mocker.getComponentUnderTest();

        // No locale provided.
        DocumentReference reference = new DocumentReference("wiki", "space", "name");

        // Mock

        XWikiDocument mockDocument = mock(XWikiDocument.class);
        when(mockDocument.getRealLanguage()).thenReturn("");

        DocumentAccessBridge mockDab = mocker.getInstance(DocumentAccessBridge.class);
        when(mockDab.getDocument(reference)).thenReturn(mockDocument);

        // Call
        String id = extractor.getId(reference);

        // Assert and verify
        Assert.assertEquals("wiki:space.name_en", id);
        verify(mockDab, times(1)).getDocument(reference);
        verify(mockDocument, times(1)).getRealLanguage();
    }

    @Test
    public void testGetSimpleDocument() throws Exception
    {
        // Mock

        // No objects (and no comments).
        when(mockDocument.getComments()).thenReturn(new Vector<BaseObject>());
        when(mockDocument.getXObjects()).thenReturn(new HashMap<DocumentReference, List<BaseObject>>());

        // Call

        DocumentSolrMetadataExtractor extractor = (DocumentSolrMetadataExtractor) mocker.getComponentUnderTest();
        SolrInputDocument solrDocument = extractor.getSolrDocument(documentReference);

        // Assert and verify

        Assert.assertEquals(String.format("%s_%s", documentReferenceString, language),
            solrDocument.getFieldValue(Fields.ID));

        Assert.assertEquals(documentReference.getWikiReference().getName(), solrDocument.getFieldValue(Fields.WIKI));
        Assert.assertEquals(documentReference.getLastSpaceReference().getName(),
            solrDocument.getFieldValue(Fields.SPACE));
        Assert.assertEquals(documentReference.getName(), solrDocument.getFieldValue(Fields.NAME));

        Assert.assertEquals(language, solrDocument.getFieldValue(Fields.LANGUAGE));
        Assert.assertEquals(hidden, solrDocument.getFieldValue(Fields.HIDDEN));
        Assert.assertEquals(EntityType.DOCUMENT.name(), solrDocument.getFieldValue(Fields.TYPE));

        Assert.assertEquals(documentReferenceLocalString, solrDocument.getFieldValue(Fields.FULLNAME));

        Assert.assertEquals(title,
            solrDocument.getFieldValue(String.format(Fields.MULTILIGNUAL_FORMAT, Fields.TITLE, language)));
        Assert.assertEquals(renderedContent,
            solrDocument.getFieldValue(String.format(Fields.MULTILIGNUAL_FORMAT, Fields.DOCUMENT_CONTENT, language)));

        Assert.assertEquals(version, solrDocument.getFieldValue(Fields.VERSION));

        Assert.assertEquals(authorString, solrDocument.getFieldValue(Fields.AUTHOR));
        Assert.assertEquals(authorDisplay, solrDocument.getFieldValue(Fields.AUTHOR_DISPLAY));
        Assert.assertEquals(creatorString, solrDocument.getFieldValue(Fields.CREATOR));
        Assert.assertEquals(creatorDisplay, solrDocument.getFieldValue(Fields.CREATOR_DISPLAY));

        Assert.assertEquals(creationDate, solrDocument.getFieldValue(Fields.CREATIONDATE));
        Assert.assertEquals(date, solrDocument.get(Fields.DATE).getValue());
    }

    @Test
    public void testGetDocumentWithObjects() throws Exception
    {
        DocumentReference commentsClassReference = new DocumentReference("wiki", "space", "commentsClass");
        String commentContent = "This is a comment";
        String commentAuthor = "wiki:space.commentAuthor";
        Date commentDate = new Date();
        // Adding a fake password field to the comments class just to test the branch in the code.
        String commentPassword = "password";
        List<String> commentList = Arrays.asList("a", "list");

        List<BaseProperty<EntityReference>> commentFields = new ArrayList<BaseProperty<EntityReference>>();

        // Mock

        BaseProperty<EntityReference> mockCommentField = mock(BaseProperty.class);
        when(mockCommentField.getName()).thenReturn("comment");
        when(mockCommentField.getValue()).thenReturn(commentContent);
        commentFields.add(mockCommentField);

        BaseProperty<EntityReference> mockAuthorField = mock(BaseProperty.class);
        when(mockAuthorField.getName()).thenReturn("author");
        when(mockAuthorField.getValue()).thenReturn(commentAuthor);
        commentFields.add(mockAuthorField);

        BaseProperty<EntityReference> mockDateField = mock(BaseProperty.class);
        when(mockDateField.getName()).thenReturn("date");
        when(mockDateField.getValue()).thenReturn(commentDate);
        commentFields.add(mockDateField);

        BaseProperty<EntityReference> mockPasswordField = mock(BaseProperty.class);
        when(mockPasswordField.getName()).thenReturn("password");
        when(mockPasswordField.getValue()).thenReturn(commentPassword);
        commentFields.add(mockPasswordField);

        BaseProperty<EntityReference> mockListField = mock(BaseProperty.class);
        when(mockListField.getName()).thenReturn("list");
        when(mockListField.getValue()).thenReturn(commentList);
        commentFields.add(mockListField);

        BaseClass mockXClass = mock(BaseClass.class);

        BaseObject mockComment = mock(BaseObject.class);

        // When handled as a comment
        Vector<BaseObject> comments = new Vector<BaseObject>();
        comments.add(mockComment);
        when(mockDocument.getComments()).thenReturn(comments);

        when(mockComment.getStringValue("comment")).thenReturn(commentContent);
        when(mockComment.getStringValue("author")).thenReturn(commentAuthor);
        when(mockComment.getDateValue("date")).thenReturn(commentDate);

        // When handled as a general object
        HashMap<DocumentReference, List<BaseObject>> xObjects = new HashMap<DocumentReference, List<BaseObject>>();
        xObjects.put(commentsClassReference, Arrays.asList(mockComment));
        when(mockDocument.getXObjects()).thenReturn(xObjects);

        when(mockComment.getXClass(mockContext)).thenReturn(mockXClass);
        when(mockComment.getFieldList()).thenReturn(commentFields);

        PropertyClass passwordClass = mock(PasswordClass.class);
        when(mockXClass.get("password")).thenReturn(passwordClass);
        when(passwordClass.getClassType()).thenReturn("Password");

        // Call

        DocumentSolrMetadataExtractor extractor = (DocumentSolrMetadataExtractor) mocker.getComponentUnderTest();
        SolrInputDocument solrDocument = extractor.getSolrDocument(documentReference);

        // Assert and verify

        Assert.assertEquals(String.format("%s by %s on %s", commentContent, commentAuthor, commentDate),
            solrDocument.getFieldValue(String.format(Fields.MULTILIGNUAL_FORMAT, Fields.COMMENT, language)));

        Collection<Object> objectProperties =
            solrDocument.getFieldValues(String.format(Fields.MULTILIGNUAL_FORMAT, Fields.OBJECT_CONTENT, language));
        MatcherAssert.assertThat(objectProperties, Matchers.containsInAnyOrder((Object) ("comment:" + commentContent),
            (Object) ("author:" + commentAuthor), (Object) ("date:" + commentDate.toString()),
            (Object) ("list:" + commentList.get(0)), (Object) ("list:" + commentList.get(1))));
        Assert.assertEquals(5, objectProperties.size());
    }
}
