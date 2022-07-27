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
package org.xwiki.search.solr.internal.reference;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Provider;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.Utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the extraction of indexable references from a start reference.
 * 
 * @version $Id$
 */
@ComponentList({
    DefaultExecution.class,
    AttachmentSolrReferenceResolver.class,
    DefaultSolrReferenceResolver.class,
    DocumentSolrReferenceResolver.class,
    ObjectPropertySolrReferenceResolver.class,
    ObjectSolrReferenceResolver.class,
    SpaceSolrReferenceResolver.class,
    WikiSolrReferenceResolver.class,
})
@ReferenceComponentList
public class SolrReferenceResolverTest
{
    @Rule
    public final MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    private SolrReferenceResolver defaultSolrReferenceResolver;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private WikiReference wikiReference1 = new WikiReference("wiki1");

    private SpaceReference spaceReference11 = new SpaceReference("data", new SpaceReference("space11", wikiReference1));

    private DocumentReference classReference111 = new DocumentReference("class111", spaceReference11);

    private DocumentReference documentReference111 = classReference111;

    private BaseClass xclass111;

    private XWikiDocument xdocument111;

    private DocumentReference documentReference112 = new DocumentReference("document112", spaceReference11);

    private XWikiDocument xdocument112;

    private DocumentReference documentReference113 = new DocumentReference("document113", spaceReference11);

    private DocumentReference documentReference113RO = new DocumentReference(documentReference113, new Locale("ro"));

    private XWikiDocument xdocument113 = null;

    private SpaceReference spaceReference12 = new SpaceReference("code", new SpaceReference("space12", wikiReference1));

    private DocumentReference documentReference121 = new DocumentReference("document121", spaceReference12);

    private AttachmentReference attachmentReference1211 = new AttachmentReference("attachment1211.ext",
        documentReference121);

    private XWikiAttachment xattachment1211;

    private AttachmentReference attachmentReference1212 = new AttachmentReference("attachment1212.ext",
        documentReference121);

    private XWikiAttachment xattachment1212;

    private XWikiDocument xdocument121;

    private DocumentReference documentReference122 = new DocumentReference("document122", spaceReference12);

    private BaseObjectReference objectReference1221;

    private BaseObject xobject1221;

    private BaseObjectReference objectReference1222;

    private ObjectPropertyReference propertyReference12221;

    private StringProperty xproperty12221;

    private ObjectPropertyReference propertyReference12223;

    private IntegerProperty xproperty12223;

    private BaseObject xobject1222;

    private XWikiDocument xdocument122;

    private SpaceReference spaceReference13 = new SpaceReference("test", new SpaceReference("space13", wikiReference1));

    private WikiReference wikiReference2 = new WikiReference("wiki2");

    private QueryManager queryManager;

    @BeforeComponent
    public void registerComponents() throws Exception
    {
        this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        this.mocker.registerMockComponent(QueryManager.class);

        WikiDescriptorManager wikiDescriptorManager = this.mocker.registerMockComponent(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getAllIds()).thenReturn(
            Arrays.asList(wikiReference1.getName(), wikiReference2.getName()));
    }

    @Before
    public void setUp() throws Exception
    {
        this.defaultSolrReferenceResolver = this.mocker.getInstance(SolrReferenceResolver.class);

        Utils.setComponentManager(this.mocker);

        // XWiki

        this.xwiki = mock(XWiki.class);

        // XWikiContext

        this.xcontext = new XWikiContext();
        this.xcontext.setWikiId("xwiki");
        this.xcontext.setWiki(this.xwiki);

        // XWikiContext Provider

        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);

        // XWikiContext trough Execution

        Execution execution = this.mocker.getInstance(Execution.class);
        execution.setContext(new ExecutionContext());
        execution.getContext().setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.xcontext);

        // References

        this.objectReference1221 = new BaseObjectReference(this.classReference111, 0, this.documentReference122);
        this.objectReference1222 = new BaseObjectReference(this.classReference111, 1, this.documentReference122);
        this.propertyReference12221 = new ObjectPropertyReference("aStringProperty", objectReference1222);
        this.propertyReference12223 = new ObjectPropertyReference("anIntegerProperty", objectReference1222);

        // XWiki model data

        this.xclass111 = mock(BaseClass.class);
        this.xdocument111 = mock(XWikiDocument.class, "xwikiDocument111");
        this.xdocument112 = mock(XWikiDocument.class, "xwikiDocument112");
        this.xdocument113 = mock(XWikiDocument.class, "xwikiDocument113");

        this.xdocument121 = mock(XWikiDocument.class, "xwikiDocument121");
        this.xattachment1211 = mock(XWikiAttachment.class, "xwikiAttachment1211");
        this.xattachment1212 = mock(XWikiAttachment.class, "xwikiAttachment1212");

        this.xdocument122 = mock(XWikiDocument.class, "xwikiDocument122");
        this.xobject1221 = mock(BaseObject.class, "xwikiObject1221");
        this.xobject1222 = mock(BaseObject.class, "xwikiObject1222");
        this.xproperty12221 = mock(StringProperty.class, "xwikiProperty12221");
        this.xproperty12223 = mock(IntegerProperty.class, "xwikiProperty12223");

        this.queryManager = this.mocker.getInstance(QueryManager.class);
        final Query spacesWiki1Query = mock(DefaultQuery.class, "getSpacesWiki1");
        final Query documentsSpace11Query = mock(DefaultQuery.class, "getSpaceDocsNameSpace11");
        final Query documentsSpace12Query = mock(DefaultQuery.class, "getSpaceDocsNameSpace12");
        final Query documentsSpace13Query = mock(DefaultQuery.class, "getSpaceDocsNameSpace13");
        final Query spacesWiki2Query = mock(DefaultQuery.class, "getSpacesWiki2");

        // Data

        when(xwiki.exists(any(DocumentReference.class), any(XWikiContext.class))).thenReturn(true);

        // Query manager and specific queries mocking.

        when(queryManager.getNamedQuery("getSpaces")).thenReturn(spacesWiki1Query);

        when(spacesWiki1Query.setWiki(wikiReference1.getName())).thenReturn(spacesWiki1Query);

        when(spacesWiki1Query.setWiki(wikiReference2.getName())).thenReturn(spacesWiki2Query);

        when(queryManager.getNamedQuery("getSpaceDocsName")).thenReturn(documentsSpace11Query);

        when(documentsSpace11Query.setWiki(any(String.class))).thenReturn(documentsSpace11Query);

        EntityReferenceSerializer<String> localEntityReferenceSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(documentsSpace11Query.bindValue("space", localEntityReferenceSerializer.serialize(spaceReference11)))
            .thenReturn(documentsSpace11Query);
        when(documentsSpace11Query.bindValue("space", localEntityReferenceSerializer.serialize(spaceReference12)))
            .thenReturn(documentsSpace12Query);
        when(documentsSpace11Query.bindValue("space", localEntityReferenceSerializer.serialize(spaceReference13)))
            .thenReturn(documentsSpace13Query);

        // Spaces in wikis.
        when(spacesWiki1Query.execute()).thenReturn(
            Arrays.<Object>asList(localEntityReferenceSerializer.serialize(spaceReference11),
                localEntityReferenceSerializer.serialize(spaceReference12),
                localEntityReferenceSerializer.serialize(spaceReference13)));

        when(spacesWiki2Query.execute()).thenReturn(Collections.emptyList());

        // space 11
        when(documentsSpace11Query.execute()).thenReturn(
            Arrays.<Object> asList(classReference111.getName(), documentReference112.getName(),
                documentReference113.getName()));

        // document 111
        when(xwiki.getDocument(eq(documentReference111), any(XWikiContext.class))).thenReturn(xdocument111);

        when(xdocument111.getXClass()).thenReturn(xclass111);

        // document 112
        when(xwiki.getDocument(eq(documentReference112), any(XWikiContext.class))).thenReturn(xdocument112);

        when(xdocument112.getXObjects()).thenReturn(Collections.<DocumentReference, List<BaseObject>>emptyMap());

        when(xdocument112.getTranslationLocales(any(XWikiContext.class))).thenReturn(Collections.<Locale>emptyList());

        // document 113
        when(xwiki.getDocument(eq(documentReference113), any(XWikiContext.class))).thenReturn(xdocument113);

        when(xdocument113.getAttachmentList()).thenReturn(Collections.<XWikiAttachment>emptyList());

        when(xdocument113.getXObjects()).thenReturn(Collections.<DocumentReference, List<BaseObject>>emptyMap());

        when(xdocument113.getTranslationLocales(any(XWikiContext.class))).thenReturn(Arrays.asList(new Locale("ro")));

        // space 12
        when(documentsSpace12Query.execute()).thenReturn(
            Arrays.<Object> asList(documentReference121.getName(), documentReference122.getName()));

        // document 121
        when(xwiki.getDocument(eq(documentReference121), any(XWikiContext.class))).thenReturn(xdocument121);

        when(xdocument121.getAttachmentList()).thenReturn(Arrays.asList(xattachment1211, xattachment1212));

        when(xattachment1211.getReference()).thenReturn(attachmentReference1211);

        when(xattachment1212.getReference()).thenReturn(attachmentReference1212);

        when(xdocument121.getXObjects()).thenReturn(Collections.<DocumentReference, List<BaseObject>>emptyMap());

        when(xdocument121.getTranslationLocales(any(XWikiContext.class))).thenReturn(Collections.<Locale>emptyList());

        // document 122
        when(xwiki.getDocument(eq(documentReference122), any(XWikiContext.class))).thenReturn(xdocument122);

        when(xdocument122.getAttachmentList()).thenReturn(Collections.<XWikiAttachment>emptyList());

        Map<DocumentReference, List<BaseObject>> xObjects = new HashMap<DocumentReference, List<BaseObject>>();
        // Yes, it seems that we can have null objects for some reason.
        xObjects.put(classReference111, Arrays.asList(null, xobject1221, xobject1222));
        when(xdocument122.getXObjects()).thenReturn(xObjects);

        when(xdocument122.getTranslationLocales(any(XWikiContext.class))).thenReturn(Collections.<Locale>emptyList());

        // object 1221
        when(xdocument122.getXObject(objectReference1221)).thenReturn(xobject1221);
        when(xdocument122.getXObject((EntityReference) objectReference1221)).thenReturn(xobject1221);

        when(xobject1221.getReference()).thenReturn(objectReference1221);

        when(xobject1221.getXClass(any(XWikiContext.class))).thenReturn(xclass111);

        when(xobject1221.getFieldList()).thenReturn(Collections.EMPTY_LIST);

        // object 1222
        when(xdocument122.getXObject(objectReference1222)).thenReturn(xobject1222);
        when(xdocument122.getXObject((EntityReference) objectReference1222)).thenReturn(xobject1222);

        when(xobject1222.getReference()).thenReturn(objectReference1222);

        when(xobject1222.getXClass(any(XWikiContext.class))).thenReturn(xclass111);

        when(xobject1222.getFieldList()).thenReturn(
            Arrays.asList(xproperty12221, xproperty12223));

        // object 1222 fields
        when(xproperty12221.getReference()).thenReturn(propertyReference12221);
        when(xproperty12221.getName()).thenReturn(propertyReference12221.getName());

        when(xproperty12223.getReference()).thenReturn(propertyReference12223);
        when(xproperty12223.getName()).thenReturn(propertyReference12223.getName());

        // class 111 fields
        when(xclass111.get(propertyReference12221.getName())).thenReturn(null);

        when(xclass111.get(propertyReference12223.getName())).thenReturn(null);

        // space 13
        when(documentsSpace13Query.execute()).thenReturn(Collections.emptyList());
    }

    // getReferences
    @Test
    public void getReferencesFarm() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(null);

        Assert.assertNotNull(result);
        assertThat(result, Matchers.<EntityReference> iterableWithSize(12));

        assertThat(
            result,
            containsInAnyOrder(classReference111, documentReference112, documentReference113, documentReference113RO,
                documentReference121, attachmentReference1211, attachmentReference1212, documentReference122,
                objectReference1221, objectReference1222, propertyReference12221, propertyReference12223));
    }

    @Test
    public void getReferencesEmptyWiki() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(wikiReference2);

        Assert.assertNotNull(result);
        assertThat(result, Matchers.<EntityReference> iterableWithSize(0));
    }

    @Test
    public void getReferencesWiki() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(wikiReference1);

        Assert.assertNotNull(result);
        assertThat(result, Matchers.<EntityReference> iterableWithSize(12));

        assertThat(
            result,
            containsInAnyOrder(classReference111, documentReference112, documentReference113, documentReference113RO,
                documentReference121, attachmentReference1211, attachmentReference1212, documentReference122,
                objectReference1221, objectReference1222, propertyReference12221, propertyReference12223));
    }

    @Test
    public void getReferencesSpaceReference() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(spaceReference11);

        Assert.assertNotNull(result);
        assertThat(result, Matchers.<EntityReference> iterableWithSize(4));

        assertThat(
            result,
            containsInAnyOrder((EntityReference) classReference111, (EntityReference) documentReference112,
                (EntityReference) documentReference113, (EntityReference) documentReference113RO));
    }

    @Test
    public void getReferencesEmptyDocument() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(documentReference112);

        Assert.assertNotNull(result);
        assertThat(result, Matchers.<EntityReference> iterableWithSize(1));

        Assert.assertEquals(documentReference112, result.iterator().next());
    }

    @Test
    public void getReferencesTranslatedDocument() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(documentReference113);

        Assert.assertNotNull(result);
        assertThat(result, Matchers.<EntityReference> iterableWithSize(2));

        assertThat(result,
            containsInAnyOrder((EntityReference) documentReference113, (EntityReference) documentReference113RO));
    }

    @Test
    public void getReferencesDocumentWithAttachments() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(documentReference121);

        Assert.assertNotNull(result);
        assertThat(result, Matchers.<EntityReference> iterableWithSize(3));

        assertThat(result, containsInAnyOrder(documentReference121, attachmentReference1211, attachmentReference1212));
    }

    @Test
    public void getReferencesDocumentWithObjects() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(documentReference122);

        Assert.assertNotNull(result);
        assertThat(result, Matchers.<EntityReference> iterableWithSize(5));

        assertThat(
            result,
            containsInAnyOrder(documentReference122, objectReference1221, propertyReference12221,
                propertyReference12223, objectReference1222));
    }

    @Test
    public void getReferencesAttachment() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(attachmentReference1211);

        Assert.assertNotNull(result);
        assertThat(result, Matchers.<EntityReference> iterableWithSize(1));

        Assert.assertEquals(attachmentReference1211, result.iterator().next());
    }

    @Test
    public void getReferencesEmptyObject() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(objectReference1221);

        Assert.assertNotNull(result);
        assertThat(result, Matchers.<EntityReference> iterableWithSize(1));

        Assert.assertEquals(objectReference1221, result.iterator().next());
    }

    @Test
    public void getReferencesObject() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(objectReference1222);

        Assert.assertNotNull(result);
        assertThat(result, Matchers.<EntityReference> iterableWithSize(3));

        assertThat(result, containsInAnyOrder(objectReference1222, propertyReference12221, propertyReference12223));
    }

    @Test
    public void getReferencesProperty() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(propertyReference12221);

        Assert.assertNotNull(result);
        assertThat(result, Matchers.<EntityReference> iterableWithSize(1));

        Assert.assertEquals(propertyReference12221, result.iterator().next());
    }

    // getId

    @Test
    public void getIdLocaleInReference() throws Exception
    {
        // Locale provided in the reference
        DocumentReference reference = new DocumentReference("wiki", "space", "name", new Locale("en"));

        // Call
        String id = this.defaultSolrReferenceResolver.getId(reference);

        // Assert
        Assert.assertEquals("wiki:space.name_en", id);
    }

    @Test
    public void getIdLocaleInDatabase() throws Exception
    {
        when(xdocument111.getRealLocale()).thenReturn(Locale.FRENCH);

        // Call
        String id = this.defaultSolrReferenceResolver.getId(this.documentReference111);

        // Assert and verify
        Assert.assertEquals(this.documentReference111 + "_fr", id);
    }

    // getQuery

    @Test
    public void getQueryWiki() throws Exception
    {
        Assert.assertEquals("wiki:wiki1", this.defaultSolrReferenceResolver.getQuery(wikiReference1));
    }

    @Test
    public void getQuerySpace() throws Exception
    {
        Assert.assertEquals("wiki:wiki1 AND space_exact:space11.data",
            this.defaultSolrReferenceResolver.getQuery(spaceReference11));
    }

    @Test
    public void getQueryDocument() throws Exception
    {
        Assert.assertEquals("wiki:wiki1 AND space_exact:space11.data AND name_exact:class111",
            this.defaultSolrReferenceResolver.getQuery(documentReference111));
    }

    @Test
    public void getQueryObject() throws Exception
    {
        Assert.assertEquals("wiki:wiki1" + " AND space_exact:space12.code AND name_exact:document122"
            + " AND class:space11.data.class111 AND number:0",
            this.defaultSolrReferenceResolver.getQuery(objectReference1221));
    }

    @Test
    public void getQueryObjectProperty() throws Exception
    {
        Assert.assertEquals(
            "id:wiki1\\:space12.code.document122\\^space11.data.class111\\[1\\].aStringProperty",
            this.defaultSolrReferenceResolver.getQuery(propertyReference12221));
    }

    @Test
    public void getQueryAttachment() throws Exception
    {
        Assert.assertEquals("id:wiki1\\:space12.code.document121@attachment1211.ext",
            this.defaultSolrReferenceResolver.getQuery(attachmentReference1211));
    }
}
