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
package org.xwiki.search.solr.internal.resolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.DefaultModelContext;
import org.xwiki.model.internal.reference.DefaultEntityReferenceValueProvider;
import org.xwiki.model.internal.reference.DefaultStringDocumentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.search.solr.internal.reference.AttachmentSolrDocumentReferenceResolver;
import org.xwiki.search.solr.internal.reference.DefaultSolrDocumentReferenceResolver;
import org.xwiki.search.solr.internal.reference.DocumentSolrDocumentReferenceResolver;
import org.xwiki.search.solr.internal.reference.ObjectPropertySolrDocumentReferenceResolver;
import org.xwiki.search.solr.internal.reference.ObjectSolrDocumentReferenceResolver;
import org.xwiki.search.solr.internal.reference.SolrDocumentReferenceResolver;
import org.xwiki.search.solr.internal.reference.SpaceSolrDocumentReferenceResolver;
import org.xwiki.search.solr.internal.reference.WikiSolrDocumentReferenceResolver;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.CompactWikiStringEntityReferenceSerializer;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceValueProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedEntityReferenceValueProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedStringDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.web.Utils;

/**
 * Tests for the extraction of indexable references from a start reference.
 * 
 * @version $Id$
 */
@ComponentList({DefaultModelContext.class, DefaultModelConfiguration.class, LocalStringEntityReferenceSerializer.class,
RelativeStringEntityReferenceResolver.class, CurrentReferenceDocumentReferenceResolver.class,
CurrentReferenceEntityReferenceResolver.class, CurrentEntityReferenceValueProvider.class,
CurrentMixedStringDocumentReferenceResolver.class, CurrentMixedEntityReferenceValueProvider.class,
DefaultEntityReferenceValueProvider.class, CompactWikiStringEntityReferenceSerializer.class,
DefaultStringDocumentReferenceResolver.class, DefaultStringEntityReferenceResolver.class,
DefaultStringEntityReferenceSerializer.class, DefaultExecution.class, AttachmentSolrDocumentReferenceResolver.class,
DefaultSolrDocumentReferenceResolver.class, DocumentSolrDocumentReferenceResolver.class,
ObjectPropertySolrDocumentReferenceResolver.class, ObjectSolrDocumentReferenceResolver.class,
SpaceSolrDocumentReferenceResolver.class, WikiSolrDocumentReferenceResolver.class})
public class SolrDocumenrReferenceResolverTest
{
    @Rule
    public final MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    private SolrDocumentReferenceResolver defaultReferenceResolver;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private WikiReference wiki1 = new WikiReference("xwiki1");

    private SpaceReference space11 = new SpaceReference("Code", wiki1);

    private DocumentReference class111 = new DocumentReference("SomeClass", space11);

    private BaseClass xwikiClass111;

    private DocumentReference document112 = new DocumentReference("SomeSheet", space11);

    private XWikiDocument xwikiDocument11x;

    private DocumentReference document113 = new DocumentReference("SomeTranslatedDocument", space11);

    private DocumentReference document113Translated = new DocumentReference("SomeTranslatedDocument", space11,
        new Locale("ro"));

    private XWikiDocument xwikiDocument113 = null;

    private SpaceReference space12 = new SpaceReference("Main", wiki1);

    private DocumentReference document121 = new DocumentReference("WebHome", space12);

    private AttachmentReference attachment1211 = new AttachmentReference("picture.png", document121);

    private XWikiAttachment xwikiAttachment1211;

    private AttachmentReference attachment1212 = new AttachmentReference("document.doc", document121);

    private XWikiAttachment xwikiAttachment1212;

    private XWikiDocument xwikiDocument121;

    private DocumentReference document122 = new DocumentReference("Test", space12);

    private BaseObjectReference object1221;

    private BaseObject xwikiObject1221;

    private BaseObjectReference object1222;

    private ObjectPropertyReference property12221;

    private StringProperty xwikiProperty12221;

    private ObjectPropertyReference passwordProperty12222;

    private StringProperty xwikiPasswordProperty12222;

    private ObjectPropertyReference property12223;

    private IntegerProperty xwikiProperty12223;

    private BaseObject xwikiObject1222;

    private XWikiDocument xwikiDocument122;

    private SpaceReference space13 = new SpaceReference("EmptySpaceThatShouldNotExist", wiki1);

    private WikiReference wiki2 = new WikiReference("xwiki2");

    private QueryManager queryManager;

    @BeforeComponent
    public void registerComponents() throws Exception
    {
        this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        this.mocker.registerMockComponent(QueryManager.class);
    }

    @Before
    public void setUp() throws Exception
    {
        this.defaultReferenceResolver = this.mocker.getInstance(SolrDocumentReferenceResolver.class);

        Utils.setComponentManager(this.mocker);

        // XWiki

        this.xwiki = mock(XWiki.class);

        // XWikiContext

        this.xcontext = new XWikiContext();
        this.xcontext.setDatabase("xwiki");
        this.xcontext.setWiki(this.xwiki);

        // XWikiContext Provider

        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);

        // XWikiContext trough Execution

        Execution execution = this.mocker.getInstance(Execution.class);
        execution.setContext(new ExecutionContext());
        execution.getContext().setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.xcontext);

        // References

        this.object1221 = new BaseObjectReference(this.class111, 0, this.document122);
        this.object1222 = new BaseObjectReference(this.class111, 1, this.document122);
        this.property12221 = new ObjectPropertyReference("aStringProperty", object1222);
        this.property12223 = new ObjectPropertyReference("anIntegerProperty", object1222);
        this.passwordProperty12222 = new ObjectPropertyReference("aPasswordProperty", object1222);

        // XWiki model data

        this.xwikiDocument11x = mock(XWikiDocument.class, "xwikiDocument11x");
        this.xwikiClass111 = mock(BaseClass.class);
        this.xwikiDocument113 = mock(XWikiDocument.class, "xwikiDocument113");
        this.xwikiDocument121 = mock(XWikiDocument.class, "xwikiDocument121");
        this.xwikiAttachment1211 = mock(XWikiAttachment.class, "xwikiAttachment1211");
        this.xwikiAttachment1212 = mock(XWikiAttachment.class, "xwikiAttachment1212");
        this.xwikiDocument122 = mock(XWikiDocument.class, "xwikiDocument122");
        this.xwikiObject1221 = mock(BaseObject.class, "xwikiObject1221");
        this.xwikiProperty12221 = mock(StringProperty.class, "xwikiProperty12221");
        this.xwikiPasswordProperty12222 = mock(StringProperty.class, "xwikiPasswordProperty12222");
        this.xwikiProperty12223 = mock(IntegerProperty.class, "xwikiProperty12223");
        this.xwikiObject1222 = mock(BaseObject.class, "xwikiObject1222");

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

        when(spacesWiki1Query.setWiki(wiki1.getName())).thenReturn(spacesWiki1Query);

        when(spacesWiki1Query.setWiki(wiki2.getName())).thenReturn(spacesWiki2Query);

        when(queryManager.getNamedQuery("getSpaceDocsName")).thenReturn(documentsSpace11Query);

        when(documentsSpace11Query.setWiki(any(String.class))).thenReturn(documentsSpace11Query);

        when(documentsSpace11Query.bindValue("space", space11.getName())).thenReturn(documentsSpace11Query);

        when(documentsSpace11Query.bindValue("space", space12.getName())).thenReturn(documentsSpace12Query);

        when(documentsSpace11Query.bindValue("space", space13.getName())).thenReturn(documentsSpace13Query);

        // Spaces in wikis.
        when(spacesWiki1Query.execute()).thenReturn(
            Arrays.<Object> asList(space11.getName(), space12.getName(), space13.getName()));

        when(spacesWiki2Query.execute()).thenReturn(Collections.EMPTY_LIST);

        // space 11
        when(documentsSpace11Query.execute()).thenReturn(
            Arrays.<Object> asList(class111.getName(), document112.getName(), document113.getName()));

        // document 111
        when(xwiki.getDocument(eq(class111), any(XWikiContext.class))).thenReturn(xwikiDocument11x);

        /*
         * when(referenceExtractor.documentAccessBridge.getAttachmentReferences(class111)).thenReturn(
         * Collections.EMPTY_LIST);
         */

        when(xwikiDocument11x.getXClass()).thenReturn(xwikiClass111);

        // document 112
        when(xwiki.getDocument(eq(document112), any(XWikiContext.class))).thenReturn(xwikiDocument11x);

        /*
         * when(referenceExtractor.documentAccessBridge.getAttachmentReferences(document112)).thenReturn(
         * Collections.EMPTY_LIST);
         */

        when(xwikiDocument11x.getXObjects()).thenReturn(Collections.EMPTY_MAP);

        when(xwikiDocument11x.getTranslationList(any(XWikiContext.class))).thenReturn(Collections.EMPTY_LIST);

        // document 113
        when(xwiki.getDocument(eq(document113), any(XWikiContext.class))).thenReturn(xwikiDocument113);

        when(xwikiDocument113.getAttachmentList()).thenReturn(Collections.EMPTY_LIST);

        when(xwikiDocument113.getXObjects()).thenReturn(Collections.EMPTY_MAP);

        when(xwikiDocument113.getTranslationList(any(XWikiContext.class))).thenReturn(Arrays.asList("ro"));

        // space 12
        when(documentsSpace12Query.execute()).thenReturn(
            Arrays.<Object> asList(document121.getName(), document122.getName()));

        // document 121
        when(xwiki.getDocument(eq(document121), any(XWikiContext.class))).thenReturn(xwikiDocument121);

        when(xwikiDocument121.getAttachmentList()).thenReturn(Arrays.asList(xwikiAttachment1211, xwikiAttachment1212));

        when(xwikiAttachment1211.getReference()).thenReturn(attachment1211);

        when(xwikiAttachment1212.getReference()).thenReturn(attachment1212);

        when(xwikiDocument121.getXObjects()).thenReturn(Collections.EMPTY_MAP);

        when(xwikiDocument121.getTranslationList(any(XWikiContext.class))).thenReturn(Collections.EMPTY_LIST);

        // document 122
        when(xwiki.getDocument(eq(document122), any(XWikiContext.class))).thenReturn(xwikiDocument122);

        when(xwikiDocument122.getAttachmentList()).thenReturn(Collections.EMPTY_LIST);

        Map<DocumentReference, List<BaseObject>> xObjects = new HashMap<DocumentReference, List<BaseObject>>();
        // Yes, it seems that we can have null objects for some reason.
        xObjects.put(class111, Arrays.asList(null, xwikiObject1221, xwikiObject1222));
        when(xwikiDocument122.getXObjects()).thenReturn(xObjects);

        when(xwikiDocument122.getTranslationList(any(XWikiContext.class))).thenReturn(Collections.EMPTY_LIST);

        // object 1221
        when(xwikiDocument122.getXObject(object1221)).thenReturn(xwikiObject1221);
        when(xwikiDocument122.getXObject((EntityReference) object1221)).thenReturn(xwikiObject1221);

        when(xwikiObject1221.getReference()).thenReturn(object1221);

        when(xwikiObject1221.getXClass(any(XWikiContext.class))).thenReturn(xwikiClass111);

        when(xwikiObject1221.getFieldList()).thenReturn(Collections.EMPTY_LIST);

        // object 1222
        when(xwikiDocument122.getXObject(object1222)).thenReturn(xwikiObject1222);
        when(xwikiDocument122.getXObject((EntityReference) object1222)).thenReturn(xwikiObject1222);

        when(xwikiObject1222.getReference()).thenReturn(object1222);

        when(xwikiObject1222.getXClass(any(XWikiContext.class))).thenReturn(xwikiClass111);

        when(xwikiObject1222.getFieldList()).thenReturn(
            Arrays.asList(xwikiProperty12221, xwikiPasswordProperty12222, xwikiProperty12223));

        // object 1222 fields
        when(xwikiProperty12221.getReference()).thenReturn(property12221);
        when(xwikiProperty12221.getName()).thenReturn(property12221.getName());

        when(xwikiPasswordProperty12222.getReference()).thenReturn(passwordProperty12222);
        when(xwikiPasswordProperty12222.getName()).thenReturn(passwordProperty12222.getName());

        when(xwikiProperty12223.getReference()).thenReturn(property12223);
        when(xwikiProperty12223.getName()).thenReturn(property12223.getName());

        // class 111 fields
        when(xwikiClass111.get(property12221.getName())).thenReturn(null);

        when(xwikiClass111.get(passwordProperty12222.getName())).thenReturn(new PasswordClass());

        when(xwikiClass111.get(property12223.getName())).thenReturn(null);

        // space 13
        when(documentsSpace13Query.execute()).thenReturn(Collections.EMPTY_LIST);
    }

    @Test
    public void testEmptyWiki() throws Exception
    {
        List<EntityReference> result = this.defaultReferenceResolver.getReferences(wiki2);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testWiki() throws Exception
    {
        List<EntityReference> result = this.defaultReferenceResolver.getReferences(wiki1);
        Assert.assertNotNull(result);

        assertThat(
            result,
            containsInAnyOrder(class111, document112, document113, document113Translated, document121, attachment1211,
                attachment1212, document122, object1221, object1222, property12221, property12223));
        Assert.assertFalse(result.contains(passwordProperty12222));
    }

    @Test
    public void testEmptySpace() throws Exception
    {
        List<EntityReference> result = this.defaultReferenceResolver.getReferences(space13);

        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testSpaceReference() throws Exception
    {
        List<EntityReference> result = this.defaultReferenceResolver.getReferences(space11);
        Assert.assertNotNull(result);
        Assert.assertEquals(4, result.size());

        assertThat(
            result,
            containsInAnyOrder((EntityReference) class111, (EntityReference) document112,
                (EntityReference) document113, (EntityReference) document113Translated));
        Assert.assertFalse(result.contains(passwordProperty12222));
    }

    @Test
    public void testEmptyDocument() throws Exception
    {
        List<EntityReference> result = this.defaultReferenceResolver.getReferences(document112);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());

        Assert.assertTrue(result.contains(document112));
    }

    @Test
    public void testTranslatedDocument() throws Exception
    {
        List<EntityReference> result = this.defaultReferenceResolver.getReferences(document113);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());

        assertThat(result, containsInAnyOrder((EntityReference) document113, (EntityReference) document113Translated));
    }

    @Test
    public void testDocumentWithAttachments() throws Exception
    {
        List<EntityReference> result = this.defaultReferenceResolver.getReferences(document121);

        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());

        assertThat(result, containsInAnyOrder(document121, attachment1211, attachment1212));
    }

    @Test
    public void testDocumentWithObjects() throws Exception
    {
        List<EntityReference> result = this.defaultReferenceResolver.getReferences(document122);

        Assert.assertNotNull(result);
        Assert.assertEquals(5, result.size());

        assertThat(result, containsInAnyOrder(document122, object1221, property12221, property12223, object1222));
        Assert.assertFalse(result.contains(passwordProperty12222));
    }

    @Test
    public void testAttachment() throws Exception
    {
        List<EntityReference> result = this.defaultReferenceResolver.getReferences(attachment1211);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());

        Assert.assertTrue(result.contains(attachment1211));
    }

    @Test
    public void testEmptyObject() throws Exception
    {
        List<EntityReference> result = this.defaultReferenceResolver.getReferences(object1221);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());

        Assert.assertTrue(result.contains(object1221));
    }

    @Test
    public void testObject() throws Exception
    {
        List<EntityReference> result = this.defaultReferenceResolver.getReferences(object1222);
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());

        assertThat(result, containsInAnyOrder(object1222, property12221, property12223));
        Assert.assertFalse(result.contains(passwordProperty12222));
    }

    @Test
    public void testProperty() throws Exception
    {
        List<EntityReference> result = this.defaultReferenceResolver.getReferences(property12221);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());

        Assert.assertTrue(result.contains(property12221));
    }

    @Test
    public void testRestrictedProperty() throws Exception
    {
        List<EntityReference> result = this.defaultReferenceResolver.getReferences(passwordProperty12222);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }
}
