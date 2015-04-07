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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Provider;

import org.apache.solr.common.SolrInputDocument;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrFieldNameEncoder;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Unit tests for document meta data extraction.
 * 
 * @version $Id$
 */
public class DocumentSolrMetadataExtractorTest
{
    @Rule
    public final MockitoComponentMockingRule<SolrMetadataExtractor> mocker =
        new MockitoComponentMockingRule<SolrMetadataExtractor>(DocumentSolrMetadataExtractor.class);

    private XWikiContext xcontext;

    /**
     * The document from which we extract the meta data.
     */
    private XWikiDocument document;

    private DocumentReference documentReference = new DocumentReference("wiki", "Space", "Name");

    @Before
    public void setUp() throws Exception
    {
        this.mocker.registerMockComponent(SolrReferenceResolver.class, "document");
        this.xcontext = mock(XWikiContext.class);

        // XWikiContext Provider
        Provider<XWikiContext> xcontextProvider = this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);

        // XWikiContext trough Execution
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.xcontext);
        Execution execution = this.mocker.registerMockComponent(Execution.class);
        when(execution.getContext()).thenReturn(executionContext);

        // XWiki
        XWiki wiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(wiki);

        // XWikiDocument
        this.document = mock(XWikiDocument.class);
        when(wiki.getDocument(this.documentReference, this.xcontext)).thenReturn(this.document);
        when(this.document.getDocumentReference()).thenReturn(this.documentReference);
        when(this.document.isHidden()).thenReturn(false);
        when(this.document.getLocale()).thenReturn(Locale.ROOT);
        when(this.document.getRealLocale()).thenReturn(Locale.US);
        when(this.document.getTranslatedDocument(isNull(Locale.class), eq(this.xcontext))).thenReturn(this.document);

        DocumentAccessBridge dab = this.mocker.registerMockComponent(DocumentAccessBridge.class);
        when(dab.getDocument(this.documentReference)).thenReturn(this.document);

        // Field Name Serializer
        EntityReferenceSerializer<String> fieldNameSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "solr");
        when(fieldNameSerializer.serialize(any(EntityReference.class))).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                EntityReference reference = (EntityReference) invocation.getArguments()[0];
                StringBuilder result = new StringBuilder();
                for (EntityReference parent : reference.getReversedReferenceChain()) {
                    result.append('.').append(parent.getName());
                }
                return result.substring(1);
            }
        });

        // Field Name Encoder
        SolrFieldNameEncoder fieldNameEncoder = this.mocker.getInstance(SolrFieldNameEncoder.class);
        when(fieldNameEncoder.encode(anyString())).then(AdditionalAnswers.returnsFirstArg());
    }

    @Test
    public void getSimpleDocument() throws Exception
    {
        //
        // Mock
        //

        // ID
        String id = "wiki:Space.Name_" + Locale.ROOT.toString();
        SolrReferenceResolver documentSolrReferenceResolver =
            this.mocker.getInstance(SolrReferenceResolver.class, "document");
        when(documentSolrReferenceResolver.getId(documentReference)).thenReturn(id);

        // Full Name
        String fullName = "Space.Name";
        EntityReferenceSerializer<String> localEntityReferenceSerializer =
            this.mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localEntityReferenceSerializer.serialize(this.documentReference)).thenReturn(fullName);

        // Creator.
        DocumentReference creatorReference = new DocumentReference("wiki", "Space", "Creator");
        when(this.document.getCreatorReference()).thenReturn(creatorReference);

        String creatorStringReference = "wiki:Space.Creator";
        EntityReferenceSerializer<String> entityReferenceSerializer =
            this.mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "default");
        when(entityReferenceSerializer.serialize(creatorReference)).thenReturn(creatorStringReference);

        String creatorDisplayName = "Crea Tor";
        when(this.xcontext.getWiki().getPlainUserName(creatorReference, this.xcontext)).thenReturn(creatorDisplayName);

        // Author.
        DocumentReference authorReference = new DocumentReference("wiki", "Space", "Author");
        when(this.document.getAuthorReference()).thenReturn(authorReference);

        String authorStringReference = "wiki:Space.Author";
        when(entityReferenceSerializer.serialize(authorReference)).thenReturn(authorStringReference);

        String authorDisplayName = "Au Thor";
        when(this.xcontext.getWiki().getPlainUserName(authorReference, this.xcontext)).thenReturn(authorDisplayName);

        // Creation Date
        Date creationDate = new Date();
        when(this.document.getCreationDate()).thenReturn(creationDate);

        // Date
        Date date = new Date();
        when(this.document.getContentUpdateDate()).thenReturn(date);

        // Version
        String version = "1.1";
        when(this.document.getVersion()).thenReturn(version);

        // Version summary
        String comment = "1.1 comment";
        when(this.document.getComment()).thenReturn(comment);

        // XObjects.
        when(this.document.getXObjects()).thenReturn(Collections.<DocumentReference, List<BaseObject>> emptyMap());

        // Title
        String title = "title";
        when(this.document.getRenderedTitle(any(Syntax.class), eq(this.xcontext))).thenReturn(title);

        // Rendered Content
        final String renderedContent = "rendered content";
        BlockRenderer plainRenderer = this.mocker.registerMockComponent(BlockRenderer.class, "plain/1.0");
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation)
            {
                Object[] args = invocation.getArguments();

                WikiPrinter printer = (WikiPrinter) args[1];
                printer.print(renderedContent);

                return null;
            }
        }).when(plainRenderer).render(any(Block.class), any(WikiPrinter.class));

        // Raw Content
        String rawContent = "raw content";
        when(this.document.getContent()).thenReturn(rawContent);

        //
        // Call
        //
        SolrInputDocument solrDocument = this.mocker.getComponentUnderTest().getSolrDocument(this.documentReference);

        //
        // Assert and verify
        //
        assertEquals(id, solrDocument.getFieldValue(FieldUtils.ID));

        assertEquals(this.documentReference.getWikiReference().getName(), solrDocument.getFieldValue(FieldUtils.WIKI));
        assertEquals(this.documentReference.getLastSpaceReference().getName(),
            solrDocument.getFieldValue(FieldUtils.SPACE));
        assertEquals(this.documentReference.getName(), solrDocument.getFieldValue(FieldUtils.NAME));

        assertEquals(Locale.US.toString(), solrDocument.getFieldValue(FieldUtils.LOCALE));
        assertEquals(Locale.US.getLanguage(), solrDocument.getFieldValue(FieldUtils.LANGUAGE));
        Collection< ? > actualLocales = solrDocument.getFieldValues(FieldUtils.LOCALES);
        // The order of the locales in the returned collection is nondeterministic.
        assertTrue(actualLocales.size() == 2 && actualLocales.contains("")
            && actualLocales.contains(Locale.US.toString()));
        assertEquals(this.document.isHidden(), solrDocument.getFieldValue(FieldUtils.HIDDEN));
        assertEquals(EntityType.DOCUMENT.name(), solrDocument.getFieldValue(FieldUtils.TYPE));

        assertEquals(fullName, solrDocument.getFieldValue(FieldUtils.FULLNAME));

        assertEquals(title, solrDocument.getFieldValue(FieldUtils.getFieldName(FieldUtils.TITLE, Locale.US)));
        assertEquals(rawContent,
            solrDocument.getFieldValue(FieldUtils.getFieldName(FieldUtils.DOCUMENT_RAW_CONTENT, Locale.US)));
        assertEquals(renderedContent,
            solrDocument.getFieldValue(FieldUtils.getFieldName(FieldUtils.DOCUMENT_RENDERED_CONTENT, Locale.US)));

        assertEquals(version, solrDocument.getFieldValue(FieldUtils.VERSION));
        assertEquals(comment, solrDocument.getFieldValue(FieldUtils.COMMENT));

        assertEquals(authorStringReference, solrDocument.getFieldValue(FieldUtils.AUTHOR));
        assertEquals(authorDisplayName, solrDocument.getFieldValue(FieldUtils.AUTHOR_DISPLAY));
        assertEquals(creatorStringReference, solrDocument.getFieldValue(FieldUtils.CREATOR));
        assertEquals(creatorDisplayName, solrDocument.getFieldValue(FieldUtils.CREATOR_DISPLAY));

        assertEquals(creationDate, solrDocument.getFieldValue(FieldUtils.CREATIONDATE));
        assertEquals(date, solrDocument.get(FieldUtils.DATE).getValue());
    }

    @Test
    public void getDocumentThrowingException() throws Exception
    {
        DocumentReference frenchDocumentReference = new DocumentReference(this.documentReference, Locale.FRENCH);
        XWikiException thrown =
            new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC, "Unreadable document");
        when(this.xcontext.getWiki().getDocument(frenchDocumentReference, this.xcontext)).thenThrow(thrown);

        try {
            this.mocker.getComponentUnderTest().getSolrDocument(frenchDocumentReference);
            fail("An exception was expected.");
        } catch (SolrIndexerException ex) {
            assertEquals("Failed to get input Solr document for entity '" + frenchDocumentReference + "'",
                ex.getMessage());
            assertSame(thrown, ex.getCause());
        }
    }

    @Test
    public void getDocumentWithObjects() throws Exception
    {
        //
        // Mock
        //
        BaseObject comment = mock(BaseObject.class);
        List<BaseProperty<EntityReference>> commentFields = new ArrayList<BaseProperty<EntityReference>>();

        String commentContent = "This is a comment";
        BaseProperty<EntityReference> contentField = mock(BaseProperty.class);
        when(contentField.getName()).thenReturn("comment");
        when(contentField.getValue()).thenReturn(commentContent);
        when(contentField.getObject()).thenReturn(comment);
        commentFields.add(contentField);

        String commentSummary = "summary";
        BaseProperty<EntityReference> summaryField = mock(BaseProperty.class);
        when(summaryField.getName()).thenReturn("summary");
        when(summaryField.getValue()).thenReturn(commentSummary);
        when(summaryField.getObject()).thenReturn(comment);
        commentFields.add(summaryField);

        String commentAuthor = "wiki:space.commentAuthor";
        BaseProperty<EntityReference> authorField = mock(BaseProperty.class);
        when(authorField.getName()).thenReturn("author");
        when(authorField.getValue()).thenReturn(commentAuthor);
        when(authorField.getObject()).thenReturn(comment);
        commentFields.add(authorField);

        Date commentDate = new Date();
        BaseProperty<EntityReference> dateField = mock(BaseProperty.class);
        when(dateField.getName()).thenReturn("date");
        when(dateField.getValue()).thenReturn(commentDate);
        when(dateField.getObject()).thenReturn(comment);
        commentFields.add(dateField);

        // Adding a fake password field to the comments class just to test the branch in the code.
        String commentPassword = "password";
        BaseProperty<EntityReference> passwordField = mock(BaseProperty.class);
        when(passwordField.getName()).thenReturn("password");
        when(passwordField.getValue()).thenReturn(commentPassword);
        commentFields.add(passwordField);

        List<String> commentList = Arrays.asList("a", "list");
        BaseProperty<EntityReference> listField = mock(BaseProperty.class);
        when(listField.getName()).thenReturn("list");
        when(listField.getValue()).thenReturn(commentList);
        when(listField.getObject()).thenReturn(comment);
        commentFields.add(listField);

        Long commentLikes = 13L;
        BaseProperty<EntityReference> numberField = mock(BaseProperty.class);
        when(numberField.getName()).thenReturn("likes");
        when(numberField.getValue()).thenReturn(commentLikes);
        when(numberField.getObject()).thenReturn(comment);
        commentFields.add(numberField);

        BaseProperty<EntityReference> booleanField = mock(BaseProperty.class);
        when(booleanField.getName()).thenReturn("enabled");
        when(booleanField.getValue()).thenReturn(1);
        when(booleanField.getObject()).thenReturn(comment);
        commentFields.add(booleanField);

        DocumentReference commentsClassReference = new DocumentReference("wiki", "space", "commentsClass");
        when(this.document.getXObjects()).thenReturn(
            Collections.singletonMap(commentsClassReference, Arrays.asList(comment)));

        EntityReferenceSerializer<String> localEntityReferenceSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localEntityReferenceSerializer.serialize(commentsClassReference)).thenReturn("space.commentsClass");

        BaseClass xclass = mock(BaseClass.class);
        when(comment.getXClass(this.xcontext)).thenReturn(xclass);
        when(comment.getFieldList()).thenReturn(commentFields);
        when(comment.getRelativeXClassReference()).thenReturn(
            commentsClassReference.removeParent(commentsClassReference.getWikiReference()));

        when(xclass.get("comment")).thenReturn(mock(TextAreaClass.class));
        when(xclass.get("summary")).thenReturn(mock(StringClass.class));
        when(xclass.get("password")).thenReturn(mock(PasswordClass.class));
        when(xclass.get("enabled")).thenReturn(mock(BooleanClass.class));

        //
        // Call
        //
        SolrInputDocument solrDocument = this.mocker.getComponentUnderTest().getSolrDocument(this.documentReference);

        //
        // Assert and verify
        //
        assertEquals(Arrays.asList("space.commentsClass"), solrDocument.getFieldValues(FieldUtils.CLASS));

        // A TextArea property must be indexed as a localized text.
        assertSame(commentContent,
            solrDocument.getFieldValue(FieldUtils.getFieldName("property.space.commentsClass.comment", Locale.US)));
        assertNull(solrDocument.getFieldValue("property.space.commentsClass.comment_string"));

        assertSame(commentSummary, solrDocument.getFieldValue("property.space.commentsClass.summary_string"));
        assertSame(commentAuthor, solrDocument.getFieldValue("property.space.commentsClass.author_string"));
        assertSame(commentDate, solrDocument.getFieldValue("property.space.commentsClass.date_date"));
        assertEquals(commentList, solrDocument.getFieldValues("property.space.commentsClass.list_string"));
        assertSame(commentLikes, solrDocument.getFieldValue("property.space.commentsClass.likes_long"));
        assertTrue((Boolean) solrDocument.getFieldValue("property.space.commentsClass.enabled_boolean"));

        // Make sure the password is not indexed (neither as a string nor as a localized text).
        assertNull(solrDocument.getFieldValue("property.space.commentsClass.password_string"));
        assertNull(solrDocument.getFieldValue(FieldUtils.getFieldName("property.space.commentsClass.password",
            Locale.US)));

        // Check the sort fields.
        assertSame(commentAuthor, solrDocument.getFieldValue("property.space.commentsClass.author_sortString"));
        assertSame(commentDate, solrDocument.getFieldValue("property.space.commentsClass.date_sortDate"));
        // The last value is used for sorting because we cannot sort on fields with multiple values.
        assertEquals("list", solrDocument.getFieldValue("property.space.commentsClass.list_sortString"));
        assertSame(commentLikes, solrDocument.getFieldValue("property.space.commentsClass.likes_sortLong"));
        assertTrue((Boolean) solrDocument.getFieldValue("property.space.commentsClass.enabled_sortBoolean"));

        // Localized texts are sorted as strings if they are not too large.
        assertSame(commentContent, solrDocument.getFieldValue("property.space.commentsClass.comment_sortString"));

        Collection<Object> objectProperties =
            solrDocument.getFieldValues(FieldUtils.getFieldName("object.space.commentsClass", Locale.US));
        MatcherAssert.assertThat(objectProperties, Matchers.<Object> containsInAnyOrder(commentContent, commentSummary,
            commentAuthor, commentDate, commentList.get(0), commentList.get(1), commentLikes, true));
        assertEquals(8, objectProperties.size());

        objectProperties =
            solrDocument.getFieldValues(FieldUtils.getFieldName(FieldUtils.OBJECT_CONTENT, Locale.US));
        MatcherAssert.assertThat(objectProperties, Matchers.<Object>containsInAnyOrder(
            "comment : " + commentContent, "summary : " + commentSummary,
            "author : " + commentAuthor,"date : " + commentDate, "list : " + commentList.get(0),
            "list : " + commentList.get(1), "likes : " + commentLikes, "enabled : true"));
        assertEquals(8, objectProperties.size());
    }

    /**
     * @see "XWIKI-9417: Search does not return any results for Static List values"
     */
    @Test
    public void setStaticListPropertyValue() throws Exception
    {
        BaseObject xobject = mock(BaseObject.class);

        @SuppressWarnings("unchecked")
        BaseProperty<EntityReference> listProperty = mock(BaseProperty.class);
        when(listProperty.getName()).thenReturn("color");
        when(listProperty.getValue()).thenReturn(Arrays.asList("red", "green"));
        when(listProperty.getObject()).thenReturn(xobject);

        DocumentReference classReference = new DocumentReference("wiki", "Space", "MyClass");
        when(this.document.getXObjects()).thenReturn(Collections.singletonMap(classReference, Arrays.asList(xobject)));

        BaseClass xclass = mock(BaseClass.class);
        when(xobject.getXClass(this.xcontext)).thenReturn(xclass);
        when(xobject.getFieldList()).thenReturn(Arrays.<Object> asList(listProperty));
        when(xobject.getRelativeXClassReference()).thenReturn(
            classReference.removeParent(classReference.getWikiReference()));

        StaticListClass staticListClass = mock(StaticListClass.class);
        when(xclass.get("color")).thenReturn(staticListClass);
        when(staticListClass.getMap(xcontext)).thenReturn(
            Collections.singletonMap("red", new ListItem("red", "Dark Red")));

        SolrInputDocument solrDocument = this.mocker.getComponentUnderTest().getSolrDocument(this.documentReference);

        // Make sure both the raw value (which is saved in the database) and the display value (specified in the XClass)
        // are indexed. The raw values are indexed as strings in order to be able to perform exact matches.
        assertEquals(Arrays.asList("red", "green"), solrDocument.getFieldValues("property.Space.MyClass.color_string"));
        assertEquals(Collections.singletonList("Dark Red"),
            solrDocument.getFieldValues(FieldUtils.getFieldName("property.Space.MyClass.color", Locale.US)));

        // Check the sort field. Only the last value we set is used for sorting because we cannot sort on fields that
        // have multiple values.
        assertEquals(Collections.singletonList("green"),
            solrDocument.getFieldValues("property.Space.MyClass.color_sortString"));
    }

    @Test
    public void getDocumentWithAttachments() throws Exception
    {
        Date logoDate = new Date(123);
        XWikiAttachment logo = createMockAttachment("logo.png", "image/png", logoDate, 13, "foo", "Alice", "Shy Alice");
        Date todoDate = new Date(456);
        XWikiAttachment todo = createMockAttachment("todo.txt", "text/plain", todoDate, 27, "bar", "Bob", "Angry Bob");
        when(this.document.getAttachmentList()).thenReturn(Arrays.<XWikiAttachment> asList(logo, todo));

        SolrInputDocument solrDocument = this.mocker.getComponentUnderTest().getSolrDocument(this.documentReference);

        assertEquals(Arrays.asList("logo.png", "todo.txt"), solrDocument.getFieldValues(FieldUtils.FILENAME));
        assertEquals(Arrays.asList("image/png", "text/plain"), solrDocument.getFieldValues(FieldUtils.MIME_TYPE));
        assertEquals(Arrays.asList(logoDate, todoDate), solrDocument.getFieldValues(FieldUtils.ATTACHMENT_DATE));
        assertEquals(Arrays.asList(13, 27), solrDocument.getFieldValues(FieldUtils.ATTACHMENT_SIZE));
        assertEquals(Arrays.asList("foo\n", "bar\n"), solrDocument.getFieldValues("attcontent_en_US"));
        assertEquals(Arrays.asList("wiki:XWiki.Alice", "wiki:XWiki.Bob"),
            solrDocument.getFieldValues(FieldUtils.ATTACHMENT_AUTHOR));
        assertEquals(Arrays.asList("Shy Alice", "Angry Bob"),
            solrDocument.getFieldValues(FieldUtils.ATTACHMENT_AUTHOR_DISPLAY));
    }

    private XWikiAttachment createMockAttachment(String fileName, String mimeType, Date date, int size, String content,
        String authorAlias, String authorDisplayName) throws Exception
    {
        XWikiAttachment attachment = mock(XWikiAttachment.class, fileName);
        when(attachment.getReference()).thenReturn(new AttachmentReference(fileName, this.documentReference));
        when(attachment.getFilename()).thenReturn(fileName);
        when(attachment.getMimeType(this.xcontext)).thenReturn(mimeType);
        when(attachment.getDate()).thenReturn(date);
        when(attachment.getFilesize()).thenReturn(size);
        when(attachment.getContentInputStream(this.xcontext)).thenReturn(new ByteArrayInputStream(content.getBytes()));

        String authorFullName = "XWiki." + authorAlias;
        DocumentReference authorReference = new DocumentReference("wiki", "XWiki", authorAlias);
        when(attachment.getAuthorReference()).thenReturn(authorReference);

        DocumentReferenceResolver<String> resolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);
        when(resolver.resolve(authorFullName, attachment.getReference())).thenReturn(authorReference);

        EntityReferenceSerializer<String> serializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        String authorStringReference = "wiki:" + authorFullName;
        when(serializer.serialize(authorReference)).thenReturn(authorStringReference);

        when(this.xcontext.getWiki().getPlainUserName(authorReference, this.xcontext)).thenReturn(authorDisplayName);

        return attachment;
    }
}
