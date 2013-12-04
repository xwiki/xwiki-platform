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

import static org.hamcrest.collection.IsCollectionWithSize.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.inject.Provider;

import org.apache.solr.common.SolrInputDocument;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.search.solr.internal.DefaultSolrFieldNameEncoder;
import org.xwiki.search.solr.internal.SolrFieldStringEntityReferenceSerializer;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.reference.DocumentSolrReferenceResolver;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Tests for document metadata extraction.
 * 
 * @version $Id$
 */
@ComponentList({DefaultStringEntityReferenceSerializer.class, LocalStringEntityReferenceSerializer.class,
DocumentSolrMetadataExtractor.class, DefaultExecution.class, DocumentSolrReferenceResolver.class,
DefaultSolrFieldNameEncoder.class, SolrFieldStringEntityReferenceSerializer.class})
public class DocumentSolrMetadataExtractorTest
{
    @Rule
    public final MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    private XWikiContext xcontext;

    private XWikiDocument mockDocument;

    private XWiki mockXWiki;

    private DocumentAccessBridge mockDab;

    private DocumentReference documentReference;

    private DocumentReference documentReferenceFrench;

    private String documentReferenceString;

    private String documentReferenceLocalString;

    private String languageENUS;

    private Locale localeENUS;

    private String renderedContent;

    private String rawContent;

    private String title;

    private String version;

    private String comment;

    private boolean hidden;

    private Date date;

    private Date creationDate;

    private DocumentReference authorReference;

    private String authorString;

    private String authorDisplay;

    private DocumentReference creatorReference;

    private String creatorString;

    private String creatorDisplay;

    @BeforeComponent
    public void registerComponents() throws Exception
    {
        this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        this.mocker.registerMockComponent(BlockRenderer.class, "plain/1.0");
        this.mockDab = this.mocker.registerMockComponent(DocumentAccessBridge.class);
    }

    @Before
    public void setUp() throws Exception
    {
        EntityReferenceSerializer<String> localSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        EntityReferenceSerializer<String> serializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "default");

        // No locale provided.
        this.documentReference = new DocumentReference("wiki", "space", "name");
        this.documentReferenceString = serializer.serialize(this.documentReference);
        this.documentReferenceLocalString = localSerializer.serialize(this.documentReference);
        this.documentReferenceFrench = new DocumentReference(this.documentReference, Locale.FRENCH);

        this.localeENUS = Locale.US;
        this.languageENUS = this.localeENUS.getLanguage();
        this.renderedContent = "content";
        this.rawContent = "raw content";
        this.title = "title";
        this.version = "1.1";
        this.comment = "1.1 comment";
        this.hidden = false;
        this.date = new Date();
        this.creationDate = new Date();

        this.authorReference = new DocumentReference("wiki", "space", "author");
        this.authorString = serializer.serialize(this.authorReference);
        this.authorDisplay = "Au Thor";

        this.creatorReference = new DocumentReference("wiki", "space", "Creator");
        this.creatorString = serializer.serialize(this.creatorReference);
        this.creatorDisplay = "Crea Tor";

        // Mock

        this.xcontext = mock(XWikiContext.class);

        // XWikiContext Provider

        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);

        // XWikiContext trough Execution

        Execution execution = this.mocker.getInstance(Execution.class);
        execution.setContext(new ExecutionContext());
        execution.getContext().setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.xcontext);

        this.mockXWiki = mock(XWiki.class);
        this.mockDocument = mock(XWikiDocument.class);

        when(this.xcontext.getWiki()).thenReturn(this.mockXWiki);
        when(this.mockXWiki.getDocument(this.documentReference, this.xcontext)).thenReturn(this.mockDocument);

        when(this.mockDocument.getDocumentReference()).thenReturn(this.documentReference);

        when(this.mockDocument.getTranslatedDocument(isNull(Locale.class), eq(this.xcontext))).thenReturn(
            this.mockDocument);

        when(this.mockDab.getDocument(this.documentReference)).thenReturn(this.mockDocument);

        when(this.mockDocument.getContent()).thenReturn(this.rawContent);

        BlockRenderer mockPlainRenderer = this.mocker.getInstance(BlockRenderer.class, "plain/1.0");
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation)
            {
                Object[] args = invocation.getArguments();

                WikiPrinter printer = (WikiPrinter) args[1];
                printer.print(DocumentSolrMetadataExtractorTest.this.renderedContent);

                return null;
            }
        }).when(mockPlainRenderer).render(any(Block.class), any(WikiPrinter.class));

        when(this.mockDocument.getRenderedTitle(any(Syntax.class), eq(this.xcontext))).thenReturn(this.title);

        when(this.mockDocument.getVersion()).thenReturn(this.version);
        when(this.mockDocument.getComment()).thenReturn(this.comment);

        when(this.mockDocument.getAuthorReference()).thenReturn(this.authorReference);
        when(this.mockXWiki.getUserName(this.authorString, null, false, this.xcontext)).thenReturn(this.authorDisplay);

        when(this.mockDocument.getCreatorReference()).thenReturn(this.creatorReference);
        when(this.mockXWiki.getUserName(this.creatorString, null, false, this.xcontext))
            .thenReturn(this.creatorDisplay);

        when(this.mockDocument.getCreationDate()).thenReturn(this.creationDate);
        when(this.mockDocument.getContentUpdateDate()).thenReturn(this.date);

        when(this.mockDocument.isHidden()).thenReturn(this.hidden);

        when(this.mockDocument.getLocale()).thenReturn(Locale.ROOT);
        when(this.mockDocument.getRealLocale()).thenReturn(this.localeENUS);
    }

    @Test
    public void getSimpleDocument() throws Exception
    {
        // Mock

        // No objects (and no comments).
        when(this.mockDocument.getXObjects()).thenReturn(new HashMap<DocumentReference, List<BaseObject>>());

        // Call

        DocumentSolrMetadataExtractor extractor =
            (DocumentSolrMetadataExtractor) this.mocker.getInstance(SolrMetadataExtractor.class, "document");
        SolrInputDocument solrDocument = extractor.getSolrDocument(this.documentReference);

        // Assert and verify

        Assert.assertEquals(String.format("%s_%s", this.documentReferenceString, this.localeENUS),
            solrDocument.getFieldValue(FieldUtils.ID));

        Assert.assertEquals(this.documentReference.getWikiReference().getName(),
            solrDocument.getFieldValue(FieldUtils.WIKI));
        Assert.assertEquals(this.documentReference.getLastSpaceReference().getName(),
            solrDocument.getFieldValue(FieldUtils.SPACE));
        Assert.assertEquals(this.documentReference.getName(), solrDocument.getFieldValue(FieldUtils.NAME));

        Assert.assertEquals(this.localeENUS.toString(), solrDocument.getFieldValue(FieldUtils.LOCALE));
        Assert.assertEquals(this.languageENUS, solrDocument.getFieldValue(FieldUtils.LANGUAGE));
        Assert.assertThat((Collection) solrDocument.getFieldValues(FieldUtils.LOCALES),
            CoreMatchers.both((Matcher) CoreMatchers.hasItems("", this.localeENUS.toString()))
                .and((Matcher) hasSize(2)));
        Assert.assertEquals(this.hidden, solrDocument.getFieldValue(FieldUtils.HIDDEN));
        Assert.assertEquals(EntityType.DOCUMENT.name(), solrDocument.getFieldValue(FieldUtils.TYPE));

        Assert.assertEquals(this.documentReferenceLocalString, solrDocument.getFieldValue(FieldUtils.FULLNAME));

        Assert.assertEquals(this.title,
            solrDocument.getFieldValue(FieldUtils.getFieldName(FieldUtils.TITLE, this.localeENUS)));
        Assert.assertEquals(this.rawContent,
            solrDocument.getFieldValue(FieldUtils.getFieldName(FieldUtils.DOCUMENT_RAW_CONTENT, this.localeENUS)));
        Assert.assertEquals(this.renderedContent,
            solrDocument.getFieldValue(FieldUtils.getFieldName(FieldUtils.DOCUMENT_RENDERED_CONTENT, this.localeENUS)));

        Assert.assertEquals(this.version, solrDocument.getFieldValue(FieldUtils.VERSION));
        Assert.assertEquals(this.comment, solrDocument.getFieldValue(FieldUtils.COMMENT));

        Assert.assertEquals(this.authorString, solrDocument.getFieldValue(FieldUtils.AUTHOR));
        Assert.assertEquals(this.authorDisplay, solrDocument.getFieldValue(FieldUtils.AUTHOR_DISPLAY));
        Assert.assertEquals(this.creatorString, solrDocument.getFieldValue(FieldUtils.CREATOR));
        Assert.assertEquals(this.creatorDisplay, solrDocument.getFieldValue(FieldUtils.CREATOR_DISPLAY));

        Assert.assertEquals(this.creationDate, solrDocument.getFieldValue(FieldUtils.CREATIONDATE));
        Assert.assertEquals(this.date, solrDocument.get(FieldUtils.DATE).getValue());
    }

    @Test
    public void getDocumentThrowingException() throws Exception
    {
        DocumentReference reference = new DocumentReference(this.documentReference, Locale.FRENCH);
        XWikiException thrown =
            new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC, "Unreadable document");
        when(this.mockXWiki.getDocument(eq(reference), eq(this.xcontext))).thenThrow(thrown);

        // Call
        DocumentSolrMetadataExtractor extractor =
            (DocumentSolrMetadataExtractor) this.mocker.getInstance(SolrMetadataExtractor.class, "document");
        try {
            extractor.getSolrDocument(reference);
        } catch (SolrIndexerException ex) {
            Assert.assertEquals("Failed to get input Solr document for entity '" + this.documentReferenceFrench + "'",
                ex.getMessage());
            Assert.assertSame(thrown, ex.getCause());
            return;
        }
        Assert.assertFalse("Shouldn't have gotten here", true);
    }

    @Test
    public void getDocumentWithObjects() throws Exception
    {
        DocumentReference commentsClassReference = new DocumentReference("wiki", "space", "commentsClass");
        String commentContent = "This is a comment";
        String commentAuthor = "wiki:space.commentAuthor";
        Date commentDate = new Date();
        // Adding a fake password field to the comments class just to test the branch in the code.
        String commentPassword = "password";
        List<String> commentList = Arrays.asList("a", "list");
        Long commentLikes = 13L;

        List<BaseProperty<EntityReference>> commentFields = new ArrayList<BaseProperty<EntityReference>>();

        // Mock

        BaseObject mockComment = mock(BaseObject.class);

        BaseProperty<EntityReference> mockCommentField = mock(BaseProperty.class);
        when(mockCommentField.getName()).thenReturn("comment");
        when(mockCommentField.getValue()).thenReturn(commentContent);
        when(mockCommentField.getObject()).thenReturn(mockComment);
        commentFields.add(mockCommentField);

        BaseProperty<EntityReference> mockAuthorField = mock(BaseProperty.class);
        when(mockAuthorField.getName()).thenReturn("author");
        when(mockAuthorField.getValue()).thenReturn(commentAuthor);
        when(mockAuthorField.getObject()).thenReturn(mockComment);
        commentFields.add(mockAuthorField);

        BaseProperty<EntityReference> mockDateField = mock(BaseProperty.class);
        when(mockDateField.getName()).thenReturn("date");
        when(mockDateField.getValue()).thenReturn(commentDate);
        when(mockDateField.getObject()).thenReturn(mockComment);
        commentFields.add(mockDateField);

        BaseProperty<EntityReference> mockPasswordField = mock(BaseProperty.class);
        when(mockPasswordField.getName()).thenReturn("password");
        when(mockPasswordField.getValue()).thenReturn(commentPassword);
        commentFields.add(mockPasswordField);

        BaseProperty<EntityReference> mockListField = mock(BaseProperty.class);
        when(mockListField.getName()).thenReturn("list");
        when(mockListField.getValue()).thenReturn(commentList);
        when(mockListField.getObject()).thenReturn(mockComment);
        commentFields.add(mockListField);
        
        BaseProperty<EntityReference> mockNumberField = mock(BaseProperty.class);
        when(mockNumberField.getName()).thenReturn("likes");
        when(mockNumberField.getValue()).thenReturn(commentLikes);
        when(mockNumberField.getObject()).thenReturn(mockComment);
        commentFields.add(mockNumberField);
        
        BaseProperty<EntityReference> mockBooleanField = mock(BaseProperty.class);
        when(mockBooleanField.getName()).thenReturn("enabled");
        when(mockBooleanField.getValue()).thenReturn(1);
        when(mockBooleanField.getObject()).thenReturn(mockComment);
        commentFields.add(mockBooleanField);

        // When handled as a general object
        HashMap<DocumentReference, List<BaseObject>> xObjects = new HashMap<DocumentReference, List<BaseObject>>();
        xObjects.put(commentsClassReference, Arrays.asList(mockComment));
        when(this.mockDocument.getXObjects()).thenReturn(xObjects);

        BaseClass mockXClass = mock(BaseClass.class);
        when(mockComment.getXClass(this.xcontext)).thenReturn(mockXClass);
        when(mockComment.getFieldList()).thenReturn(commentFields);
        when(mockComment.getRelativeXClassReference()).thenReturn(
            commentsClassReference.removeParent(commentsClassReference.getWikiReference()));

        when(mockXClass.get("comment")).thenReturn(mock(TextAreaClass.class));
        when(mockXClass.get("password")).thenReturn(mock(PasswordClass.class));
        when(mockXClass.get("enabled")).thenReturn(mock(BooleanClass.class));

        // Call

        DocumentSolrMetadataExtractor extractor =
            (DocumentSolrMetadataExtractor) this.mocker.getInstance(SolrMetadataExtractor.class, "document");
        SolrInputDocument solrDocument = extractor.getSolrDocument(this.documentReference);

        // Assert and verify

        Assert.assertEquals(Arrays.asList("space.commentsClass"), solrDocument.getFieldValues(FieldUtils.CLASS));

        // A TextArea property must be indexed as a localized text.
        Assert.assertSame(commentContent, solrDocument.getFieldValue(FieldUtils.getFieldName(
            "property.space.commentsClass.comment", this.localeENUS)));

        Assert.assertSame(commentAuthor, solrDocument.getFieldValue("property.space.commentsClass.author_string"));
        Assert.assertSame(commentDate, solrDocument.getFieldValue("property.space.commentsClass.date_date"));
        Assert.assertEquals(commentList, solrDocument.getFieldValues("property.space.commentsClass.list_string"));
        Assert.assertSame(commentLikes, solrDocument.getFieldValue("property.space.commentsClass.likes_long"));
        Assert.assertTrue((Boolean) solrDocument.getFieldValue("property.space.commentsClass.enabled_boolean"));

        // Make sure the password is not indexed (neither as a string nor as a localized text).
        Assert.assertNull(solrDocument.getFieldValue("property.space.commentsClass.password_string"));
        Assert.assertNull(solrDocument.getFieldValue(FieldUtils.getFieldName("property.space.commentsClass.password",
            this.localeENUS)));

        Collection<Object> objectProperties =
            solrDocument.getFieldValues(FieldUtils.getFieldName("object.space.commentsClass", this.localeENUS));
        MatcherAssert.assertThat(objectProperties, Matchers.<Object> containsInAnyOrder(commentContent, commentAuthor,
            commentDate, commentList.get(0), commentList.get(1), commentLikes, true));
        Assert.assertEquals(7, objectProperties.size());

        objectProperties =
            solrDocument.getFieldValues(FieldUtils.getFieldName(FieldUtils.OBJECT_CONTENT, this.localeENUS));
        MatcherAssert.assertThat(objectProperties, Matchers.<Object>containsInAnyOrder("comment : " + commentContent,
            "author : " + commentAuthor,"date : " + commentDate, "list : " + commentList.get(0),
            "list : " + commentList.get(1), "likes : " + commentLikes, "enabled : true"));
        Assert.assertEquals(7, objectProperties.size());
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
        when(this.mockDocument.getXObjects()).thenReturn(
            Collections.singletonMap(classReference, Arrays.asList(xobject)));

        BaseClass xclass = mock(BaseClass.class);
        when(xobject.getXClass(this.xcontext)).thenReturn(xclass);
        when(xobject.getFieldList()).thenReturn(Arrays.<Object> asList(listProperty));
        when(xobject.getRelativeXClassReference()).thenReturn(
            classReference.removeParent(classReference.getWikiReference()));

        StaticListClass staticListClass = mock(StaticListClass.class);
        when(xclass.get("color")).thenReturn(staticListClass);
        when(staticListClass.getMap(xcontext)).thenReturn(
            Collections.singletonMap("red", new ListItem("red", "Dark Red")));

        DocumentSolrMetadataExtractor extractor =
            (DocumentSolrMetadataExtractor) this.mocker.getInstance(SolrMetadataExtractor.class, "document");
        SolrInputDocument solrDocument = extractor.getSolrDocument(this.documentReference);

        // Make sure both the raw value (which is saved in the database) and the display value (specified in the XClass)
        // are indexed. The raw values are indexed as strings in order to be able to perform exact matches.
        Assert.assertEquals(Arrays.asList("red", "green"),
            solrDocument.getFieldValues("property.Space.MyClass.color_string"));
        Assert.assertEquals(Collections.singletonList("Dark Red"),
            solrDocument.getFieldValues(FieldUtils.getFieldName("property.Space.MyClass.color", this.localeENUS)));
    }
}
