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
package org.xwiki.search.solr.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;

import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
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
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.CompactWikiStringEntityReferenceSerializer;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceValueProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedEntityReferenceValueProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedStringDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.objects.BaseObject;
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
DefaultStringEntityReferenceSerializer.class})
@MockingRequirement(DefaultIndexableReferenceExtractor.class)
public class DefaultIndexableReferenceExtractorTest extends
    AbstractMockingComponentTestCase<IndexableReferenceExtractor>
{
    DefaultIndexableReferenceExtractor referenceExtractor;

    private XWikiContext xwikiContext;

    private XWiki xwiki;

    WikiReference wiki1 = new WikiReference("xwiki1");

    SpaceReference space11 = new SpaceReference("Code", wiki1);

    DocumentReference class111 = new DocumentReference("SomeClass", space11);

    BaseClass xwikiClass111 = null;

    DocumentReference document112 = new DocumentReference("SomeSheet", space11);

    XWikiDocument xwikiDocument11x = null;

    DocumentReference document113 = new DocumentReference("SomeTranslatedDocument", space11);

    DocumentReference document113Translated =
        new DocumentReference("SomeTranslatedDocument", space11, new Locale("ro"));

    XWikiDocument xwikiDocument113 = null;

    SpaceReference space12 = new SpaceReference("Main", wiki1);

    DocumentReference document121 = new DocumentReference("WebHome", space12);

    AttachmentReference attachment1211 = new AttachmentReference("picture.png", document121);

    AttachmentReference attachment1212 = new AttachmentReference("document.doc", document121);

    XWikiDocument xwikiDocument121 = null;

    DocumentReference document122 = new DocumentReference("Test", space12);

    ObjectReference object1221 = new ObjectReference("xwiki1:Code.SomeClass[0]", document122);

    BaseObject xwikiObject1221 = null;

    ObjectReference object1222 = new ObjectReference("xwiki1:Code.SomeClass[1]", document122);

    ObjectPropertyReference property12221 = new ObjectPropertyReference("aStringProperty", object1222);

    StringProperty xwikiProperty12221 = null;

    ObjectPropertyReference passwordProperty12222 = new ObjectPropertyReference("aPasswordProperty", object1222);

    StringProperty xwikiPasswordProperty12222 = null;

    ObjectPropertyReference property12223 = new ObjectPropertyReference("anIntegerProperty", object1222);

    IntegerProperty xwikiProperty12223 = null;

    BaseObject xwikiObject1222 = null;

    XWikiDocument xwikiDocument122 = null;

    SpaceReference space13 = new SpaceReference("EmptySpaceThatShouldNotExist", wiki1);

    WikiReference wiki2 = new WikiReference("xwiki2");

    QueryManager queryManager;

    @Before
    public void configure() throws Exception
    {
        this.referenceExtractor = (DefaultIndexableReferenceExtractor) getMockedComponent();

        // Using ClassImposteriser to be able to mock classes like XWiki and XWikiDocument
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);

        Utils.setComponentManager(getComponentManager());

        // XWikiContext and XWiki

        final Execution execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext executionContext = new ExecutionContext();

        this.xwiki = getMockery().mock(XWiki.class);

        this.xwikiContext = new XWikiContext();
        this.xwikiContext.setDatabase("xwiki");
        this.xwikiContext.setWiki(this.xwiki);

        executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.xwikiContext);

        // XWiki model data
        xwikiDocument11x = getMockery().mock(XWikiDocument.class, "document11x");
        xwikiClass111 = getMockery().mock(BaseClass.class);
        xwikiDocument113 = getMockery().mock(XWikiDocument.class, "document113");
        xwikiDocument121 = getMockery().mock(XWikiDocument.class, "document121");
        xwikiDocument122 = getMockery().mock(XWikiDocument.class, "document122");
        xwikiObject1221 = getMockery().mock(BaseObject.class, "object1221");
        xwikiProperty12221 = getMockery().mock(StringProperty.class, "property12221");
        xwikiPasswordProperty12222 = getMockery().mock(StringProperty.class, "passwordProperty12222");
        xwikiProperty12223 = getMockery().mock(IntegerProperty.class, "property12223");
        xwikiObject1222 = getMockery().mock(BaseObject.class, "object1222");
        final EntityReferenceSerializer<String> serializer =
            getComponentManager().getInstance(
                new DefaultParameterizedType(null, EntityReferenceSerializer.class, String.class));

        queryManager = getComponentManager().getInstance(QueryManager.class);
        final Query spacesWiki1Query = getMockery().mock(DefaultQuery.class, "getSpacesWiki1");
        final Query documentsSpace11Query = getMockery().mock(DefaultQuery.class, "getSpaceDocsNameSpace11");
        final Query documentsSpace12Query = getMockery().mock(DefaultQuery.class, "getSpaceDocsNameSpace12");
        final Query documentsSpace13Query = getMockery().mock(DefaultQuery.class, "getSpaceDocsNameSpace13");
        final Query spacesWiki2Query = getMockery().mock(DefaultQuery.class, "getSpacesWiki2");

        getMockery().checking(new Expectations()
        {
            {
                allowing(execution).getContext();
                will(returnValue(executionContext));

                // ignoring(any(Logger.class));

                // Data

                allowing(xwiki).exists(with(any(DocumentReference.class)), with(xwikiContext));
                will(returnValue(true));

                allowing(referenceExtractor.documentAccessBridge).exists(with(any(DocumentReference.class)));
                will(returnValue(true));

                // Query manager and specific queries mocking.

                allowing(queryManager).getNamedQuery("getSpaces");
                will(returnValue(spacesWiki1Query));

                allowing(spacesWiki1Query).setWiki(wiki1.getName());
                will(returnValue(spacesWiki1Query));

                allowing(spacesWiki1Query).setWiki(wiki2.getName());
                will(returnValue(spacesWiki2Query));

                allowing(queryManager).getNamedQuery("getSpaceDocsName");
                will(returnValue(documentsSpace11Query));

                allowing(documentsSpace11Query).setWiki(with(any(String.class)));
                will(returnValue(documentsSpace11Query));

                allowing(documentsSpace11Query).bindValue("space", space11.getName());
                will(returnValue(documentsSpace11Query));

                allowing(documentsSpace11Query).bindValue("space", space12.getName());
                will(returnValue(documentsSpace12Query));

                allowing(documentsSpace11Query).bindValue("space", space13.getName());
                will(returnValue(documentsSpace13Query));

                // Spaces in wikis.
                allowing(spacesWiki1Query).execute();
                will(returnValue(Arrays.asList(space11.getName(), space12.getName(), space13.getName())));

                allowing(spacesWiki2Query).execute();
                will(returnValue(Collections.emptyList()));

                // space 11
                allowing(documentsSpace11Query).execute();
                will(returnValue(Arrays.asList(class111.getName(), document112.getName(), document113.getName())));

                // document 111
                allowing(xwiki).getDocument(class111, xwikiContext);
                will(returnValue(xwikiDocument11x));

                allowing(referenceExtractor.documentAccessBridge).getAttachmentReferences(class111);
                will(returnValue(Collections.emptyList()));

                allowing(xwikiDocument11x).getXClass();
                will(returnValue(xwikiClass111));

                // document 112
                allowing(xwiki).getDocument(document112, xwikiContext);
                will(returnValue(xwikiDocument11x));

                allowing(referenceExtractor.documentAccessBridge).getAttachmentReferences(document112);
                will(returnValue(Collections.emptyList()));

                allowing(xwikiDocument11x).getXObjects();
                will(returnValue(Collections.emptyMap()));

                allowing(xwikiDocument11x).getTranslationList(xwikiContext);
                will(returnValue(Collections.emptyList()));

                // document 113
                allowing(xwiki).getDocument(document113, xwikiContext);
                will(returnValue(xwikiDocument113));

                allowing(referenceExtractor.documentAccessBridge).getAttachmentReferences(document113);
                will(returnValue(Collections.emptyList()));

                allowing(xwikiDocument113).getXObjects();
                will(returnValue(Collections.emptyMap()));

                allowing(xwikiDocument113).getTranslationList(xwikiContext);
                will(returnValue(Arrays.asList("ro")));

                // space 12
                allowing(documentsSpace12Query).execute();
                will(returnValue(Arrays.asList(document121.getName(), document122.getName())));

                // document 121
                allowing(xwiki).getDocument(document121, xwikiContext);
                will(returnValue(xwikiDocument121));

                allowing(referenceExtractor.documentAccessBridge).getAttachmentReferences(document121);
                will(returnValue(Arrays.asList(attachment1211, attachment1212)));

                allowing(xwikiDocument121).getXObjects();
                will(returnValue(Collections.emptyMap()));

                allowing(xwikiDocument121).getTranslationList(xwikiContext);
                will(returnValue(Collections.emptyList()));

                // document 122
                allowing(xwiki).getDocument(document122, xwikiContext);
                will(returnValue(xwikiDocument122));

                allowing(referenceExtractor.documentAccessBridge).getAttachmentReferences(document122);
                will(returnValue(Collections.emptyList()));

                allowing(xwikiDocument122).getXObjects();
                Map<DocumentReference, List<BaseObject>> xObjects = new HashMap<DocumentReference, List<BaseObject>>();
                // Yes, it seems that we can have null objects for some reason.
                xObjects.put(class111, Arrays.asList(null, xwikiObject1221, xwikiObject1222));
                will(returnValue(xObjects));

                allowing(xwikiDocument122).getTranslationList(xwikiContext);
                will(returnValue(Collections.emptyList()));

                // object 1221
                allowing(xwikiDocument122).getXObject(object1221);
                will(returnValue(xwikiObject1221));

                allowing(xwikiObject1221).getReference();
                will(returnValue(object1221));

                allowing(xwikiObject1221).getXClass(xwikiContext);
                will(returnValue(xwikiClass111));

                allowing(xwikiObject1221).getFieldList();
                will(returnValue(Collections.emptyList()));

                // object 1222
                allowing(xwikiDocument122).getXObject(object1222);
                will(returnValue(xwikiObject1222));

                allowing(xwikiObject1222).getReference();
                will(returnValue(object1222));

                allowing(xwikiObject1222).getXClass(xwikiContext);
                will(returnValue(xwikiClass111));

                allowing(xwikiObject1222).getFieldList();
                will(returnValue(Arrays.asList(xwikiProperty12221, xwikiPasswordProperty12222, xwikiProperty12223)));

                // object 1222 fields
                allowing(xwikiProperty12221).getReference();
                will(returnValue(property12221));
                allowing(xwikiProperty12221).getName();
                will(returnValue(property12221.getName()));

                allowing(xwikiPasswordProperty12222).getReference();
                will(returnValue(passwordProperty12222));
                allowing(xwikiPasswordProperty12222).getName();
                will(returnValue(passwordProperty12222.getName()));

                allowing(xwikiProperty12223).getReference();
                will(returnValue(property12223));
                allowing(xwikiProperty12223).getName();
                will(returnValue(property12223.getName()));

                // class 111 fields
                allowing(xwikiClass111).get(property12221.getName());
                will(returnValue(null));

                allowing(xwikiClass111).get(passwordProperty12222.getName());
                PasswordClass passWordClass = new PasswordClass();
                will(returnValue(passWordClass));

                allowing(xwikiClass111).get(property12223.getName());
                will(returnValue(null));

                // space 13
                allowing(documentsSpace13Query).execute();
                will(returnValue(Collections.emptyList()));
            }
        });
    }

    @Test
    public void testEmptyWiki() throws Exception
    {
        List<EntityReference> result = referenceExtractor.getReferences(wiki2);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testWiki() throws Exception
    {
        List<EntityReference> result = referenceExtractor.getReferences(wiki1);
        Assert.assertNotNull(result);
        Assert.assertEquals(12, result.size());

        assertThat(
            result,
            containsInAnyOrder(class111, document112, document113, document113Translated, document121, attachment1211,
                attachment1212, document122, object1221, object1222, property12221, property12223));
        Assert.assertFalse(result.contains(passwordProperty12222));
    }

    @Test
    public void testEmptySpace() throws Exception
    {
        List<EntityReference> result = referenceExtractor.getReferences(space13);

        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testSpaceReference() throws Exception
    {
        List<EntityReference> result = referenceExtractor.getReferences(space11);
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
        List<EntityReference> result = referenceExtractor.getReferences(document112);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());

        Assert.assertTrue(result.contains(document112));
    }

    @Test
    public void testTranslatedDocument() throws Exception
    {
        List<EntityReference> result = referenceExtractor.getReferences(document113);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());

        assertThat(result, containsInAnyOrder((EntityReference) document113, (EntityReference) document113Translated));
    }

    @Test
    public void testDocumentWithAttachments() throws Exception
    {
        List<EntityReference> result = referenceExtractor.getReferences(document121);

        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());

        assertThat(result, containsInAnyOrder(document121, attachment1211, attachment1212));
    }

    @Test
    public void testDocumentWithObjects() throws Exception
    {
        List<EntityReference> result = referenceExtractor.getReferences(document122);

        Assert.assertNotNull(result);
        Assert.assertEquals(5, result.size());

        assertThat(result, containsInAnyOrder(document122, object1221, property12221, property12223, object1222));
        Assert.assertFalse(result.contains(passwordProperty12222));
    }

    @Test
    public void testAttachment() throws Exception
    {
        List<EntityReference> result = referenceExtractor.getReferences(attachment1211);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());

        Assert.assertTrue(result.contains(attachment1211));
    }

    @Test
    public void testEmptyObject() throws Exception
    {
        List<EntityReference> result = referenceExtractor.getReferences(object1221);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());

        Assert.assertTrue(result.contains(object1221));
    }

    @Test
    public void testObject() throws Exception
    {
        List<EntityReference> result = referenceExtractor.getReferences(object1222);
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());

        assertThat(result, containsInAnyOrder(object1222, property12221, property12223));
        Assert.assertFalse(result.contains(passwordProperty12222));
    }

    @Test
    public void testProperty() throws Exception
    {
        List<EntityReference> result = referenceExtractor.getReferences(property12221);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());

        Assert.assertTrue(result.contains(property12221));
    }

    @Test
    public void testRestrictedProperty() throws Exception
    {
        List<EntityReference> result = referenceExtractor.getReferences(passwordProperty12222);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }
}
