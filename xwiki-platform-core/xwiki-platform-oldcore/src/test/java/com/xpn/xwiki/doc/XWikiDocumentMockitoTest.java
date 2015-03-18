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
package com.xpn.xwiki.doc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.VelocityContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.test.mockito.MockitoComponentManagerRule;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.web.EditForm;

/**
 * Unit tests for {@link XWikiDocument}.
 * 
 * @version $Id$
 */
public class XWikiDocumentMockitoTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    /**
     * The object being tested.
     */
    private XWikiDocument document;

    @Before
    public void setUp() throws Exception
    {
        this.oldcore.getMocker().registerMockComponent(EntityReferenceSerializer.TYPE_STRING);
        this.oldcore.getMocker().registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");

        // Activate programming rights in order to be able to call
        // com.xpn.xwiki.api.Document#getDocument().
        when(this.oldcore.getMockRightService().hasProgrammingRights(this.oldcore.getXWikiContext())).thenReturn(true);

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.oldcore.getMockXWiki().saveDocument(this.document, "", true, this.oldcore.getXWikiContext());
    }

    @Test
    public void getChildrenReferences() throws Exception
    {
        Query query = mock(Query.class);
        when(this.oldcore.getQueryManager().createQuery(anyString(), eq(Query.XWQL))).thenReturn(query);

        QueryFilter hiddenFilter = this.oldcore.getMocker().registerMockComponent(QueryFilter.class, "hidden");

        when(query.setLimit(7)).thenReturn(query);

        List<Object[]> result = Arrays.asList(new Object[] {"X", "y"}, new Object[] {"A", "b"});
        when(query.<Object[]>execute()).thenReturn(result);

        List<DocumentReference> childrenReferences =
            document.getChildrenReferences(7, 3, this.oldcore.getXWikiContext());

        verify(query).addFilter(hiddenFilter);
        verify(query).setLimit(7);
        verify(query).setOffset(3);

        Assert.assertEquals(2, childrenReferences.size());
        Assert.assertEquals(new DocumentReference("wiki", "X", "y"), childrenReferences.get(0));
        Assert.assertEquals(new DocumentReference("wiki", "A", "b"), childrenReferences.get(1));
    }

    /**
     * @see "XWIKI-8024: XWikiDocument#setAsContextDoc doesn't set the 'cdoc' in the Velocity context"
     */
    @Test
    public void setAsContextDoc() throws Exception
    {
        VelocityManager velocityManager = this.oldcore.getMocker().registerMockComponent(VelocityManager.class);
        VelocityContext velocityContext = mock(VelocityContext.class);
        when(velocityManager.getVelocityContext()).thenReturn(velocityContext);

        this.document.setAsContextDoc(this.oldcore.getXWikiContext());

        assertSame(this.document, this.oldcore.getXWikiContext().getDoc());

        ArgumentCaptor<Document> argument = ArgumentCaptor.forClass(Document.class);
        verify(velocityContext).put(eq("doc"), argument.capture());
        assertSame(this.document, argument.getValue().getDocument());
        verify(velocityContext).put(eq("tdoc"), argument.capture());
        assertSame(this.document, argument.getValue().getDocument());
        verify(velocityContext).put(eq("cdoc"), argument.capture());
        assertSame(this.document, argument.getValue().getDocument());
    }

    @Test
    public void setTranslationAsContextDoc() throws Exception
    {
        VelocityManager velocityManager = this.oldcore.getMocker().registerMockComponent(VelocityManager.class);
        VelocityContext velocityContext = mock(VelocityContext.class);
        when(velocityManager.getVelocityContext()).thenReturn(velocityContext);

        this.document.setLocale(Locale.US);
        XWikiDocument defaultTranslation = new XWikiDocument(this.document.getDocumentReference());
        when(
            this.oldcore.getMockXWiki().getDocument(this.document.getDocumentReference(),
                this.oldcore.getXWikiContext())).thenReturn(defaultTranslation);

        this.document.setAsContextDoc(this.oldcore.getXWikiContext());

        assertSame(this.document, this.oldcore.getXWikiContext().getDoc());

        ArgumentCaptor<Document> argument = ArgumentCaptor.forClass(Document.class);
        verify(velocityContext).put(eq("doc"), argument.capture());
        assertSame(defaultTranslation, argument.getValue().getDocument());
        verify(velocityContext).put(eq("tdoc"), argument.capture());
        assertSame(this.document, argument.getValue().getDocument());
        verify(velocityContext).put(eq("cdoc"), argument.capture());
        assertSame(this.document, argument.getValue().getDocument());
    }

    /**
     * Generate a fake map for the request used in the tests of {@link #readObjectsFromForm()} and
     * {@link #readObjectsFromFormUpdateOrCreate()}.
     * 
     * @return Map of fake parameters which should test every cases
     */
    private Map<String, String[]> generateFakeRequestMap()
    {
        Map<String, String[]> parameters = new HashMap<>();
        // Testing update of values in existing object with existing properties
        String[] string1 = {"string1"};
        parameters.put("space.page_0_string", string1);
        String[] int1 = {"7"};
        parameters.put("space.page_1_int", int1);
        // Testing creation and update of an object's properties when object
        // doesn't exist
        String[] string2 = {"string2"};
        String[] int2 = {"13"};
        parameters.put("space.page_2_string", string2);
        parameters.put("space.page_2_int", int2);
        // Testing that objects with non-following number is not created
        parameters.put("space.page_42_string", string1);
        parameters.put("space.page_42_int", int1);
        // Testing that invalid parameter are ignored
        parameters.put("invalid", new String[] {"whatever"});
        // Testing that invalid xclass page are ignored
        parameters.put("InvalidSpace.InvalidPage_0_string", new String[] {"whatever"});
        // Testing that an invalid number is ignored (first should be ignored by
        // regexp parser, second by an exception)
        parameters.put("space.page_notANumber_string", new String[] {"whatever"});
        parameters.put("space.page_9999999999_string", new String[] {"whatever"});
        return parameters;
    }

    /**
     * Generate the fake class that is used for the test of {@link #readObjectsFromForm()} and
     * {@link #readObjectsFromFormUpdateOrCreate()}.
     * 
     * @return The fake BaseClass
     */
    private BaseClass generateFakeClass()
    {
        BaseClass baseClass = this.document.getXClass();
        baseClass.addTextField("string", "String", 30);
        baseClass.addTextAreaField("area", "Area", 10, 10);
        baseClass.addTextAreaField("puretextarea", "Pure text area", 10, 10);
        // set the text areas an non interpreted content
        ((TextAreaClass) baseClass.getField("puretextarea")).setContentType("puretext");
        baseClass.addPasswordField("passwd", "Password", 30);
        baseClass.addBooleanField("boolean", "Boolean", "yesno");
        baseClass.addNumberField("int", "Int", 10, "integer");
        baseClass.addStaticListField("stringlist", "StringList", "value1, value2");

        return baseClass;
    }

    /**
     * Generate 2 clones of a fake object in the document
     * 
     * @return Return the reference of the first clone
     */
    private void generateFakeObjects()
    {
        BaseObject baseObject = null, baseObject2 = null;
        try {
            baseObject = this.document.newXObject(this.document.getDocumentReference(), this.oldcore.getXWikiContext());
            baseObject2 =
                this.document.newXObject(this.document.getDocumentReference(), this.oldcore.getXWikiContext());
        } catch (XWikiException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        baseObject.setStringValue("string", "string");
        baseObject.setIntValue("int", 42);
        baseObject2.setStringValue("string", "string");
        baseObject2.setIntValue("int", 42);
    }

    /**
     * Unit test for {@link XWikiDocument#readObjectsFromForm(EditForm, XWikiContext)}.
     */
    @Test
    public void readObjectsFromForm() throws Exception
    {
        HttpServletRequest request = mock(HttpServletRequest.class);
        MockitoComponentManagerRule mocker = this.oldcore.getMocker();
        XWiki wiki = this.oldcore.getMockXWiki();
        XWikiContext context = this.oldcore.getXWikiContext();
        DocumentReferenceResolver<String> documentReferenceResolverString =
            mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        // Entity Reference resolver is used in <BaseObject>.getXClass()
        DocumentReferenceResolver<EntityReference> documentReferenceResolverEntity =
            mocker.registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        EntityReferenceSerializer<String> entityReferenceResolver =
            mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");

        Map<String, String[]> parameters = generateFakeRequestMap();
        BaseClass baseClass = generateFakeClass();
        generateFakeObjects();

        when(request.getParameterMap()).thenReturn(parameters);

        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        // This entity resolver with this 'resolve' method is used in
        // <BaseCollection>.getXClassReference()
        when(documentReferenceResolverEntity.resolve(any(EntityReference.class), any(DocumentReference.class)))
            .thenReturn(this.document.getDocumentReference());
        when(documentReferenceResolverString.resolve("space.page")).thenReturn(documentReference);
        when(entityReferenceResolver.serialize(any(EntityReference.class))).thenReturn("space.page");

        EditForm eform = new EditForm();
        eform.setRequest(request);
        document.readObjectsFromForm(eform, context);

        assertEquals(2, this.document.getXObjectSize(baseClass.getDocumentReference()));
        assertEquals("string", this.document.getXObject(baseClass.getDocumentReference(), 0).getStringValue("string"));
        assertEquals(42, this.document.getXObject(baseClass.getDocumentReference(), 1).getIntValue("int"));
        assertNull(this.document.getXObject(baseClass.getDocumentReference(), 2));
        assertNull(this.document.getXObject(baseClass.getDocumentReference(), 42));
    }

    /**
     * Unit test for {@link XWikiDocument#readObjectsFromFormUpdateOrCreate(EditForm, XWikiContext)} .
     */
    @Test
    public void readObjectsFromFormUpdateOrCreate() throws Exception
    {
        HttpServletRequest request = mock(HttpServletRequest.class);
        MockitoComponentManagerRule mocker = this.oldcore.getMocker();
        XWiki wiki = this.oldcore.getMockXWiki();
        XWikiContext context = this.oldcore.getXWikiContext();
        DocumentReferenceResolver<String> documentReferenceResolverString =
            mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        // Entity Reference resolver is used in <BaseObject>.getXClass()
        DocumentReferenceResolver<EntityReference> documentReferenceResolverEntity =
            mocker.registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "current");

        Map<String, String[]> parameters = generateFakeRequestMap();
        BaseClass baseClass = generateFakeClass();
        generateFakeObjects();
        EditForm eform = new EditForm();

        when(request.getParameterMap()).thenReturn(parameters);
        when(documentReferenceResolverString.resolve("space.page")).thenReturn(this.document.getDocumentReference());
        when(documentReferenceResolverString.resolve("InvalidSpace.InvalidPage")).thenReturn(
            new DocumentReference("wiki", "InvalidSpace", "InvalidPage"));
        // This entity resolver with this 'resolve' method is used in
        // <BaseCollection>.getXClassReference()
        when(documentReferenceResolverEntity.resolve(any(EntityReference.class), any(DocumentReference.class)))
            .thenReturn(this.document.getDocumentReference());
        when(wiki.getDocument(this.document.getDocumentReference(), context)).thenReturn(this.document);

        eform.setRequest(request);
        this.document.readObjectsFromFormUpdateOrCreate(eform, context);

        assertEquals(3, this.document.getXObjectSize(baseClass.getDocumentReference()));
        assertEquals("string1", this.document.getXObject(baseClass.getDocumentReference(), 0).getStringValue("string"));
        assertEquals(7, this.document.getXObject(baseClass.getDocumentReference(), 1).getIntValue("int"));
        assertNotNull(this.document.getXObject(baseClass.getDocumentReference(), 2));
        assertEquals("string2", this.document.getXObject(baseClass.getDocumentReference(), 2).getStringValue("string"));
        assertEquals(13, this.document.getXObject(baseClass.getDocumentReference(), 2).getIntValue("int"));
        assertNull(this.document.getXObject(baseClass.getDocumentReference(), 42));
    }
}
