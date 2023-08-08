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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrInputDocument;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.AdditionalAnswers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.model.EntityType;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.search.solr.internal.SolrSearchCoreUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrFieldNameEncoder;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.EmailClass;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for document meta data extraction.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList({SolrSearchCoreUtils.class, SolrLinkSerializer.class})
@ReferenceComponentList
class DocumentSolrMetadataExtractorTest
{
    @InjectMockComponents
    private DocumentSolrMetadataExtractor metadataExtractor;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private Execution execution;

    @MockComponent
    @Named("plain/1.0")
    private BlockRenderer renderer;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentReferenceUserReferenceSerializer;

    @MockComponent
    @Named("solr")
    private EntityReferenceSerializer<String> fieldNameSerializer;

    @MockComponent
    private SolrFieldNameEncoder fieldNameEncoder;

    @MockComponent
    private GeneralMailConfiguration mailConfiguration;

    @MockComponent
    @Named("document")
    private SolrReferenceResolver documentSolrReferenceResolver;

    private XWikiContext xcontext = mock(XWikiContext.class);

    /**
     * The document from which we extract the meta data.
     */
    private XWikiDocument document = mock(XWikiDocument.class);

    private XWikiDocument translatedDocument = mock(XWikiDocument.class, Locale.FRENCH.toString());

    private DocumentReference documentReference =
        new DocumentReference("wiki", Arrays.asList("Path", "To", "Page"), "WebHome");

    private DocumentReference frenchDocumentReference = new DocumentReference(this.documentReference, Locale.FRENCH);

    private DocumentAuthors documentAuthors = mock(DocumentAuthors.class);

    @BeforeEach
    void setUp() throws Exception
    {
        // XWikiContext Provider
        when(this.contextProvider.get()).thenReturn(this.xcontext);

        // XWikiContext trough Execution
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.xcontext);
        when(this.execution.getContext()).thenReturn(executionContext);

        // XWiki
        XWiki wiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(wiki);

        // XWikiDocument
        when(wiki.getDocument(this.documentReference, this.xcontext)).thenReturn(this.document);
        when(this.document.getDocumentReference()).thenReturn(this.documentReference);
        when(this.document.isHidden()).thenReturn(false);
        when(this.document.getLocale()).thenReturn(Locale.ROOT);
        when(this.document.getRealLocale()).thenReturn(Locale.US);
        when(this.document.getAuthors()).thenReturn(this.documentAuthors);

        when(this.document.getTranslatedDocument(Locale.FRENCH, this.xcontext)).thenReturn(this.translatedDocument);
        when(this.translatedDocument.getRealLocale()).thenReturn(Locale.FRENCH);
        when(this.translatedDocument.getLocale()).thenReturn(Locale.FRENCH);
        when(this.translatedDocument.getDocumentReference()).thenReturn(this.frenchDocumentReference);
        when(this.translatedDocument.getAuthors()).thenReturn(this.documentAuthors);

        when(this.fieldNameSerializer.serialize(any())).then(new Answer<String>()
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

        when(this.fieldNameEncoder.encode(any())).then(AdditionalAnswers.returnsFirstArg());
    }

    @Test
    void getSimpleDocument() throws Exception
    {
        // ID
        String id = "wiki:Space.Name_" + Locale.ROOT.toString();
        when(documentSolrReferenceResolver.getId(documentReference)).thenReturn(id);

        // Creator.
        UserReference creatorUserReference = mock(UserReference.class);
        when(this.documentAuthors.getCreator()).thenReturn(creatorUserReference);
        DocumentReference creatorReference = new DocumentReference("wiki", "Space", "Creator");
        when(this.documentReferenceUserReferenceSerializer.serialize(creatorUserReference))
            .thenReturn(creatorReference);

        String creatorStringReference = "wiki:Space.Creator";
        when(this.userReferenceSerializer.serialize(creatorUserReference)).thenReturn(creatorStringReference);

        String creatorDisplayName = "Crea Tor";
        when(this.xcontext.getWiki().getPlainUserName(creatorReference, this.xcontext)).thenReturn(creatorDisplayName);

        // Author.
        UserReference authorUserReference = mock(UserReference.class);
        when(this.documentAuthors.getOriginalMetadataAuthor()).thenReturn(authorUserReference);
        DocumentReference authorReference = new DocumentReference("wiki", "Space", "Author");
        when(this.documentReferenceUserReferenceSerializer.serialize(authorUserReference)).thenReturn(authorReference);

        String authorStringReference = "wiki:Space.Author";
        when(this.userReferenceSerializer.serialize(authorUserReference)).thenReturn(authorStringReference);

        String authorDisplayName = "Au Thor";
        when(this.xcontext.getWiki().getPlainUserName(authorReference, this.xcontext)).thenReturn(authorDisplayName);

        // Creation Date
        Date creationDate = new Date();
        when(this.document.getCreationDate()).thenReturn(creationDate);

        // Date
        Date date = new Date();
        when(this.document.getDate()).thenReturn(date);

        // Version
        String version = "1.1";
        when(this.document.getVersion()).thenReturn(version);

        // Version summary
        String comment = "1.1 comment";
        when(this.document.getComment()).thenReturn(comment);

        // Links
        when(this.document.getUniqueLinkedEntities(any(XWikiContext.class))).thenReturn(Set.of(
            new DocumentReference("wiki", "space", "document1"), new DocumentReference("wiki", "space", "document2")));

        // XObjects.
        when(this.document.getXObjects()).thenReturn(Collections.<DocumentReference, List<BaseObject>>emptyMap());

        // Title
        String title = "title";
        when(this.document.getRenderedTitle(any(), same(this.xcontext))).thenReturn(title);

        // Rendered Content
        final String renderedContent = "rendered content";
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation)
            {
                Object[] args = invocation.getArguments();

                WikiPrinter printer = (WikiPrinter) args[1];
                printer.print(renderedContent);

                return null;
            }
        }).when(this.renderer).render((Block) any(), any());

        // Raw Content
        String rawContent = "raw content";
        when(this.document.getContent()).thenReturn(rawContent);

        //
        // Call
        //
        SolrInputDocument solrDocument = this.metadataExtractor.getSolrDocument(this.documentReference);

        //
        // Assert and verify
        //
        assertEquals(id, solrDocument.getFieldValue(FieldUtils.ID));

        assertEquals(this.documentReference.getWikiReference().getName(), solrDocument.getFieldValue(FieldUtils.WIKI));
        assertEquals(Arrays.asList("Path", "To", "Page"), solrDocument.getFieldValues(FieldUtils.SPACES));
        assertEquals(this.documentReference.getName(), solrDocument.getFieldValue(FieldUtils.NAME));

        assertEquals(Arrays.asList("0/Path.", "1/Path.To.", "2/Path.To.Page."),
            solrDocument.getFieldValues(FieldUtils.SPACE_FACET));
        assertEquals(Arrays.asList("Path", "Path.To", "Path.To.Page"),
            solrDocument.getFieldValues(FieldUtils.SPACE_PREFIX));

        assertEquals(Locale.US.toString(), solrDocument.getFieldValue(FieldUtils.LOCALE));
        assertEquals(Locale.US.getLanguage(), solrDocument.getFieldValue(FieldUtils.LANGUAGE));
        Collection<?> actualLocales = solrDocument.getFieldValues(FieldUtils.LOCALES);
        // The order of the locales in the returned collection is nondeterministic.
        assertTrue(
            actualLocales.size() == 2 && actualLocales.contains("") && actualLocales.contains(Locale.US.toString()));
        assertEquals(this.document.isHidden(), solrDocument.getFieldValue(FieldUtils.HIDDEN));
        assertEquals(EntityType.DOCUMENT.name(), solrDocument.getFieldValue(FieldUtils.TYPE));

        assertEquals("Path.To.Page.WebHome", solrDocument.getFieldValue(FieldUtils.FULLNAME));

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

        assertEquals("document:wiki:Path.To.Page.WebHome", solrDocument.getFieldValue(FieldUtils.REFERENCE));

        assertEquals(Set.of("entity:document:wiki:space.document2", "entity:document:wiki:space.document1"),
            new HashSet<>(solrDocument.getFieldValues(FieldUtils.LINKS)));
        assertEquals(
            Set.of("entity:wiki:wiki", "entity:space:wiki:space", "entity:document:wiki:space.document2",
                "entity:document:wiki:space.document1"),
            new HashSet<>(solrDocument.getFieldValues(FieldUtils.LINKS_EXTENDED)));
    }

    @Test
    void getDocumentThrowingException() throws Exception
    {
        XWikiException thrown = new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC, "Unreadable document");
        when(this.xcontext.getWiki().getDocument(this.documentReference, this.xcontext)).thenThrow(thrown);

        SolrIndexerException solrIndexerException = assertThrows(SolrIndexerException.class,
            () -> this.metadataExtractor.getSolrDocument(this.frenchDocumentReference));
        assertEquals("Failed to get input Solr document for entity '" + this.frenchDocumentReference + "'",
            solrIndexerException.getMessage());
        assertSame(thrown, solrIndexerException.getCause());
    }

    @Test
    void getDocumentWithObjects() throws Exception
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
        when(this.document.getXObjects())
            .thenReturn(Collections.singletonMap(commentsClassReference, Arrays.asList(comment)));

        BaseClass xclass = mock(BaseClass.class);
        when(comment.getXClass(this.xcontext)).thenReturn(xclass);
        when(comment.getFieldList()).thenReturn(commentFields);
        when(comment.getRelativeXClassReference())
            .thenReturn(commentsClassReference.removeParent(commentsClassReference.getWikiReference()));

        StringClass stringClass = mock(StringClass.class);
        when(stringClass.getClassType()).thenReturn("String");

        when(xclass.get("comment")).thenReturn(mock(TextAreaClass.class));
        when(xclass.get("summary")).thenReturn(stringClass);
        when(xclass.get("password")).thenReturn(mock(PasswordClass.class));
        when(xclass.get("enabled")).thenReturn(mock(BooleanClass.class));

        //
        // Call
        //
        SolrInputDocument solrDocument = this.metadataExtractor.getSolrDocument(this.frenchDocumentReference);

        //
        // Assert and verify
        //
        assertEquals(Arrays.asList("space.commentsClass"), solrDocument.getFieldValues(FieldUtils.CLASS));

        // A TextArea property must be indexed as a localized text.
        assertSame(commentContent,
            solrDocument.getFieldValue(FieldUtils.getFieldName("property.space.commentsClass.comment", Locale.FRENCH)));
        assertNull(solrDocument.getFieldValue("property.space.commentsClass.comment_string"));

        // A String property must be indexed both as localized text and as raw (unanalyzed) text.
        assertSame(commentSummary,
            solrDocument.getFieldValue(FieldUtils.getFieldName("property.space.commentsClass.summary", Locale.FRENCH)));
        assertSame(commentSummary, solrDocument.getFieldValue("property.space.commentsClass.summary_string"));

        assertSame(commentAuthor, solrDocument.getFieldValue("property.space.commentsClass.author_string"));
        assertSame(commentDate, solrDocument.getFieldValue("property.space.commentsClass.date_date"));
        assertEquals(commentList, solrDocument.getFieldValues("property.space.commentsClass.list_string"));
        assertSame(commentLikes, solrDocument.getFieldValue("property.space.commentsClass.likes_long"));
        assertTrue((Boolean) solrDocument.getFieldValue("property.space.commentsClass.enabled_boolean"));

        // Make sure the password is not indexed (neither as a string nor as a localized text).
        assertNull(solrDocument.getFieldValue("property.space.commentsClass.password_string"));
        assertNull(solrDocument
            .getFieldValue(FieldUtils.getFieldName("property.space.commentsClass.password", Locale.FRENCH)));

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
            solrDocument.getFieldValues(FieldUtils.getFieldName("object.space.commentsClass", Locale.FRENCH));
        MatcherAssert.assertThat(objectProperties, Matchers.<Object>containsInAnyOrder(commentContent, commentSummary,
            commentAuthor, commentDate, commentList.get(0), commentList.get(1), commentLikes, true));
        assertEquals(8, objectProperties.size());

        objectProperties =
            solrDocument.getFieldValues(FieldUtils.getFieldName(FieldUtils.OBJECT_CONTENT, Locale.FRENCH));
        MatcherAssert.assertThat(objectProperties,
            Matchers.<Object>containsInAnyOrder("comment : " + commentContent, "summary : " + commentSummary,
                "author : " + commentAuthor, "date : " + commentDate, "list : " + commentList.get(0),
                "list : " + commentList.get(1), "likes : " + commentLikes, "enabled : true"));
        assertEquals(8, objectProperties.size());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void getDocumentWithEmailObject(boolean obfuscate) throws Exception
    {
        when(this.mailConfiguration.shouldObfuscate()).thenReturn(obfuscate);

        // Mock the user object
        BaseObject userObject = mock(BaseObject.class);
        // Mock the email property
        String email = "test@example.com";
        BaseProperty<EntityReference> emailField = mock(BaseProperty.class);
        when(emailField.getName()).thenReturn("email");
        when(emailField.getValue()).thenReturn(email);
        when(emailField.getObject()).thenReturn(userObject);

        // Mock the class reference
        DocumentReference userClassRef = new DocumentReference("wiki", "space", "userClass");

        // Add the mocked email object
        when(this.document.getXObjects())
            .thenReturn(Collections.singletonMap(userClassRef, Arrays.asList(userObject)));

        // Mock the class
        BaseClass xclass = mock(BaseClass.class);
        when(userObject.getXClass(this.xcontext)).thenReturn(xclass);
        when(userObject.getFieldList()).thenReturn(List.of(emailField));
        when(userObject.getRelativeXClassReference())
            .thenReturn(userClassRef.removeParent(userClassRef.getWikiReference()));

        when(xclass.get("email")).thenReturn(mock(EmailClass.class));

        //
        // Call
        //
        SolrInputDocument solrDocument = this.metadataExtractor.getSolrDocument(this.frenchDocumentReference);

        //
        // Assert and verify
        //
        String serializedUserClass = "space.userClass";
        assertEquals(List.of(serializedUserClass), solrDocument.getFieldValues(FieldUtils.CLASS));

        // Make sure the password is not indexed (neither as a string nor as a localized text).
        String emailProperty = "property." + serializedUserClass + ".email";
        String emailString = emailProperty + "_string";
        String emailSortString = emailProperty + "_sortString";
        if (obfuscate) {
            assertNull(solrDocument.getFieldValue(emailString));
            assertNull(solrDocument.getFieldValue(emailSortString));

            assertNull(solrDocument
                .getFieldValue(FieldUtils.getFieldName(emailProperty, Locale.FRENCH)));
        } else {
            assertEquals(email, solrDocument.getFieldValue(emailString));
            assertEquals(email, solrDocument.getFieldValue(emailSortString));
        }

        Collection<Object> objectProperties =
            solrDocument.getFieldValues(FieldUtils.getFieldName("object." + serializedUserClass, Locale.FRENCH));
        assertEquals(obfuscate ? null : List.of(email), objectProperties);

        objectProperties =
            solrDocument.getFieldValues(FieldUtils.getFieldName(FieldUtils.OBJECT_CONTENT, Locale.FRENCH));
        assertEquals(obfuscate ? null : List.of("email : " + email), objectProperties);
    }

    /**
     * @see "XWIKI-9417: Search does not return any results for Static List values"
     */
    @Test
    void setStaticListPropertyValue() throws Exception
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
        when(xobject.getFieldList()).thenReturn(Arrays.<Object>asList(listProperty));
        when(xobject.getRelativeXClassReference())
            .thenReturn(classReference.removeParent(classReference.getWikiReference()));

        StaticListClass staticListClass = mock(StaticListClass.class);
        when(xclass.get("color")).thenReturn(staticListClass);
        when(staticListClass.getMap(xcontext))
            .thenReturn(Collections.singletonMap("red", new ListItem("red", "Dark Red")));

        SolrInputDocument solrDocument = this.metadataExtractor.getSolrDocument(this.documentReference);

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

    private XWikiAttachment createMockAttachment(String fileName, String mimeType, Date date, String content,
        String authorAlias, String authorDisplayName) throws Exception
    {
        return createMockAttachment(fileName, mimeType, date, content.getBytes(), authorAlias, authorDisplayName);
    }

    private XWikiAttachment createMockAttachment(String fileName, String mimeType, Date date, InputStream content,
        String authorAlias, String authorDisplayName) throws Exception
    {
        return createMockAttachment(fileName, mimeType, date, IOUtils.toByteArray(content), authorAlias,
            authorDisplayName);
    }

    private XWikiAttachment createMockAttachment(String fileName, String mimeType, Date date, byte[] content,
        String authorAlias, String authorDisplayName) throws Exception
    {
        XWikiAttachment attachment = mock(XWikiAttachment.class, fileName);
        when(attachment.getReference()).thenReturn(new AttachmentReference(fileName, this.documentReference));
        when(attachment.getFilename()).thenReturn(fileName);
        when(attachment.getMimeType(this.xcontext)).thenReturn(mimeType);
        when(attachment.getDate()).thenReturn(date);
        when(attachment.getLongSize()).thenReturn((long)content.length);
        when(attachment.getContentInputStream(this.xcontext)).thenReturn(new ByteArrayInputStream(content));

        DocumentReference authorReference = new DocumentReference("wiki", "XWiki", authorAlias);
        when(attachment.getAuthorReference()).thenReturn(authorReference);

        when(this.xcontext.getWiki().getPlainUserName(authorReference, this.xcontext)).thenReturn(authorDisplayName);

        return attachment;
    }

    // Attachments

    private void assertAttachmentExtract(String expect, String filename) throws Exception
    {
        XWikiAttachment logo;
        try (InputStream stream = getClass().getResourceAsStream("/files/" + filename)) {
            logo = createMockAttachment(filename, "image/png", new Date(), stream, "Alice", "Shy Alice");
            when(this.document.getAttachmentList()).thenReturn(Arrays.<XWikiAttachment>asList(logo));
        }

        SolrInputDocument solrDocument = this.metadataExtractor.getSolrDocument(this.documentReference);

        assertEquals(expect, solrDocument.getFieldValue("attcontent_en_US"), "Wrong attachment content indexed");

    }

    @Test
    void getDocumentWithAttachments() throws Exception
    {
        Date logoDate = new Date(123);
        XWikiAttachment logo = createMockAttachment("logo.png", "image/png", logoDate, "foo", "Alice", "Shy Alice");
        Date todoDate = new Date(456);
        XWikiAttachment todo = createMockAttachment("todo.txt", "text/plain", todoDate, "bar bar", "Bob", "Angry Bob");
        when(this.document.getAttachmentList()).thenReturn(Arrays.<XWikiAttachment>asList(logo, todo));

        SolrInputDocument solrDocument =
            this.metadataExtractor.getSolrDocument(this.frenchDocumentReference);

        assertEquals(Arrays.asList("logo.png", "todo.txt"), solrDocument.getFieldValues(FieldUtils.FILENAME));
        assertEquals(Arrays.asList("image/png", "text/plain"), solrDocument.getFieldValues(FieldUtils.MIME_TYPE));
        assertEquals(Arrays.asList(logoDate, todoDate), solrDocument.getFieldValues(FieldUtils.ATTACHMENT_DATE));
        assertEquals(Arrays.asList(3L, 7L), solrDocument.getFieldValues(FieldUtils.ATTACHMENT_SIZE));
        assertEquals(Arrays.asList("foo\n", "bar bar\n"), solrDocument.getFieldValues("attcontent_fr"));
        assertEquals(Arrays.asList("wiki:XWiki.Alice", "wiki:XWiki.Bob"),
            solrDocument.getFieldValues(FieldUtils.ATTACHMENT_AUTHOR));
        assertEquals(Arrays.asList("Shy Alice", "Angry Bob"),
            solrDocument.getFieldValues(FieldUtils.ATTACHMENT_AUTHOR_DISPLAY));
    }

    @Test
    void testAttachmentExtractFromTxt() throws Exception
    {
        assertAttachmentExtract("text content\n", "txt.txt");
    }

    @Test
    void testAttachmentExtractFromMSOffice97() throws Exception
    {
        assertAttachmentExtract("MS Office 97 content\n\n", "msoffice97.doc");

    }

    @Test
    void testAttachmentExtractFromOpenXML() throws Exception
    {
        assertAttachmentExtract("OpenXML content\n", "openxml.docx");
    }

    @Test
    void testAttachmentExtractFromOpenDocument() throws Exception
    {
        assertAttachmentExtract("OpenDocument content\n", "opendocument.odt");
    }

    @Test
    void testAttachmentExtractFromPDF() throws Exception
    {
        assertAttachmentExtract("\nPDF content\n\n\n", "pdf.pdf");
    }

    @Test
    void testAttachmentExtractFromZIP() throws Exception
    {
        assertAttachmentExtract("\nzip.txt\nzip content\n\n\n\n", "zip.zip");
    }

    @Test
    void testAttachmentExtractFromHTML() throws Exception
    {
        assertAttachmentExtract("something\n", "html.html");
    }

    @Test
    void testAttachmentExtractFromClass() throws Exception
    {
        String expectedContent =
            "public synchronized class HelloWorld {\n" +
                "    public void HelloWorld();\n" +
                "    public static void main(String[]);\n" +
                "}\n\n";
        assertAttachmentExtract(expectedContent, "HelloWorld.class");
    }
}
