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

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.link.LinkException;
import org.xwiki.link.LinkStore;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.UserConfiguration;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.doc.XWikiAttachmentList;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.component.XWikiDocumentFilterUtilsComponentList;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.validation.XWikiValidationInterface;
import com.xpn.xwiki.web.EditForm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiDocument}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@XWikiDocumentFilterUtilsComponentList
public class XWikiDocumentMockitoTest
{
    private static final String DOCWIKI = "wiki";

    private static final String DOCSPACE = "space";

    private static final String DOCNAME = "page";

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME);

    private static final DocumentReference CLASS_REFERENCE = DOCUMENT_REFERENCE;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentReferenceUserReferenceSerializer;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceDocumentReferenceResolver;

    @MockComponent
    private UserConfiguration userConfiguration;

    @MockComponent
    @Named("compactwiki/document")
    private UserReferenceSerializer<String> compactWikiUserReferenceSerializer;

    @MockComponent
    private LinkStore linkStore;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    /**
     * The object being tested.
     */
    private XWikiDocument document;

    private BaseClass baseClass;

    private BaseObject baseObject;

    private BaseObject baseObject2;

    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    private List<XWikiAttachment> attachmentList;

    @BeforeEach
    void beforeEach() throws Exception
    {
        // Activate programming rights in order to be able to call com.xpn.xwiki.api.Document#getDocument().
        when(this.oldcore.getMockRightService().hasProgrammingRights(this.oldcore.getXWikiContext())).thenReturn(true);

        this.document = new XWikiDocument(DOCUMENT_REFERENCE);
        this.document.setSyntax(Syntax.PLAIN_1_0);
        this.attachmentList = this.document.getAttachmentList();
        this.baseClass = this.document.getXClass();
        this.baseClass.addTextField("string", "String", 30);
        this.baseClass.addTextAreaField("area", "Area", 10, 10);
        this.baseClass.addTextAreaField("puretextarea", "Pure text area", 10, 10);
        // set the text areas an non interpreted content
        ((TextAreaClass) this.baseClass.getField("puretextarea")).setContentType("puretext");
        this.baseClass.addPasswordField("passwd", "Password", 30);
        this.baseClass.addBooleanField("boolean", "Boolean", "yesno");
        this.baseClass.addNumberField("int", "Int", 10, "integer");
        this.baseClass.addStaticListField("stringlist", "StringList", 1, true, "value1, value2");

        this.baseObject = this.document.newXObject(CLASS_REFERENCE, this.oldcore.getXWikiContext());
        this.baseObject.setStringValue("string", "string");
        this.baseObject.setLargeStringValue("area", "area");
        this.baseObject.setLargeStringValue("puretextarea", "puretextarea");
        this.baseObject.setStringValue("passwd", "passwd");
        this.baseObject.setIntValue("boolean", 1);
        this.baseObject.setIntValue("int", 42);
        this.baseObject.setStringListValue("stringlist", Arrays.asList("VALUE1", "VALUE2"));

        this.baseObject2 = this.baseObject.clone();
        this.document.addXObject(this.baseObject2);

        this.oldcore.getSpyXWiki().saveDocument(this.document, "", true, this.oldcore.getXWikiContext());

        this.defaultEntityReferenceSerializer =
            this.oldcore.getMocker().getInstance(EntityReferenceSerializer.TYPE_STRING);

        this.oldcore.getXWikiContext().setWikiId(DOCWIKI);

        // Reset the cached (static) MetaClass instance because it may have been initialized during the execution of the
        // previous test classes, so before the StaticListMetaClass component needed by this test class was loaded.
        MetaClass.setMetaClass(null);

        when(this.userConfiguration.getStoreHint()).thenReturn("document");
    }

    @Test
    void getChildrenReferences() throws Exception
    {
        Query query = mock(Query.class);
        when(this.oldcore.getQueryManager().createQuery(any(), eq(Query.XWQL))).thenReturn(query);

        QueryFilter hiddenFilter = this.oldcore.getMocker().registerMockComponent(QueryFilter.class, "hidden");

        when(query.setLimit(7)).thenReturn(query);

        List<String> result = Arrays.asList("X.y", "A.b");
        when(query.<String>execute()).thenReturn(result);

        List<DocumentReference> childrenReferences =
            document.getChildrenReferences(7, 3, this.oldcore.getXWikiContext());

        verify(query).addFilter(hiddenFilter);
        verify(query).setLimit(7);
        verify(query).setOffset(3);

        assertEquals(2, childrenReferences.size());
        assertEquals(new DocumentReference("wiki", "X", "y"), childrenReferences.get(0));
        assertEquals(new DocumentReference("wiki", "A", "b"), childrenReferences.get(1));
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
        String[] string1 = {"bloublou"};
        parameters.put("space.page_0_string", string1);
        String[] int1 = {"7"};
        parameters.put("space.page_1_int", int1);
        // Testing creation and update of an object's properties when object
        // doesn't exist
        String[] string2 = {"blabla"};
        String[] int2 = {"13"};
        parameters.put("space.page_3_string", string2);
        parameters.put("space.page_3_int", int2);
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
        BaseObject baseObject = null, baseObject2 = null, baseObject3 = null;
        try {
            baseObject = this.document.newXObject(this.document.getDocumentReference(), this.oldcore.getXWikiContext());
            baseObject2 =
                this.document.newXObject(this.document.getDocumentReference(), this.oldcore.getXWikiContext());
            baseObject3 =
                this.document.newXObject(this.document.getDocumentReference(), this.oldcore.getXWikiContext());
        } catch (XWikiException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        baseObject.setStringValue("string", "string");
        baseObject.setIntValue("int", 42);
        baseObject2.setStringValue("string", "string2");
        baseObject2.setIntValue("int", 42);
        baseObject3.setStringValue("string", "string3");
        baseObject3.setIntValue("int", 42);
    }

    /**
     * Unit test for {@link XWikiDocument#readObjectsFromForm(EditForm, XWikiContext)}.
     */
    @Test
    void readObjectsFromForm() throws Exception
    {
        this.document = new XWikiDocument(new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME));
        this.oldcore.getSpyXWiki().saveDocument(this.document, "", true, this.oldcore.getXWikiContext());

        HttpServletRequest request = mock(HttpServletRequest.class);
        MockitoComponentManager mocker = this.oldcore.getMocker();
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

        assertEquals(3, this.document.getXObjectSize(baseClass.getDocumentReference()));
        assertEquals("string", this.document.getXObject(baseClass.getDocumentReference(), 0).getStringValue("string"));
        assertEquals(42, this.document.getXObject(baseClass.getDocumentReference(), 0).getIntValue("int"));
        assertEquals("string2", this.document.getXObject(baseClass.getDocumentReference(), 1).getStringValue("string"));
        assertEquals(42, this.document.getXObject(baseClass.getDocumentReference(), 1).getIntValue("int"));
        assertEquals("string3", this.document.getXObject(baseClass.getDocumentReference(), 2).getStringValue("string"));
        assertEquals(42, this.document.getXObject(baseClass.getDocumentReference(), 2).getIntValue("int"));
        assertNull(this.document.getXObject(baseClass.getDocumentReference(), 3));
        assertNull(this.document.getXObject(baseClass.getDocumentReference(), 42));
    }

    /**
     * Unit test for {@link XWikiDocument#readObjectsFromFormUpdateOrCreate(EditForm, XWikiContext)} .
     */
    @Test
    void readObjectsFromFormUpdateOrCreate() throws Exception
    {
        this.document = new XWikiDocument(new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME));
        this.oldcore.getSpyXWiki().saveDocument(this.document, "", true, this.oldcore.getXWikiContext());

        HttpServletRequest request = mock(HttpServletRequest.class);
        MockitoComponentManager mocker = this.oldcore.getMocker();
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

        when(request.getParameter("objectPolicy")).thenReturn("updateOrCreate");
        when(request.getParameterMap()).thenReturn(parameters);
        when(documentReferenceResolverString.resolve("space.page")).thenReturn(this.document.getDocumentReference());
        when(documentReferenceResolverString.resolve("InvalidSpace.InvalidPage"))
            .thenReturn(new DocumentReference("wiki", "InvalidSpace", "InvalidPage"));
        // This entity resolver with this 'resolve' method is used in
        // <BaseCollection>.getXClassReference()
        when(documentReferenceResolverEntity.resolve(any(EntityReference.class), any(DocumentReference.class)))
            .thenReturn(this.document.getDocumentReference());
        doReturn(this.document).when(this.oldcore.getSpyXWiki()).getDocument(this.document.getDocumentReference(),
            context);

        eform.setRequest(request);
        eform.readRequest();
        this.document.readObjectsFromFormUpdateOrCreate(eform, context);

        assertEquals(43, this.document.getXObjectSize(baseClass.getDocumentReference()));
        assertEquals("bloublou",
            this.document.getXObject(baseClass.getDocumentReference(), 0).getStringValue("string"));
        assertEquals(42, this.document.getXObject(baseClass.getDocumentReference(), 0).getIntValue("int"));
        assertEquals("string2", this.document.getXObject(baseClass.getDocumentReference(), 1).getStringValue("string"));
        assertEquals(7, this.document.getXObject(baseClass.getDocumentReference(), 1).getIntValue("int"));
        assertEquals("string3", this.document.getXObject(baseClass.getDocumentReference(), 2).getStringValue("string"));
        assertEquals(42, this.document.getXObject(baseClass.getDocumentReference(), 2).getIntValue("int"));
        assertNotNull(this.document.getXObject(baseClass.getDocumentReference(), 3));
        assertEquals("blabla", this.document.getXObject(baseClass.getDocumentReference(), 3).getStringValue("string"));
        assertEquals(13, this.document.getXObject(baseClass.getDocumentReference(), 3).getIntValue("int"));
        assertNull(this.document.getXObject(baseClass.getDocumentReference(), 4));
        assertNotNull(this.document.getXObject(baseClass.getDocumentReference(), 42));
        assertEquals("bloublou",
            this.document.getXObject(baseClass.getDocumentReference(), 42).getStringValue("string"));
        assertEquals(7, this.document.getXObject(baseClass.getDocumentReference(), 42).getIntValue("int"));
    }

    /**
     * Unit test for {@link XWikiDocument#readAddedUpdatedAndRemovedObjectsFromForm(EditForm, XWikiContext)}.
     */
    @Test
    void readAddedUpdatedAndRemovedObjectsFromForm() throws Exception
    {
        this.document = new XWikiDocument(new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME));
        this.oldcore.getSpyXWiki().saveDocument(this.document, "", true, this.oldcore.getXWikiContext());

        HttpServletRequest request = mock(HttpServletRequest.class);
        MockitoComponentManager mocker = this.oldcore.getMocker();
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

        when(request.getParameterValues("addedObjects")).thenReturn(new String[] {"space.page_1", "space.page_42"});
        when(request.getParameterValues("deletedObjects")).thenReturn(new String[] {"space.page_2"});
        when(request.getParameterMap()).thenReturn(parameters);
        when(documentReferenceResolverString.resolve("space.page")).thenReturn(this.document.getDocumentReference());
        when(documentReferenceResolverString.resolve("InvalidSpace.InvalidPage"))
            .thenReturn(new DocumentReference("wiki", "InvalidSpace", "InvalidPage"));
        // This entity resolver with this 'resolve' method is used in
        // <BaseCollection>.getXClassReference()
        when(documentReferenceResolverEntity.resolve(any(EntityReference.class), any(DocumentReference.class)))
            .thenReturn(this.document.getDocumentReference());
        doReturn(this.document).when(this.oldcore.getSpyXWiki()).getDocument(this.document.getDocumentReference(),
            context);

        eform.setRequest(request);
        eform.readRequest();
        this.document.readAddedUpdatedAndRemovedObjectsFromForm(eform, context);

        assertEquals(43, this.document.getXObjectSize(baseClass.getDocumentReference()));
        assertEquals("bloublou",
            this.document.getXObject(baseClass.getDocumentReference(), 0).getStringValue("string"));
        assertEquals(42, this.document.getXObject(baseClass.getDocumentReference(), 0).getIntValue("int"));
        assertEquals("string2", this.document.getXObject(baseClass.getDocumentReference(), 1).getStringValue("string"));
        assertEquals(7, this.document.getXObject(baseClass.getDocumentReference(), 1).getIntValue("int"));
        assertNull(this.document.getXObject(baseClass.getDocumentReference(), 2));
        assertNull(this.document.getXObject(baseClass.getDocumentReference(), 3));
        assertNull(this.document.getXObject(baseClass.getDocumentReference(), 4));
        assertNotNull(this.document.getXObject(baseClass.getDocumentReference(), 42));
        assertEquals("bloublou",
            this.document.getXObject(baseClass.getDocumentReference(), 42).getStringValue("string"));
        assertEquals(7, this.document.getXObject(baseClass.getDocumentReference(), 42).getIntValue("int"));
    }

    @Test
    void testDeprecatedConstructors()
    {
        DocumentReference defaultReference = new DocumentReference("xwiki", "Main", "WebHome");

        XWikiDocument doc = new XWikiDocument(null);
        assertEquals(defaultReference, doc.getDocumentReference());

        doc = new XWikiDocument();
        assertEquals(defaultReference, doc.getDocumentReference());

        doc = new XWikiDocument("notused", "space.page");
        assertEquals("space", doc.getSpaceName());
        assertEquals("page", doc.getPageName());
        assertEquals(this.oldcore.getXWikiContext().getWikiId(), doc.getWikiName());

        doc = new XWikiDocument("space", "page");
        assertEquals("space", doc.getSpaceName());
        assertEquals("page", doc.getPageName());
        assertEquals(this.oldcore.getXWikiContext().getWikiId(), doc.getWikiName());

        doc = new XWikiDocument("wiki2", "space", "page");
        assertEquals("space", doc.getSpaceName());
        assertEquals("page", doc.getPageName());
        assertEquals("wiki2", doc.getWikiName());

        doc = new XWikiDocument("wiki2", "notused", "notused:space.page");
        assertEquals("space", doc.getSpaceName());
        assertEquals("page", doc.getPageName());
        assertEquals("wiki2", doc.getWikiName());
    }

    @Test
    void testMinorMajorVersions()
    {
        this.document = new XWikiDocument(new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME));

        // there is no version in doc yet, so 1.1
        assertEquals("1.1", this.document.getVersion());

        this.document.setMinorEdit(false);
        this.document.incrementVersion();
        // no version => incrementVersion sets 1.1
        assertEquals("1.1", this.document.getVersion());

        this.document.setMinorEdit(false);
        this.document.incrementVersion();
        // increment major version
        assertEquals("2.1", this.document.getVersion());

        this.document.setMinorEdit(true);
        this.document.incrementVersion();
        // increment minor version
        assertEquals("2.2", this.document.getVersion());
    }

    @Test
    void testGetPreviousVersion() throws XWikiException
    {
        this.document = new XWikiDocument(new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME));

        Date now = new Date();
        XWikiDocumentArchive archiveDoc = new XWikiDocumentArchive(this.document.getId());
        this.document.setDocumentArchive(archiveDoc);

        assertEquals("1.1", this.document.getVersion());
        assertNull(this.document.getPreviousVersion());

        this.document.incrementVersion();
        archiveDoc.updateArchive(this.document, "Admin", now, "", this.document.getRCSVersion(),
            this.oldcore.getXWikiContext());
        assertEquals("1.1", this.document.getVersion());
        assertNull(this.document.getPreviousVersion());

        this.document.setMinorEdit(true);
        this.document.incrementVersion();
        archiveDoc.updateArchive(this.document, "Admin", now, "", this.document.getRCSVersion(),
            this.oldcore.getXWikiContext());
        assertEquals("1.2", this.document.getVersion());
        assertEquals("1.1", this.document.getPreviousVersion());

        this.document.setMinorEdit(false);
        this.document.incrementVersion();
        archiveDoc.updateArchive(this.document, "Admin", now, "", this.document.getRCSVersion(),
            this.oldcore.getXWikiContext());
        assertEquals("2.1", this.document.getVersion());
        assertEquals("1.2", this.document.getPreviousVersion());

        this.document.setMinorEdit(true);
        this.document.incrementVersion();
        archiveDoc.updateArchive(this.document, "Admin", now, "", this.document.getRCSVersion(),
            this.oldcore.getXWikiContext());
        assertEquals("2.2", this.document.getVersion());
        assertEquals("2.1", this.document.getPreviousVersion());

        archiveDoc.resetArchive();

        assertEquals("2.2", this.document.getVersion());
        assertNull(this.document.getPreviousVersion());
    }

    @Test
    void testCloneNullObjects()
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", DOCSPACE, DOCNAME));

        EntityReference relativeClassReference =
            new EntityReference(DOCNAME, EntityType.DOCUMENT, new EntityReference(DOCSPACE, EntityType.SPACE));
        DocumentReference classReference = new DocumentReference("wiki", DOCSPACE, DOCNAME);
        DocumentReference duplicatedClassReference = new DocumentReference("otherwiki", DOCSPACE, DOCNAME);

        // no object
        XWikiDocument clonedDocument = document.clone();
        assertTrue(clonedDocument.getXObjects().isEmpty());

        XWikiDocument duplicatedDocument = document.duplicate(new DocumentReference("otherwiki", DOCSPACE, DOCNAME));
        assertTrue(duplicatedDocument.getXObjects().isEmpty());

        // 1 null object

        document.addXObject(classReference, null);

        clonedDocument = document.clone();
        assertEquals(1, clonedDocument.getXObjects(classReference).size());
        assertEquals(document.getXObjects(classReference), clonedDocument.getXObjects(classReference));

        duplicatedDocument = document.duplicate(new DocumentReference("otherwiki", DOCSPACE, DOCNAME));
        assertTrue(duplicatedDocument.getXObjects().isEmpty());

        // 1 null object and 1 object

        BaseObject object = new BaseObject();
        object.setXClassReference(relativeClassReference);
        document.addXObject(object);

        clonedDocument = document.clone();
        assertEquals(2, clonedDocument.getXObjects(classReference).size());
        assertEquals(document.getXObjects(classReference), clonedDocument.getXObjects(classReference));

        duplicatedDocument = document.duplicate(new DocumentReference("otherwiki", DOCSPACE, DOCNAME));
        assertEquals(2, duplicatedDocument.getXObjects(duplicatedClassReference).size());
    }

    @Test
    void testToStringReturnsFullName()
    {
        assertEquals("space.page", this.document.toString());
        assertEquals("Main.WebHome", new XWikiDocument().toString());
    }

    @Test
    void testCloneSaveVersions()
    {
        XWikiDocument doc1 = new XWikiDocument(new DocumentReference("qwe", "qwe", "qwe"));
        XWikiDocument doc2 = doc1.clone();
        doc1.incrementVersion();
        doc2.incrementVersion();
        assertEquals(doc1.getVersion(), doc2.getVersion());
    }

    @Test
    void testAddObject() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("test", "test", "document"));
        BaseObject object = BaseClass.newCustomClassInstance("XWiki.XWikiUsers", this.oldcore.getXWikiContext());
        doc.addObject("XWiki.XWikiUsers", object);
        assertEquals(doc.getFullName(), object.getName(), "XWikiDocument.addObject does not set the object's name");
    }

    @Test
    void testObjectNumbersAfterXMLRoundrip() throws XWikiException
    {
        String wiki = oldcore.getXWikiContext().getWikiId();

        XWikiDocument tagDocument = new XWikiDocument(new DocumentReference(wiki, "XWiki", "TagClass"));
        BaseClass tagClass = tagDocument.getXClass();
        tagClass.addStaticListField(XWikiConstant.TAG_CLASS_PROP_TAGS, "Tags", 30, true, "", "checkbox");
        this.oldcore.getSpyXWiki().saveDocument(tagDocument, this.oldcore.getXWikiContext());

        XWikiDocument doc = new XWikiDocument(new DocumentReference(wiki, "test", "document"));
        doReturn("iso-8859-1").when(this.oldcore.getSpyXWiki()).getEncoding();

        BaseObject object1 = doc.newXObject(tagDocument.getDocumentReference(), this.oldcore.getXWikiContext());
        BaseObject object2 = doc.newXObject(tagDocument.getDocumentReference(), this.oldcore.getXWikiContext());
        BaseObject object3 = doc.newXObject(tagDocument.getDocumentReference(), this.oldcore.getXWikiContext());

        // Remove first object
        doc.removeXObject(object1);

        String docXML = doc.toXML(this.oldcore.getXWikiContext());
        XWikiDocument docFromXML = new XWikiDocument(doc.getDocumentReference());
        docFromXML.fromXML(docXML);

        List<BaseObject> objects = doc.getXObjects(tagDocument.getDocumentReference());
        List<BaseObject> objectsFromXML = docFromXML.getXObjects(tagDocument.getDocumentReference());

        assertNotNull(objects);
        assertNotNull(objectsFromXML);

        assertTrue(objects.size() == objectsFromXML.size());

        for (int i = 0; i < objects.size(); i++) {
            if (objects.get(i) == null) {
                assertNull(objectsFromXML.get(i));
            } else {
                assertTrue(objects.get(i).getNumber() == objectsFromXML.get(i).getNumber());
            }
        }
    }

    @Test
    void testGetXObjectWithObjectReference()
    {
        assertSame(this.baseObject, this.document.getXObject(this.baseObject.getReference()));

        assertSame(this.baseObject,
            this.document.getXObject(new ObjectReference(
                this.defaultEntityReferenceSerializer.serialize(this.baseObject.getXClassReference()),
                this.document.getDocumentReference())));
    }

    @Test
    void testGetXObjectWithNumber()
    {
        assertSame(this.baseObject, this.document.getXObject(CLASS_REFERENCE, this.baseObject.getNumber()));
        assertSame(this.baseObject2, this.document.getXObject(CLASS_REFERENCE, this.baseObject2.getNumber()));
        assertSame(this.baseObject,
            this.document.getXObject((EntityReference) CLASS_REFERENCE, this.baseObject.getNumber()));
        assertSame(this.baseObject2,
            this.document.getXObject((EntityReference) CLASS_REFERENCE, this.baseObject2.getNumber()));
    }

    @Test
    void testGetXObjectCreateWithNumber() throws XWikiException
    {
        assertSame(this.baseObject, this.document.getXObject(CLASS_REFERENCE, this.baseObject.getNumber(), true,
            this.oldcore.getXWikiContext()));
        assertSame(this.baseObject2, this.document.getXObject(CLASS_REFERENCE, this.baseObject2.getNumber(), true,
            this.oldcore.getXWikiContext()));
        assertSame(this.baseObject, this.document.getXObject(CLASS_REFERENCE, this.baseObject.getNumber(), true,
            this.oldcore.getXWikiContext()));
        assertSame(this.baseObject2, this.document.getXObject(CLASS_REFERENCE, this.baseObject2.getNumber(), true,
            this.oldcore.getXWikiContext()));

        BaseObject newObject = this.document.getXObject(CLASS_REFERENCE, 42, true, this.oldcore.getXWikiContext());
        assertNotSame(this.baseObject, newObject);
        assertNotSame(this.baseObject2, newObject);
        assertEquals(42, newObject.getNumber());
        assertSame(newObject, this.document.getXObject(CLASS_REFERENCE, newObject.getNumber()));
    }

    @Test
    void testGetXObjectsWhenClassDoesNotExist()
    {
        assertEquals(Collections.emptyList(),
            this.document.getXObjects(new DocumentReference("not", "existing", "class")));
    }

    @Test
    void testSetXObjectswithPreviousObject()
    {
        BaseObject object = new BaseObject();
        object.setXClassReference(this.baseObject.getXClassReference());
        this.document.addXObject(object);

        this.document.setXObjects(this.baseObject.getXClassReference(), Arrays.asList(object));

        assertEquals(Arrays.asList(object), this.document.getXObjects(this.baseObject.getXClassReference()));
    }

    @Test
    void testSetXObjectWhithNoPreviousObject()
    {
        XWikiDocument document = new XWikiDocument(this.document.getDocumentReference());

        document.setXObject(this.baseObject.getXClassReference(), 0, this.baseObject);

        assertEquals(Arrays.asList(this.baseObject), document.getXObjects(this.baseObject.getXClassReference()));
    }

    /**
     * Test that the parent remain the same relative value whatever the context.
     */
    @Test
    void testGetParent()
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("docwiki", "docspace", "docpage"));

        assertEquals("", doc.getParent());
        doc.setParent(null);
        assertEquals("", doc.getParent());

        doc.setParent("page");
        assertEquals("page", doc.getParent());

        this.oldcore.getXWikiContext().setWikiId("otherwiki");
        assertEquals("page", doc.getParent());

        doc.setDocumentReference(new DocumentReference("otherwiki", "otherspace", "otherpage"));
        assertEquals("page", doc.getParent());
    }

    @Test
    void testGetParentReference()
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("docwiki", "docspace", "docpage"));

        assertNull(doc.getParentReference());

        doc.setParent("parentpage");

        // ////////////////////////////////////////////////////////////////
        // The following tests are checking that document reference cache is properly cleaned something could make the
        // parent change

        assertEquals(new DocumentReference("docwiki", "docspace", "parentpage"), doc.getParentReference());

        doc.setName("docpage2");
        assertEquals(new DocumentReference("docwiki", "docspace", "parentpage"), doc.getParentReference());

        doc.setSpace("docspace2");
        assertEquals(new DocumentReference("docwiki", "docspace2", "parentpage"), doc.getParentReference());

        doc.setDatabase("docwiki2");
        assertEquals(new DocumentReference("docwiki2", "docspace2", "parentpage"), doc.getParentReference());

        doc.setDocumentReference(new DocumentReference("docwiki", "docspace", "docpage"));
        assertEquals(new DocumentReference("docwiki", "docspace", "parentpage"), doc.getParentReference());

        doc.setFullName("docwiki2:docspace2.docpage2", this.oldcore.getXWikiContext());
        assertEquals(new DocumentReference("docwiki2", "docspace2", "parentpage"), doc.getParentReference());

        doc.setParent("parentpage2");
        assertEquals(new DocumentReference("docwiki2", "docspace2", "parentpage2"), doc.getParentReference());
    }

    @Test
    void testSetRelativeParentReference()
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("docwiki", "docspace", "docpage"));

        doc.setParentReference(new EntityReference("docpage2", EntityType.DOCUMENT));
        assertEquals(new DocumentReference("docwiki", "docspace", "docpage2"), doc.getParentReference());
        assertEquals("docpage2", doc.getParent());
    }

    /**
     * Verify that setting a new creator will create a new revision (we verify that that metadata dirty flag is set to
     * true).
     * 
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-7445">XWIKI-7445</a>
     */
    @Test
    void testSetCreatorReferenceSetsMetadataDirtyFlag()
    {
        // Make sure we set the flag to false to verify it's changed
        this.document.setMetaDataDirty(false);

        DocumentReference creator = new DocumentReference("Wiki", "XWiki", "Creator");
        when(this.userReferenceDocumentReferenceResolver.resolve(creator)).thenReturn(mock(UserReference.class));
        this.document.setCreatorReference(creator);

        assertEquals(true, this.document.isMetaDataDirty());
    }

    /**
     * Verify that setting a new creator that is the same as the currenet creator doesn't create a new revision (we
     * verify that the metadata dirty flag is not set).
     * 
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-7445">XWIKI-7445</a>
     */
    @Test
    void testSetCreatorReferenceWithSameCreatorDoesntSetMetadataDirtyFlag()
    {
        // Make sure we set the metadata dirty flag to false to verify it's not changed thereafter
        DocumentReference creator = new DocumentReference("Wiki", "XWiki", "Creator");
        this.document.setCreatorReference(creator);
        this.document.setMetaDataDirty(false);

        // Set the creator with the same reference to verify it doesn't change the flag
        this.document.setCreatorReference(new DocumentReference("Wiki", "XWiki", "Creator"));

        assertEquals(false, this.document.isMetaDataDirty());
    }

    /**
     * Verify that setting a new author will create a new revision (we verify that that metadata dirty flag is set to
     * true).
     * 
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-7445">XWIKI-7445</a>
     */
    @Test
    void testSetAuthorReferenceSetsMetadataDirtyFlag()
    {
        // Make sure we set the flag to false to verify it's changed
        this.document.setMetaDataDirty(false);

        DocumentReference author = new DocumentReference("Wiki", "XWiki", "Author");
        when(this.userReferenceDocumentReferenceResolver.resolve(author)).thenReturn(mock(UserReference.class));
        this.document.setAuthorReference(author);

        assertEquals(true, this.document.isMetaDataDirty());
    }

    /**
     * Verify that setting a new author that is the same as the currenet creator doesn't create a new revision (we
     * verify that the metadata dirty flag is not set).
     * 
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-7445">XWIKI-7445</a>
     */
    @Test
    void testSetAuthorReferenceWithSameAuthorDoesntSetMetadataDirtyFlag()
    {
        // Make sure we set the metadata dirty flag to false to verify it's not changed thereafter
        DocumentReference author = new DocumentReference("Wiki", "XWiki", "Author");
        UserReference userReference = mock(UserReference.class);
        when(this.documentReferenceUserReferenceSerializer.serialize(userReference)).thenReturn(author);
        when(this.userReferenceDocumentReferenceResolver.resolve(author)).thenReturn(userReference);
        this.document.setAuthorReference(author);
        this.document.setMetaDataDirty(false);

        // Set the author with the same reference to verify it doesn't change the flag
        this.document.setAuthorReference(new DocumentReference("Wiki", "XWiki", "Author"));

        assertEquals(false, this.document.isMetaDataDirty());
    }

    /**
     * Verify that setting a new content author will create a new revision (we verify that that metadata dirty flag is
     * set to true).
     * 
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-7445">XWIKI-7445</a>
     */
    @Test
    void testSetContentAuthorReferenceSetsMetadataDirtyFlag()
    {
        // Make sure we set the flag to false to verify it's changed
        this.document.setMetaDataDirty(false);

        DocumentReference contentAuthor = new DocumentReference("Wiki", "XWiki", "ContentAuthor");
        when(this.userReferenceDocumentReferenceResolver.resolve(contentAuthor)).thenReturn(mock(UserReference.class));
        this.document.setContentAuthorReference(contentAuthor);

        assertEquals(true, this.document.isMetaDataDirty());
    }

    /**
     * Verify that setting a new content author that is the same as the currenet creator doesn't create a new revision
     * (we verify that the metadata dirty flag is not set).
     * 
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-7445">XWIKI-7445</a>
     */
    @Test
    void testSetContentAuthorReferenceWithSameContentAuthorDoesntSetMetadataDirtyFlag()
    {
        // Make sure we set the metadata dirty flag to false to verify it's not changed thereafter
        DocumentReference contentAuthor = new DocumentReference("Wiki", "XWiki", "ContentAuthor");
        this.document.setContentAuthorReference(contentAuthor);
        this.document.setMetaDataDirty(false);

        // Set the content author with the same reference to verify it doesn't change the flag
        this.document.setContentAuthorReference(new DocumentReference("Wiki", "XWiki", "ContentAuthor"));

        assertEquals(false, this.document.isMetaDataDirty());
    }

    @Test
    void testSetContentSetsContentDirtyFlag()
    {
        // Make sure we set the flags to false to verify it's changed
        this.document.setContentDirty(false);
        this.document.setMetaDataDirty(false);

        this.document.setContent("something");

        assertTrue(this.document.isContentDirty());
        assertFalse(this.document.isMetaDataDirty());
    }

    @Test
    void testSetSameContentDoesNotSetContentDirtyFlag()
    {
        this.document.setContent("something");
        // Make sure we set the flag to false to verify it's changed
        this.document.setContentDirty(false);

        // Set the same content again.
        this.document.setContent("something");

        assertFalse(this.document.isContentDirty());
    }

    @Test
    void testModifyObjectsSetsOnlyMetadataDirtyFlag() throws Exception
    {
        DocumentReference classReference = this.document.getDocumentReference();

        // Make sure we set the flags to false to verify it's changed
        this.document.setContentDirty(false);
        this.document.setMetaDataDirty(false);

        // New objects.
        BaseObject object = this.document.newXObject(classReference, this.oldcore.getXWikiContext());

        assertTrue(this.document.isMetaDataDirty());
        assertFalse(this.document.isContentDirty());

        // Make sure we set the flags to false to verify it's changed
        this.document.setContentDirty(false);
        this.document.setMetaDataDirty(false);

        // Set/add objects.
        this.document.setXObject(0, object);

        assertTrue(this.document.isMetaDataDirty());
        assertFalse(this.document.isContentDirty());

        // Make sure we set the flags to false to verify it's changed
        this.document.setContentDirty(false);
        this.document.setMetaDataDirty(false);

        // Remove objects
        this.document.removeXObject(object);

        assertTrue(this.document.isMetaDataDirty());
        assertFalse(this.document.isContentDirty());
    }

    @Test
    void testModifyAttachmentsSetsOnlyMetadataDirtyFlag() throws Exception
    {
        // Make sure we set the flags to false to verify it's changed
        this.document.setContentDirty(false);
        this.document.setMetaDataDirty(false);

        // Add attachments.
        XWikiAttachment attachment =
            document.addAttachment("file", new ByteArrayInputStream(new byte[] {}), this.oldcore.getXWikiContext());

        assertTrue(this.document.isMetaDataDirty());
        assertFalse(this.document.isContentDirty());

        // Make sure we set the flags to false to verify it's changed
        this.document.setContentDirty(false);
        this.document.setMetaDataDirty(false);
        // Add attachments (2).
        XWikiAttachment attachment2 =
            document.addAttachment("file2", new ByteArrayInputStream(new byte[] {}), this.oldcore.getXWikiContext());
        assertTrue(this.document.isMetaDataDirty());
        assertFalse(this.document.isContentDirty());

        // Make sure we set the flags to false to verify it's changed
        this.document.setContentDirty(false);
        this.document.setMetaDataDirty(false);

        // Modify attachment.
        attachment.setContent(new ByteArrayInputStream(new byte[] {1, 2, 3}));

        assertTrue(this.document.isMetaDataDirty());
        assertFalse(this.document.isContentDirty());

        // Make sure we set the flags to false to verify it's changed
        this.document.setContentDirty(false);
        this.document.setMetaDataDirty(false);

        // Remove objects
        this.document.removeAttachment(attachment);

        assertTrue(this.document.isMetaDataDirty());
        assertFalse(this.document.isContentDirty());
    }

    @Test
    void testEqualsDatas()
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        XWikiDocument otherDocument = document.clone();

        assertTrue(document.equals(otherDocument));
        assertTrue(document.equalsData(otherDocument));

        otherDocument.setAuthorReference(new DocumentReference("wiki", "space", "otherauthor"));
        otherDocument.setContentAuthorReference(otherDocument.getAuthorReference());
        otherDocument.setCreatorReference(otherDocument.getAuthorReference());
        otherDocument.setVersion("42.0");
        otherDocument.setComment("other comment");
        otherDocument.setMinorEdit(true);

        document.setMinorEdit(false);

        assertFalse(document.equals(otherDocument));
        assertTrue(document.equalsData(otherDocument));
    }

    @Test
    void testEqualsAttachments() throws XWikiException
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        XWikiDocument otherDocument = document.clone();

        XWikiAttachment attachment = document.addAttachment("file", new byte[] {1, 2}, this.oldcore.getXWikiContext());
        XWikiAttachment otherAttachment =
            otherDocument.addAttachment("file", new byte[] {1, 2}, this.oldcore.getXWikiContext());

        assertTrue(document.equals(otherDocument));
        assertTrue(document.equalsData(otherDocument));

        otherAttachment.setContent(new byte[] {1, 2, 3});

        assertFalse(document.equals(otherDocument));
        assertFalse(document.equalsData(otherDocument));
    }

    @Test
    void testSetMetadataDirtyWhenAttachmenListChanges() throws XWikiException
    {
        XWikiDocument document = new XWikiDocument();

        XWikiAttachment attachment = document.addAttachment("file", new byte[] {1, 2}, this.oldcore.getXWikiContext());

        // Force the metadata not dirty.
        document.setMetaDataDirty(false);

        List<XWikiAttachment> attachments = document.getAttachmentList();
        // Modify (clear) the attachments list)
        attachments.clear();

        // Check that the the metadata is now dirty as a result.
        assertTrue(document.isMetaDataDirty());

        // Check adding to list
        document.setMetaDataDirty(false);
        attachments.add(new XWikiAttachment());
        assertTrue(document.isMetaDataDirty());

        // Check removing from the list
        document.setMetaDataDirty(false);
        attachments.remove(0);
        assertTrue(document.isMetaDataDirty());
    }

    /**
     * XWIKI-8463: Backwards compatibility issue with setting the same attachment list to a document
     */
    @Test
    void testSetGetAttachmentList() throws Exception
    {
        String attachmentName1 = "someFile.txt";
        String attachmentName2 = "someOtherFile.txt";
        this.document.addAttachment(attachmentName1, new byte[0], this.oldcore.getXWikiContext());
        this.document.addAttachment(attachmentName2, new byte[0], this.oldcore.getXWikiContext());

        List<String> attachmentNames = new ArrayList<String>();

        assertEquals(2, this.document.getAttachmentList().size());
        for (XWikiAttachment attachment : this.document.getAttachmentList()) {
            attachmentNames.add(attachment.getFilename());
        }
        assertTrue(attachmentNames.contains(attachmentName1));
        assertTrue(attachmentNames.contains(attachmentName2));

        // Set back the same list returned by the getter.
        this.document.setAttachmentList(this.document.getAttachmentList());

        // The result needs to stay the same.
        assertEquals(2, this.document.getAttachmentList().size());
        attachmentNames.clear();
        for (XWikiAttachment attachment : this.document.getAttachmentList()) {
            attachmentNames.add(attachment.getFilename());
        }
        assertTrue(attachmentNames.contains(attachmentName1));
        assertTrue(attachmentNames.contains(attachmentName2));
    }

    /**
     * Unit test for {@link XWikiDocument#readFromTemplate(DocumentReference, XWikiContext)}.
     */
    @Test
    void testReadFromTemplate() throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();

        SpaceReference spaceReference = new SpaceReference("Space", new WikiReference("wiki"));
        XWikiDocument template = new XWikiDocument(new DocumentReference("Template", spaceReference));
        template.setParentReference(new EntityReference("Parent", EntityType.DOCUMENT, spaceReference));
        template.setTitle("Enter title here");
        template.setSyntax(Syntax.XWIKI_2_0);
        template.setContent("Enter content here");

        DocumentReference templateAuthor = new DocumentReference("test", "Users", "John");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");

        XWikiAttachment aliceAttachment = new XWikiAttachment(template, "alice.png");
        aliceAttachment.setContent(new ByteArrayInputStream("alice content".getBytes()));
        aliceAttachment.setVersion("2.3");
        aliceAttachment.setDate(simpleDateFormat.parse("12/03/2018"));
        aliceAttachment.setAuthorReference(templateAuthor);
        template.setAttachment(aliceAttachment);

        XWikiAttachment bobAttachment = new XWikiAttachment(template, "bob.png");
        bobAttachment.setVersion("5.3");
        bobAttachment.setDate(simpleDateFormat.parse("25/5/2019"));
        bobAttachment.setAuthorReference(templateAuthor);
        template.setAttachment(bobAttachment);

        // Verify that the attachment content is loaded before being copied.
        XWikiAttachmentStoreInterface attachmentContentStore = mock(XWikiAttachmentStoreInterface.class);
        xcontext.getWiki().setDefaultAttachmentContentStore(attachmentContentStore);
        doAnswer(invocation -> {
            XWikiAttachment attachment = invocation.getArgument(0);
            if ("bob.png".equals(attachment.getFilename())) {
                XWikiAttachmentContent attachmentContent = new XWikiAttachmentContent(attachment);
                attachmentContent.setContent(new ByteArrayInputStream("bob content".getBytes()));
                attachment.setAttachment_content(attachmentContent);
            }
            return null;
        }).when(attachmentContentStore).loadAttachmentContent(any(XWikiAttachment.class), eq(xcontext), eq(true));

        this.oldcore.getSpyXWiki().saveDocument(template, xcontext);

        XWikiDocument target = new XWikiDocument(new DocumentReference("Page", spaceReference));

        DocumentReference targetAuthor = new DocumentReference("test", "Users", "Denis");
        xcontext.setUserReference(targetAuthor);

        XWikiAttachment aliceModifiedAttachment = new XWikiAttachment(target, "alice.png");
        aliceModifiedAttachment.setContent(new ByteArrayInputStream("alice modified content".getBytes()));
        aliceModifiedAttachment.setVersion("1.2");
        aliceModifiedAttachment.setDate(simpleDateFormat.parse("07/10/2020"));
        aliceModifiedAttachment.setAuthorReference(targetAuthor);
        target.setAttachment(aliceModifiedAttachment);

        XWikiAttachment carolAttachment = new XWikiAttachment(target, "carol.png");
        carolAttachment.setContent(new ByteArrayInputStream("carol content".getBytes()));
        carolAttachment.setVersion("3.1");
        carolAttachment.setDate(simpleDateFormat.parse("13/11/2020"));
        carolAttachment.setAuthorReference(targetAuthor);
        target.setAttachment(carolAttachment);

        target.readFromTemplate(template.getDocumentReference(), xcontext);

        assertEquals(template.getDocumentReference(), target.getTemplateDocumentReference());
        assertEquals(template.getParentReference(), target.getParentReference());
        assertEquals(template.getTitle(), target.getTitle());
        assertEquals(template.getSyntax(), target.getSyntax());
        assertEquals(template.getContent(), target.getContent());

        assertEquals(3, target.getAttachmentList().size());
        assertAttachment("alice modified content", "1.2", targetAuthor, simpleDateFormat.parse("07/10/2020"),
            target.getAttachment("alice.png"));
        assertAttachment("bob content", "1.1", targetAuthor, null, target.getAttachment("bob.png"));
        assertAttachment("carol content", "3.1", targetAuthor, simpleDateFormat.parse("13/11/2020"),
            target.getAttachment("carol.png"));
    }

    private void assertAttachment(String expectedContent, String expectedVersion,
        DocumentReference expectedAuthorReference, Date expectedDate, XWikiAttachment actualAttachment) throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        assertEquals(expectedContent, IOUtils.toString(actualAttachment.getContentInputStream(xcontext), "UTF-8"));
        assertEquals(expectedVersion, actualAttachment.getVersion());
        assertEquals(expectedAuthorReference, actualAttachment.getAuthorReference());
        if (expectedDate != null) {
            assertEquals(expectedDate, actualAttachment.getDate());
        } else {
            // The expected date is pretty recent (no more than 5 seconds ago).
            long delta = new Date().getTime() - actualAttachment.getDate().getTime();
            assertTrue(delta < 5000);
        }
    }

    @Test
    void testResolveClassReference() throws Exception
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("docwiki", "docspace", "docpage"));

        DocumentReference expected1 = new DocumentReference("docwiki", "XWiki", "docpage");
        assertEquals(expected1, doc.resolveClassReference(""));

        DocumentReference expected2 = new DocumentReference("docwiki", "XWiki", "page");
        assertEquals(expected2, doc.resolveClassReference("page"));

        DocumentReference expected3 = new DocumentReference("docwiki", "space", "page");
        assertEquals(expected3, doc.resolveClassReference("space.page"));

        DocumentReference expected4 = new DocumentReference("wiki", "space", "page");
        assertEquals(expected4, doc.resolveClassReference("wiki:space.page"));
    }

    /**
     * Verify that cloning objects modify their references to point to the document in which they are cloned into.
     */
    @Test
    void testCloneObjectsHaveCorrectReference()
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("somewiki", "somespace", "somepage"));
        doc.cloneXObjects(this.document);
        assertTrue(doc.getXObjects().size() > 0);

        // Verify that the object references point to the doc in which it's cloned.
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : doc.getXObjects().entrySet()) {
            for (BaseObject baseObject : entry.getValue()) {
                assertEquals(doc.getDocumentReference(), baseObject.getDocumentReference());
            }
        }
    }

    /**
     * Verify that merging objects modify their references to point to the document in which they are cloned into and
     * that GUID for merged objects are different from the original GUIDs.
     */
    @Test
    void testMergeObjectsHaveCorrectReferenceAndDifferentGuids()
    {
        List<String> originalGuids = new ArrayList<String>();
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : this.document.getXObjects().entrySet()) {
            for (BaseObject baseObject : entry.getValue()) {
                originalGuids.add(baseObject.getGuid());
            }
        }

        // Use a document from a different wiki to see if the class reference of the merged objects is adjusted:
        // documents can't have objects of types defined in a different wiki.
        XWikiDocument doc = new XWikiDocument(new DocumentReference("somewiki", "somespace", "somepage"));
        doc.mergeXObjects(this.document);

        assertTrue(doc.getXObjects().size() > 0);

        for (Map.Entry<DocumentReference, List<BaseObject>> entry : doc.getXObjects().entrySet()) {
            // Verify that the class reference and the target document reference have the same wiki component.
            assertEquals(doc.getDocumentReference().getWikiReference(), entry.getKey().getWikiReference());
            for (BaseObject baseObject : entry.getValue()) {
                // Verify that the object references point to the doc in which it's cloned.
                assertEquals(doc.getDocumentReference(), baseObject.getDocumentReference());
                // Verify that GUIDs are not the same as the original ones
                assertFalse(originalGuids.contains(baseObject.getGuid()), "Non unique object GUID found!");
            }
        }
    }

    /**
     * Tests that objects are not copied again when {@link XWikiDocument#mergeXObjects(XWikiDocument)} is called twice.
     */
    @Test
    void testMergeObjectsTwice()
    {
        // Make sure the target document and the template document are from different wikis.
        XWikiDocument targetDoc = new XWikiDocument(new DocumentReference("someWiki", "someSpace", "somePage"));

        // Merge the objects.
        targetDoc.mergeXObjects(this.document);

        assertEquals(1, targetDoc.getXObjects().size());
        assertEquals(0, targetDoc.getXObjectSize(CLASS_REFERENCE));
        DocumentReference classReference = CLASS_REFERENCE.replaceParent(CLASS_REFERENCE.getWikiReference(),
            targetDoc.getDocumentReference().getWikiReference());
        assertEquals(2, targetDoc.getXObjectSize(classReference));

        // Try to merge the objects again.
        targetDoc.mergeXObjects(this.document);

        // Check that the object from the template document was not copied again.
        assertEquals(2, targetDoc.getXObjectSize(classReference));
    }

    /** Check that a new empty document has empty content (used to have a new line before 2.5). */
    @Test
    void testInitialContent()
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("somewiki", "somespace", "somepage"));
        assertEquals("", doc.getContent());
    }

    @Test
    void testAuthorAfterDocumentCopy() throws XWikiException
    {
        DocumentReference author = new DocumentReference("Wiki", "XWiki", "Albatross");
        UserReference userReference = mock(UserReference.class);
        when(this.documentReferenceUserReferenceSerializer.serialize(userReference)).thenReturn(author);
        when(this.userReferenceDocumentReferenceResolver.resolve(author)).thenReturn(userReference);
        this.document.setAuthorReference(author);
        XWikiDocument copy =
            this.document.copyDocument(this.document.getName() + " Copy", this.oldcore.getXWikiContext());

        assertEquals(author, copy.getAuthorReference());
    }

    @Test
    void testCreatorAfterDocumentCopy() throws XWikiException
    {
        UserReference userReference = mock(UserReference.class);
        DocumentReference creator = new DocumentReference("Wiki", "XWiki", "Condor");
        when(this.userReferenceDocumentReferenceResolver.resolve(creator)).thenReturn(userReference);
        when(this.documentReferenceUserReferenceSerializer.serialize(userReference)).thenReturn(creator);
        this.document.setCreatorReference(creator);
        XWikiDocument copy =
            this.document.copyDocument(this.document.getName() + " Copy", this.oldcore.getXWikiContext());

        assertEquals(creator, copy.getCreatorReference());
    }

    @Test
    void testCreationDateAfterDocumentCopy() throws Exception
    {
        Date sourceCreationDate = this.document.getCreationDate();
        Thread.sleep(1000);
        XWikiDocument copy =
            this.document.copyDocument(this.document.getName() + " Copy", this.oldcore.getXWikiContext());

        assertEquals(sourceCreationDate, copy.getCreationDate());
    }

    @Test
    void testObjectGuidsAfterDocumentCopy() throws Exception
    {
        assertTrue(this.document.getXObjects().size() > 0);

        List<String> originalGuids = new ArrayList<String>();
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : this.document.getXObjects().entrySet()) {
            for (BaseObject baseObject : entry.getValue()) {
                originalGuids.add(baseObject.getGuid());
            }
        }

        XWikiDocument copy =
            this.document.copyDocument(this.document.getName() + " Copy", this.oldcore.getXWikiContext());

        // Verify that the cloned objects have different GUIDs
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : copy.getXObjects().entrySet()) {
            for (BaseObject baseObject : entry.getValue()) {
                assertFalse(originalGuids.contains(baseObject.getGuid()), "Non unique object GUID found!");
            }
        }
    }

    @Test
    void testRelativeObjectReferencesAfterDocumentCopy() throws Exception
    {
        XWikiDocument copy = this.document.copyDocument(new DocumentReference("copywiki", "copyspace", "copypage"),
            this.oldcore.getXWikiContext());

        // Verify that the XObject's XClass reference points to the target wiki and not the old wiki.
        // This tests the XObject cache.
        DocumentReference targetXClassReference = new DocumentReference("copywiki", DOCSPACE, DOCNAME);
        assertNotNull(copy.getXObject(targetXClassReference));

        // Also verify that actual XObject's reference (not from the cache).
        assertEquals(1, copy.getXObjects().size());
        BaseObject bobject = copy.getXObjects().get(copy.getXObjects().keySet().iterator().next()).get(0);
        assertEquals(new DocumentReference("copywiki", DOCSPACE, DOCNAME), bobject.getXClassReference());
    }

    @Test
    void testCustomMappingAfterDocumentCopy() throws Exception
    {
        this.document.getXClass().setCustomMapping("internal");

        XWikiDocument copy = this.document.copyDocument(new DocumentReference("copywiki", "copyspace", "copypage"),
            this.oldcore.getXWikiContext());

        assertEquals("", copy.getXClass().getCustomMapping());
    }

    /**
     * Normally the xobject vector has the Nth object on the Nth position, but in case an object gets misplaced, trying
     * to remove it should indeed remove that object, and no other.
     */
    @Test
    void testRemovingObjectWithWrongObjectVector()
    {
        // Setup: Create a document and two xobjects
        BaseObject o1 = new BaseObject();
        BaseObject o2 = new BaseObject();
        o1.setXClassReference(CLASS_REFERENCE);
        o2.setXClassReference(CLASS_REFERENCE);

        // Test: put the second xobject on the third position
        // addObject creates the object vector and configures the objects
        // o1 is added at position 0
        // o2 is added at position 1
        XWikiDocument doc = new XWikiDocument(DOCUMENT_REFERENCE);
        doc.addXObject(o1);
        doc.addXObject(o2);

        // Modify the o2 object's position to ensure it can still be found and removed by the removeObject method.
        assertEquals(1, o2.getNumber());
        o2.setNumber(0);
        // Set a field on o1 so that when comparing it with o2 they are different. This is needed so that the remove
        // will pick the right object to remove (since we've voluntarily set a wrong number of o2 it would pick o1
        // if they were equals).
        o1.addField("somefield", new StringProperty());

        // Call the tested method, removing o2 from position 2 which is set to null
        boolean result = doc.removeXObject(o2);

        // Check the correct behavior:
        assertTrue(result);
        List<BaseObject> objects = doc.getXObjects(CLASS_REFERENCE);
        assertTrue(objects.contains(o1));
        assertFalse(objects.contains(o2));
        assertNull(objects.get(1));

        // Second test: swap the two objects, so that the first object is in the position the second should have
        // Start over, re-adding the two objects
        doc = new XWikiDocument(DOCUMENT_REFERENCE);
        doc.addXObject(o1);
        doc.addXObject(o2);
    }

    @Test
    void testCopyDocument() throws XWikiException
    {
        DocumentReference oldReference =
            new DocumentReference(CLASS_REFERENCE.getWikiReference().getName(), "space1", "document1");
        DocumentReference newReference =
            new DocumentReference(CLASS_REFERENCE.getWikiReference().getName(), "space2", "document2");

        XWikiDocument doc = new XWikiDocument(oldReference);
        doc.setTitle("Some title");
        BaseObject o = new BaseObject();
        o.setXClassReference(CLASS_REFERENCE);
        doc.addXObject(o);
        doc.setLocale(Locale.ENGLISH);
        doc.setNew(false);

        XWikiDocument newDoc = doc.copyDocument(newReference, this.oldcore.getXWikiContext());
        BaseObject newO = newDoc.getXObject(CLASS_REFERENCE);

        assertNotSame(o, newDoc.getXObject(CLASS_REFERENCE));
        assertFalse(newO.getGuid().equals(o.getGuid()));
        // Verify that the title is copied
        assertEquals("Some title", newDoc.getTitle());
        assertEquals(Locale.ENGLISH, newDoc.getLocale());
        assertEquals(newReference, newDoc.getDocumentReference());
        assertEquals(new DocumentReference(newReference, Locale.ENGLISH), newDoc.getDocumentReferenceWithLocale());
        assertTrue(newDoc.isNew());
    }

    /**
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-6743">XWIKI-6743</a>
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-12349">XWIKI-12349</a>
     */
    @Test
    void testCopyDocumentSetsTitleToNewDocNameIfPreviouslySetToDocName() throws XWikiException
    {
        copyDocumentAndAssertTitle(new DocumentReference("wiki1", "space1", "page1"), "page1",
            new DocumentReference("wiki2", "space2", "page2"), "page2");

        copyDocumentAndAssertTitle(new DocumentReference("wiki1", "space1", "WebHome"), "space1",
            new DocumentReference("wiki2", "space2", "page2"), "page2");

        copyDocumentAndAssertTitle(new DocumentReference("wiki1", "space1", "WebHome"), "space1",
            new DocumentReference("wiki2", "space2", "WebHome"), "space2");
    }

    private void copyDocumentAndAssertTitle(DocumentReference oldReference, String oldTitle,
        DocumentReference newReference, String expectedNewTitle) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(oldReference);
        doc.setTitle(oldTitle);

        XWikiDocument newDoc = doc.copyDocument(newReference, this.oldcore.getXWikiContext());

        // Verify that we get the expected title.
        assertEquals(expectedNewTitle, newDoc.getTitle());
    }

    @Test
    void testValidate() throws XWikiException, AccessDeniedException
    {
        this.document.setValidationScript("validationScript");
        this.baseClass.setValidationScript("validationScript");

        doReturn(new XWikiValidationInterface()
        {
            @Override
            public boolean validateObject(BaseObject object, XWikiContext context)
            {
                return true;
            }

            @Override
            public boolean validateDocument(XWikiDocument doc, XWikiContext context)
            {
                return true;
            }
        }).when(this.oldcore.getSpyXWiki()).parseGroovyFromPage("validationScript", this.oldcore.getXWikiContext());

        // With PR

        assertTrue(this.document.validate(this.oldcore.getXWikiContext()));
        assertTrue(this.baseClass.validateObject(this.baseObject, this.oldcore.getXWikiContext()));

        // Without PR

        doThrow(AccessDeniedException.class).when(this.oldcore.getMockContextualAuthorizationManager())
            .checkAccess(Right.PROGRAM, new DocumentReference("wiki", "space", "validationScript"));

        assertFalse(this.document.validate(this.oldcore.getXWikiContext()));
        assertFalse(this.baseClass.validateObject(this.baseObject, this.oldcore.getXWikiContext()));
    }

    @Test
    void tofromXMLDocument() throws XWikiException
    {
        // equals won't work on password fields because of https://jira.xwiki.org/browse/XWIKI-12561
        this.baseClass.removeField("passwd");
        this.baseObject.removeField("passwd");
        this.baseObject2.removeField("passwd");
        this.oldcore.getSpyXWiki().saveDocument(this.document, "", true, this.oldcore.getXWikiContext());

        Document document = this.document.toXMLDocument(this.oldcore.getXWikiContext());

        XWikiDocument newDocument = new XWikiDocument(this.document.getDocumentReference());
        newDocument.fromXML(document, false);

        assertEquals(this.document, newDocument);
        // Assert that the document restored from XML is restricted in contrast to the original document.
        assertFalse(this.document.isRestricted());
        assertTrue(newDocument.isRestricted());
    }

    @Test
    void getAttachmentWithNullFilename() throws XWikiException
    {
        assertNull(this.document.getAttachment(null));
    }

    @Test
    void listAdd() throws XWikiException
    {
        // reset
        attachmentList.clear();
        // add without index
        XWikiAttachment attachment = new XWikiAttachment(this.document, "testAttachment");
        attachmentList.add(attachment);
        assertTrue(this.document.getAttachmentList().contains(attachment));
        assertTrue(this.document.getAttachment("testAttachment") == attachment);
        assertTrue(((XWikiAttachmentList) (attachmentList)).getByFilename("testAttachment") == attachment);
        assertFalse(attachmentList.add(attachment));
        assertTrue(attachmentList.size() == 1);

        // add using index
        XWikiAttachment attachment2 = new XWikiAttachment(this.document, "testAttachment2");
        attachmentList.add(0, attachment2);
        assertTrue(this.document.getAttachmentList().contains(attachment2));
        assertTrue(this.document.getAttachment("testAttachment2") == attachment2);
        assertTrue(((XWikiAttachmentList) (attachmentList)).getByFilename("testAttachment2") == attachment2);
    }

    @Test
    void listMaintainsOrder() throws XWikiException
    {
        XWikiAttachment attachment1 = new XWikiAttachment(this.document, "attachmentA");
        XWikiAttachment attachment2 = new XWikiAttachment(this.document, "attachmentB");
        XWikiAttachment attachment3 = new XWikiAttachment(this.document, "attachmentC");
        attachmentList.add(attachment3);
        attachmentList.add(attachment1);
        attachmentList.add(attachment2);
        List<XWikiAttachment> list = new ArrayList<XWikiAttachment>();
        list.add(attachment1);
        list.add(attachment2);
        list.add(attachment3);
        assertEquals(this.document.getAttachmentList(), list);
    }

    @Test
    void listClear() throws XWikiException
    {
        attachmentList.clear();
        assertTrue(attachmentList.isEmpty());
    }

    @Test
    void listRemove() throws XWikiException
    {
        // remove through object parameter
        XWikiAttachment attachment = new XWikiAttachment(this.document, "remove");
        attachmentList.add(attachment);
        attachmentList.remove(attachment);
        assertFalse(attachmentList.contains(attachment));

        // remove through index parameter
        XWikiAttachment attachment2 = new XWikiAttachment(this.document, "remove");
        attachmentList.add(attachment2);
        attachmentList.remove(0);
        assertFalse(attachmentList.contains(attachment2));

        // remove attachment that is not in the list
        assertFalse(attachmentList.remove(attachment));

    }

    @Test
    void listSet() throws XWikiException
    {
        XWikiAttachment attachment = new XWikiAttachment(this.document, "testAttachment");
        attachmentList.set(0, attachment);
        assertTrue(this.document.getAttachmentList().contains(attachment));
        assertTrue(this.document.getAttachment("testAttachment") == attachment);
        XWikiAttachment attachment2 = new XWikiAttachment(this.document, "testAttachment");
        attachmentList.set(0, attachment2);
        assertTrue(this.document.getAttachmentList().contains(attachment2));
        assertFalse(this.document.getAttachmentList().contains(attachment));
        assertFalse(this.document.getAttachment("testAttachment") == attachment);
        assertTrue(this.document.getAttachment("testAttachment") == attachment2);
    }

    @Test
    void listAddAll() throws XWikiException
    {
        ArrayList<XWikiAttachment> list = new ArrayList<XWikiAttachment>();
        XWikiAttachment attachment1 = new XWikiAttachment(this.document, "attachmentA");
        XWikiAttachment attachment2 = new XWikiAttachment(this.document, "attachmentB");
        XWikiAttachment attachment3 = new XWikiAttachment(this.document, "attachmentC");
        list.add(attachment1);
        list.add(attachment2);
        list.add(attachment3);
        attachmentList.addAll(list);
        assertTrue(attachmentList.contains(attachment1));
        assertTrue(attachmentList.contains(attachment2));
        assertTrue(attachmentList.contains(attachment3));

        // reset
        attachmentList.clear();
        attachmentList.addAll(0, list);
        assertTrue(attachmentList.contains(attachment1));
        assertTrue(attachmentList.contains(attachment2));
        assertTrue(attachmentList.contains(attachment3));

    }

    @Test
    void listRemoveAll() throws XWikiException
    {
        ArrayList<XWikiAttachment> list = new ArrayList<XWikiAttachment>();
        XWikiAttachment attachment1 = new XWikiAttachment(this.document, "attachmentA");
        XWikiAttachment attachment2 = new XWikiAttachment(this.document, "attachmentB");
        XWikiAttachment attachment3 = new XWikiAttachment(this.document, "attachmentC");
        XWikiAttachment attachment4 = new XWikiAttachment(this.document, "attachmentD");
        list.add(attachment1);
        list.add(attachment2);
        list.add(attachment3);
        attachmentList.addAll(list);
        attachmentList.add(attachment4);
        attachmentList.removeAll(list);
        assertFalse(attachmentList.contains(attachment1));
        assertFalse(attachmentList.contains(attachment2));
        assertFalse(attachmentList.contains(attachment3));
        assertTrue(attachmentList.contains(attachment4));
    }

    @Test
    void listRetainAll() throws XWikiException
    {
        ArrayList<XWikiAttachment> list = new ArrayList<XWikiAttachment>();
        XWikiAttachment attachment1 = new XWikiAttachment(this.document, "attachmentA");
        XWikiAttachment attachment2 = new XWikiAttachment(this.document, "attachmentB");
        XWikiAttachment attachment3 = new XWikiAttachment(this.document, "attachmentC");
        XWikiAttachment attachment4 = new XWikiAttachment(this.document, "attachmentD");
        list.add(attachment1);
        list.add(attachment2);
        list.add(attachment3);
        attachmentList.addAll(list);
        attachmentList.add(attachment4);
        attachmentList.retainAll(list);
        assertTrue(attachmentList.contains(attachment1));
        assertTrue(attachmentList.contains(attachment2));
        assertTrue(attachmentList.contains(attachment3));
        assertFalse(attachmentList.contains(attachment4));
    }

    @Test
    void modifyAttachmentName()
    {
        XWikiAttachment attachment = new XWikiAttachment();

        this.document.getAttachmentList().add(attachment);

        assertSame(1, this.document.getAttachmentList().size());
        assertSame(attachment, this.document.getAttachmentList().get(0));

        attachment.setFilename("attachment");

        assertSame(1, this.document.getAttachmentList().size());
        assertSame(attachment, this.document.getAttachmentList().get(0));
        assertSame(attachment, this.document.getAttachment("attachment"));

        attachment.setFilename("attachment2");

        assertSame(1, this.document.getAttachmentList().size());
        assertSame(attachment, this.document.getAttachmentList().get(0));
        assertNull(this.document.getAttachment("attachment"));
        assertSame(attachment, this.document.getAttachment("attachment2"));
    }

    @Test
    void getMetaDataDiff() throws Exception
    {
        XWikiDocument prevDoc = new XWikiDocument(DOCUMENT_REFERENCE);
        XWikiDocument nextDoc = new XWikiDocument(DOCUMENT_REFERENCE);
        XWikiDocument thisDoc = new XWikiDocument(DOCUMENT_REFERENCE);

        List<MetaDataDiff> diff = thisDoc.getMetaDataDiff(prevDoc, nextDoc, this.oldcore.getXWikiContext());
        assertEquals(0, diff.size());

        UserReference alice = mock(UserReference.class, "alice");
        prevDoc.getAuthors().setOriginalMetadataAuthor(alice);

        UserReference bob = mock(UserReference.class, "bob");
        nextDoc.getAuthors().setOriginalMetadataAuthor(bob);

        when(this.compactWikiUserReferenceSerializer.serialize(alice, DOCUMENT_REFERENCE)).thenReturn("XWiki.alice");
        when(this.compactWikiUserReferenceSerializer.serialize(bob, DOCUMENT_REFERENCE)).thenReturn("XWiki.bob");

        diff = thisDoc.getMetaDataDiff(prevDoc, nextDoc, this.oldcore.getXWikiContext());
        assertEquals(1, diff.size());
        assertEquals("author", diff.get(0).getField());
        assertEquals("XWiki.alice", diff.get(0).getPrevValue());
        assertEquals("XWiki.bob", diff.get(0).getNewValue());
    }

    @Test
    void getBackLinkedReferences() throws XWikiException, LinkException
    {
        XWikiDocument doc = new XWikiDocument(DOCUMENT_REFERENCE);

        DocumentReference backlink1 = new DocumentReference("wiki", "space", "page1");
        DocumentReference backlink21 = new DocumentReference("wiki", "space", "page2", Locale.ENGLISH);
        DocumentReference backlink22 = new DocumentReference("wiki", "space", "page2", Locale.FRENCH);

        assertEquals(Set.of(), new HashSet<>(doc.getBackLinkedReferences(this.oldcore.getXWikiContext())));

        when(this.linkStore.resolveBackLinkedEntities(DOCUMENT_REFERENCE))
            .thenReturn(Set.of(backlink1, backlink21, backlink22));

        assertEquals(Set.of(backlink1, backlink21.withoutLocale()),
            new HashSet<>(doc.getBackLinkedReferences(this.oldcore.getXWikiContext())));
    }
}
