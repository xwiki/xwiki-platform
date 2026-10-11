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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Provider;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the extraction of indexable references from a start reference.
 *
 * @version $Id$
 */
@ComponentTest
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
class SolrReferenceResolverTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private SolrReferenceResolver defaultSolrReferenceResolver;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private WikiReference wikiReference1 = new WikiReference("wiki1");

    private SpaceReference spaceReference11 = new SpaceReference("data", new SpaceReference("space11", this.wikiReference1));

    private DocumentReference classReference111 = new DocumentReference("class111", this.spaceReference11);

    private DocumentReference documentReference111 = this.classReference111;

    private BaseClass xclass111;

    private XWikiDocument xdocument111;

    private DocumentReference documentReference112 = new DocumentReference("document112", this.spaceReference11);

    private XWikiDocument xdocument112;

    private DocumentReference documentReference113 = new DocumentReference("document113", this.spaceReference11);

    private DocumentReference documentReference113RO = new DocumentReference(this.documentReference113, new Locale("ro"));

    private XWikiDocument xdocument113 = null;

    private SpaceReference spaceReference12 = new SpaceReference("code", new SpaceReference("space12", this.wikiReference1));

    private DocumentReference documentReference121 = new DocumentReference("document121", this.spaceReference12);

    private AttachmentReference attachmentReference1211 = new AttachmentReference("attachment1211.ext",
        this.documentReference121);

    private AttachmentReference attachmentReference1212 = new AttachmentReference("attachment1212.ext",
        this.documentReference121);

    private XWikiAttachment xattachment1211;

    private XWikiAttachment xattachment1212;

    private XWikiDocument xdocument121;

    private DocumentReference documentReference122 = new DocumentReference("document122", this.spaceReference12);

    private BaseObjectReference objectReference1221;

    private BaseObject xobject1221;

    private BaseObjectReference objectReference1222;

    private ObjectPropertyReference propertyReference12221;

    private StringProperty xproperty12221;

    private ObjectPropertyReference propertyReference12223;

    private IntegerProperty xproperty12223;

    private BaseObject xobject1222;

    private XWikiDocument xdocument122;

    private SpaceReference spaceReference13 = new SpaceReference("test", new SpaceReference("space13", this.wikiReference1));

    private WikiReference wikiReference2 = new WikiReference("wiki2");

    private QueryManager queryManager;

    @BeforeComponent
    void registerComponents() throws Exception
    {
        this.componentManager.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        this.componentManager.registerMockComponent(QueryManager.class);

        WikiDescriptorManager wikiDescriptorManager = this.componentManager.registerMockComponent(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getAllIds()).thenReturn(
            List.of(this.wikiReference1.getName(), this.wikiReference2.getName()));
    }

    @BeforeEach
    void setUp() throws Exception
    {
        this.defaultSolrReferenceResolver = this.componentManager.getInstance(SolrReferenceResolver.class);

        Utils.setComponentManager(this.componentManager);

        // XWiki

        this.xwiki = mock(XWiki.class);

        // XWikiContext

        this.xcontext = new XWikiContext();
        this.xcontext.setWikiId("xwiki");
        this.xcontext.setWiki(this.xwiki);

        // XWikiContext Provider

        Provider<XWikiContext> xcontextProvider = this.componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);

        // XWikiContext trough Execution

        Execution execution = this.componentManager.getInstance(Execution.class);
        execution.setContext(new ExecutionContext());
        execution.getContext().setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.xcontext);

        // References

        this.objectReference1221 = new BaseObjectReference(this.classReference111, 0, this.documentReference122);
        this.objectReference1222 = new BaseObjectReference(this.classReference111, 1, this.documentReference122);
        this.propertyReference12221 = new ObjectPropertyReference("aStringProperty", this.objectReference1222);
        this.propertyReference12223 = new ObjectPropertyReference("anIntegerProperty", this.objectReference1222);

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

        this.queryManager = this.componentManager.getInstance(QueryManager.class);
        final Query spacesWiki1Query = mock(DefaultQuery.class, "getSpacesWiki1");
        final Query documentsSpace11Query = mock(DefaultQuery.class, "getSpaceDocsNameSpace11");
        final Query documentsSpace12Query = mock(DefaultQuery.class, "getSpaceDocsNameSpace12");
        final Query documentsSpace13Query = mock(DefaultQuery.class, "getSpaceDocsNameSpace13");
        final Query spacesWiki2Query = mock(DefaultQuery.class, "getSpacesWiki2");

        // Data

        when(this.xwiki.exists(any(DocumentReference.class), any(XWikiContext.class))).thenReturn(true);

        // Query manager and specific queries mocking.

        when(this.queryManager.getNamedQuery("getSpaces")).thenReturn(spacesWiki1Query);

        when(spacesWiki1Query.setWiki(this.wikiReference1.getName())).thenReturn(spacesWiki1Query);

        when(spacesWiki1Query.setWiki(this.wikiReference2.getName())).thenReturn(spacesWiki2Query);

        when(this.queryManager.getNamedQuery("getSpaceDocsName")).thenReturn(documentsSpace11Query);

        when(documentsSpace11Query.setWiki(any(String.class))).thenReturn(documentsSpace11Query);

        EntityReferenceSerializer<String> localEntityReferenceSerializer =
            this.componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(documentsSpace11Query.bindValue("space", localEntityReferenceSerializer.serialize(this.spaceReference11)))
            .thenReturn(documentsSpace11Query);
        when(documentsSpace11Query.bindValue("space", localEntityReferenceSerializer.serialize(this.spaceReference12)))
            .thenReturn(documentsSpace12Query);
        when(documentsSpace11Query.bindValue("space", localEntityReferenceSerializer.serialize(this.spaceReference13)))
            .thenReturn(documentsSpace13Query);

        // Spaces in wikis.
        when(spacesWiki1Query.execute()).thenReturn(
            List.of(localEntityReferenceSerializer.serialize(this.spaceReference11),
                localEntityReferenceSerializer.serialize(this.spaceReference12),
                localEntityReferenceSerializer.serialize(this.spaceReference13)));

        when(spacesWiki2Query.execute()).thenReturn(List.of());

        // space 11
        when(documentsSpace11Query.execute()).thenReturn(
            List.of(this.classReference111.getName(), this.documentReference112.getName(),
                this.documentReference113.getName()));

        // document 111
        when(this.xwiki.getDocument(eq(this.documentReference111), any(XWikiContext.class))).thenReturn(this.xdocument111);

        when(this.xdocument111.getXClass()).thenReturn(this.xclass111);

        // document 112
        when(this.xwiki.getDocument(eq(this.documentReference112), any(XWikiContext.class))).thenReturn(this.xdocument112);

        when(this.xdocument112.getXObjects()).thenReturn(Map.of());

        when(this.xdocument112.getTranslationLocales(any(XWikiContext.class))).thenReturn(List.of());

        // document 113
        when(this.xwiki.getDocument(eq(this.documentReference113), any(XWikiContext.class))).thenReturn(this.xdocument113);

        when(this.xdocument113.getAttachmentList()).thenReturn(List.of());

        when(this.xdocument113.getXObjects()).thenReturn(Map.of());

        when(this.xdocument113.getTranslationLocales(any(XWikiContext.class))).thenReturn(List.of(new Locale("ro")));

        // space 12
        when(documentsSpace12Query.execute()).thenReturn(
            List.of(this.documentReference121.getName(), this.documentReference122.getName()));

        // document 121
        when(this.xwiki.getDocument(eq(this.documentReference121), any(XWikiContext.class))).thenReturn(this.xdocument121);

        when(this.xdocument121.getAttachmentList()).thenReturn(List.of(this.xattachment1211, this.xattachment1212));

        when(this.xattachment1211.getReference()).thenReturn(this.attachmentReference1211);

        when(this.xattachment1212.getReference()).thenReturn(this.attachmentReference1212);

        when(this.xdocument121.getXObjects()).thenReturn(Map.of());

        when(this.xdocument121.getTranslationLocales(any(XWikiContext.class))).thenReturn(List.of());

        // document 122
        when(this.xwiki.getDocument(eq(this.documentReference122), any(XWikiContext.class))).thenReturn(this.xdocument122);

        when(this.xdocument122.getAttachmentList()).thenReturn(List.of());

        Map<DocumentReference, List<BaseObject>> xObjects = new HashMap<DocumentReference, List<BaseObject>>();
        // Yes, it seems that we can have null objects for some reason.
        xObjects.put(this.classReference111, Arrays.asList(null, this.xobject1221, this.xobject1222));
        when(this.xdocument122.getXObjects()).thenReturn(xObjects);

        when(this.xdocument122.getTranslationLocales(any(XWikiContext.class))).thenReturn(List.of());

        // object 1221
        when(this.xdocument122.getXObject(this.objectReference1221)).thenReturn(this.xobject1221);
        when(this.xdocument122.getXObject((EntityReference) this.objectReference1221)).thenReturn(this.xobject1221);

        when(this.xobject1221.getReference()).thenReturn(this.objectReference1221);

        when(this.xobject1221.getXClass(any(XWikiContext.class))).thenReturn(this.xclass111);

        when(this.xobject1221.getFieldList()).thenReturn(List.of());

        // object 1222
        when(this.xdocument122.getXObject(this.objectReference1222)).thenReturn(this.xobject1222);
        when(this.xdocument122.getXObject((EntityReference) this.objectReference1222)).thenReturn(this.xobject1222);

        when(this.xobject1222.getReference()).thenReturn(this.objectReference1222);

        when(this.xobject1222.getXClass(any(XWikiContext.class))).thenReturn(this.xclass111);

        when(this.xobject1222.getFieldList()).thenReturn(
            List.of(this.xproperty12221, this.xproperty12223));

        // object 1222 fields
        when(this.xproperty12221.getReference()).thenReturn(this.propertyReference12221);
        when(this.xproperty12221.getName()).thenReturn(this.propertyReference12221.getName());

        when(this.xproperty12223.getReference()).thenReturn(this.propertyReference12223);
        when(this.xproperty12223.getName()).thenReturn(this.propertyReference12223.getName());

        // class 111 fields
        when(this.xclass111.get(this.propertyReference12221.getName())).thenReturn(null);

        when(this.xclass111.get(this.propertyReference12223.getName())).thenReturn(null);

        // space 13
        when(documentsSpace13Query.execute()).thenReturn(List.of());
    }

    // getReferences
    @Test
    void getReferencesFarm() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(null);

        assertNotNull(result);
        assertThat(result, Matchers.<EntityReference>iterableWithSize(12));

        assertThat(
            result,
            containsInAnyOrder(this.classReference111, this.documentReference112, this.documentReference113,
                this.documentReference113RO, this.documentReference121, this.attachmentReference1211,
                this.attachmentReference1212, this.documentReference122, this.objectReference1221,
                this.objectReference1222, this.propertyReference12221, this.propertyReference12223));
    }

    @Test
    void getReferencesEmptyWiki() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(this.wikiReference2);

        assertNotNull(result);
        assertThat(result, Matchers.<EntityReference>iterableWithSize(0));
    }

    @Test
    void getReferencesWiki() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(this.wikiReference1);

        assertNotNull(result);
        assertThat(result, Matchers.<EntityReference>iterableWithSize(12));

        assertThat(
            result,
            containsInAnyOrder(this.classReference111, this.documentReference112, this.documentReference113,
                this.documentReference113RO, this.documentReference121, this.attachmentReference1211,
                this.attachmentReference1212, this.documentReference122, this.objectReference1221,
                this.objectReference1222, this.propertyReference12221, this.propertyReference12223));
    }

    @Test
    void getReferencesSpaceReference() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(this.spaceReference11);

        assertNotNull(result);
        assertThat(result, Matchers.<EntityReference>iterableWithSize(4));

        assertThat(
            result,
            containsInAnyOrder((EntityReference) this.classReference111, (EntityReference) this.documentReference112,
                (EntityReference) this.documentReference113, (EntityReference) this.documentReference113RO));
    }

    @Test
    void getReferencesEmptyDocument() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(this.documentReference112);

        assertNotNull(result);
        assertThat(result, Matchers.<EntityReference>iterableWithSize(1));

        assertEquals(this.documentReference112, result.iterator().next());
    }

    @Test
    void getReferencesTranslatedDocument() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(this.documentReference113);

        assertNotNull(result);
        assertThat(result, Matchers.<EntityReference>iterableWithSize(2));

        assertThat(result,
            containsInAnyOrder((EntityReference) this.documentReference113,
                (EntityReference) this.documentReference113RO));
    }

    @Test
    void getReferencesDocumentWithAttachments() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(this.documentReference121);

        assertNotNull(result);
        assertThat(result, Matchers.<EntityReference>iterableWithSize(3));

        assertThat(result, containsInAnyOrder(this.documentReference121, this.attachmentReference1211,
            this.attachmentReference1212));
    }

    @Test
    void getReferencesDocumentWithObjects() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(this.documentReference122);

        assertNotNull(result);
        assertThat(result, Matchers.<EntityReference>iterableWithSize(5));

        assertThat(
            result,
            containsInAnyOrder(this.documentReference122, this.objectReference1221, this.propertyReference12221,
                this.propertyReference12223, this.objectReference1222));
    }

    @Test
    void getReferencesAttachment() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(this.attachmentReference1211);

        assertNotNull(result);
        assertThat(result, Matchers.<EntityReference>iterableWithSize(1));

        assertEquals(this.attachmentReference1211, result.iterator().next());
    }

    @Test
    void getReferencesEmptyObject() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(this.objectReference1221);

        assertNotNull(result);
        assertThat(result, Matchers.<EntityReference>iterableWithSize(1));

        assertEquals(this.objectReference1221, result.iterator().next());
    }

    @Test
    void getReferencesObject() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(this.objectReference1222);

        assertNotNull(result);
        assertThat(result, Matchers.<EntityReference>iterableWithSize(3));

        assertThat(result, containsInAnyOrder(this.objectReference1222, this.propertyReference12221,
            this.propertyReference12223));
    }

    @Test
    void getReferencesProperty() throws Exception
    {
        Iterable<EntityReference> result = this.defaultSolrReferenceResolver.getReferences(this.propertyReference12221);

        assertNotNull(result);
        assertThat(result, Matchers.<EntityReference>iterableWithSize(1));

        assertEquals(this.propertyReference12221, result.iterator().next());
    }

    // getId

    @Test
    void getIdLocaleInReference() throws Exception
    {
        // Locale provided in the reference
        DocumentReference reference = new DocumentReference("wiki", "space", "name", Locale.ENGLISH);

        // Call
        String id = this.defaultSolrReferenceResolver.getId(reference);

        // Assert
        assertEquals("wiki:space.name_en", id);
    }

    @Test
    void getIdLocaleInDatabase() throws Exception
    {
        when(this.xdocument111.getRealLocale()).thenReturn(Locale.FRENCH);

        // Call
        String id = this.defaultSolrReferenceResolver.getId(this.documentReference111);

        // Assert and verify
        assertEquals(this.documentReference111 + "_", id);
    }

    // getQuery

    @Test
    void getQueryWiki() throws Exception
    {
        assertEquals("wiki:wiki1", this.defaultSolrReferenceResolver.getQuery(this.wikiReference1));
    }

    @Test
    void getQuerySpace() throws Exception
    {
        assertEquals("wiki:wiki1 AND space_exact:space11.data",
            this.defaultSolrReferenceResolver.getQuery(this.spaceReference11));
    }

    @Test
    void getQueryDocument() throws Exception
    {
        assertEquals("wiki:wiki1 AND space_exact:space11.data AND name_exact:class111",
            this.defaultSolrReferenceResolver.getQuery(this.documentReference111));
    }

    @Test
    void getQueryObject() throws Exception
    {
        assertEquals("wiki:wiki1" + " AND space_exact:space12.code AND name_exact:document122"
            + " AND class:space11.data.class111 AND number:0",
            this.defaultSolrReferenceResolver.getQuery(this.objectReference1221));
    }

    @Test
    void getQueryObjectProperty() throws Exception
    {
        assertEquals(
            "id:wiki1\\:space12.code.document122\\^space11.data.class111\\[1\\].aStringProperty",
            this.defaultSolrReferenceResolver.getQuery(this.propertyReference12221));
    }

    @Test
    void getQueryAttachment() throws Exception
    {
        assertEquals("id:wiki1\\:space12.code.document121@attachment1211.ext",
            this.defaultSolrReferenceResolver.getQuery(this.attachmentReference1211));
    }
}
