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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;

import junit.framework.Assert;

import org.apache.velocity.VelocityContext;
import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.context.Execution;
import org.xwiki.display.internal.DisplayConfiguration;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.DocumentSection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.XWikiMessageTool;

/**
 * Unit tests for {@link XWikiDocument}.
 * 
 * @version $Id$
 */
public class XWikiDocumentTest extends AbstractBridgedXWikiComponentTestCase
{
    private static final String DOCWIKI = "Wiki";

    private static final String DOCSPACE = "Space";

    private static final String DOCNAME = "Page";

    private static final String DOCFULLNAME = DOCSPACE + "." + DOCNAME;

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME);

    private static final String CLASSNAME = DOCFULLNAME;

    private static final DocumentReference CLASS_REFERENCE = DOCUMENT_REFERENCE;

    private XWikiDocument document;

    private XWikiDocument translatedDocument;

    private Mock mockXWiki;

    private Mock mockXWikiRenderingEngine;

    private Mock mockXWikiVersioningStore;

    private Mock mockXWikiStoreInterface;

    private Mock mockXWikiMessageTool;

    private Mock mockXWikiRightService;

    private Mock mockVelocityManager;

    private Mock mockVelocityEngine;

    private Mock mockDisplayConfiguration;

    private CustomStub velocityEngineEvaluateStub;

    private BaseClass baseClass;

    private BaseObject baseObject;

    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.defaultEntityReferenceSerializer = getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);

        this.document = new XWikiDocument(new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME));
        this.document.setSyntax(Syntax.XWIKI_1_0);
        this.document.setLanguage("en");
        this.document.setDefaultLanguage("en");
        this.document.setNew(false);

        this.translatedDocument = new XWikiDocument();
        this.translatedDocument.setSyntax(Syntax.XWIKI_2_0);
        this.translatedDocument.setLanguage("fr");
        this.translatedDocument.setNew(false);

        getContext().put("isInRenderingEngine", true);

        this.mockXWiki = mock(XWiki.class);

        this.mockXWikiRenderingEngine = mock(XWikiRenderingEngine.class);

        this.mockXWikiVersioningStore = mock(XWikiVersioningStoreInterface.class);
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(returnValue(null));

        this.mockXWikiStoreInterface = mock(XWikiStoreInterface.class);
        this.document.setStore((XWikiStoreInterface) this.mockXWikiStoreInterface.proxy());

        this.mockXWikiMessageTool =
            mock(XWikiMessageTool.class, new Class[] {ResourceBundle.class, XWikiContext.class}, new Object[] {null,
            getContext()});
        this.mockXWikiMessageTool.stubs().method("get").will(returnValue("message"));

        this.mockXWikiRightService = mock(XWikiRightService.class);
        this.mockXWikiRightService.stubs().method("hasProgrammingRights").will(returnValue(true));

        this.mockXWiki.stubs().method("getRenderingEngine").will(returnValue(this.mockXWikiRenderingEngine.proxy()));
        this.mockXWiki.stubs().method("getVersioningStore").will(returnValue(this.mockXWikiVersioningStore.proxy()));
        this.mockXWiki.stubs().method("getStore").will(returnValue(this.mockXWikiStoreInterface.proxy()));
        this.mockXWiki.stubs().method("getDocument").will(returnValue(this.document));
        this.mockXWiki.stubs().method("getLanguagePreference").will(returnValue("en"));
        this.mockXWiki.stubs().method("getSectionEditingDepth").will(returnValue(2L));
        this.mockXWiki.stubs().method("getRightService").will(returnValue(this.mockXWikiRightService.proxy()));
        this.mockXWiki.stubs().method("isVirtualMode").will(returnValue(false));
        this.mockXWiki.stubs().method("exists").will(returnValue(false));
        this.mockXWiki.stubs().method("evaluateTemplate").will(returnValue(""));

        getContext().setWiki((XWiki) this.mockXWiki.proxy());
        getContext().put("msg", this.mockXWikiMessageTool.proxy());

        this.baseClass = this.document.getXClass();
        this.baseClass.addTextField("string", "String", 30);
        this.baseClass.addTextAreaField("area", "Area", 10, 10);
        this.baseClass.addTextAreaField("puretextarea", "Pure text area", 10, 10);
        // set the text areas an non interpreted content
        ((TextAreaClass) this.baseClass.getField("puretextarea")).setContentType("puretext");
        this.baseClass.addPasswordField("passwd", "Password", 30);
        this.baseClass.addBooleanField("boolean", "Boolean", "yesno");
        this.baseClass.addNumberField("int", "Int", 10, "integer");
        this.baseClass.addStaticListField("stringlist", "StringList", "value1, value2");

        this.mockXWiki.stubs().method("getClass").will(returnValue(this.baseClass));
        this.mockXWiki.stubs().method("getXClass").will(returnValue(this.baseClass));

        this.baseObject = this.document.newObject(CLASSNAME, getContext());
        this.baseObject.setStringValue("string", "string");
        this.baseObject.setLargeStringValue("area", "area");
        this.baseObject.setStringValue("passwd", "passwd");
        this.baseObject.setIntValue("boolean", 1);
        this.baseObject.setIntValue("int", 42);
        this.baseObject.setStringListValue("stringlist", Arrays.asList("VALUE1", "VALUE2"));

        this.mockXWikiStoreInterface.stubs().method("search").will(returnValue(new ArrayList<XWikiDocument>()));
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        // Setup display configuration.
        this.mockDisplayConfiguration = registerMockComponent(DisplayConfiguration.class);
        this.mockDisplayConfiguration.stubs().method("getDocumentDisplayerHint").will(returnValue("default"));
        this.mockDisplayConfiguration.stubs().method("getTitleHeadingDepth").will(returnValue(2));

        // Setup the mock Velocity engine.
        this.mockVelocityManager = registerMockComponent(VelocityManager.class);
        this.mockVelocityEngine = mock(VelocityEngine.class);
        this.mockVelocityManager.stubs().method("getVelocityContext").will(returnValue(null));
        this.mockVelocityManager.stubs().method("getVelocityEngine").will(returnValue(this.mockVelocityEngine.proxy()));
        velocityEngineEvaluateStub = new CustomStub("Implements VelocityEngine.evaluate")
        {
            public Object invoke(Invocation invocation) throws Throwable
            {
                // Output the given text without changes.
                StringWriter writer = (StringWriter) invocation.parameterValues.get(1);
                String text = (String) invocation.parameterValues.get(3);
                writer.append(text);
                return true;
            }
        };
        this.mockVelocityEngine.stubs().method("evaluate").will(velocityEngineEvaluateStub);
        this.mockVelocityEngine.stubs().method("startedUsingMacroNamespace");
        this.mockVelocityEngine.stubs().method("stoppedUsingMacroNamespace");
    }

    public void testDeprecatedConstructors()
    {
        DocumentReference defaultReference = new DocumentReference("xwiki", "Main", "WebHome");

        XWikiDocument doc = new XWikiDocument(null);
        assertEquals(defaultReference, doc.getDocumentReference());

        doc = new XWikiDocument();
        assertEquals(defaultReference, doc.getDocumentReference());

        doc = new XWikiDocument("notused", "space.page");
        assertEquals("space", doc.getSpaceName());
        assertEquals("page", doc.getPageName());
        assertEquals("xwiki", doc.getWikiName());

        doc = new XWikiDocument("space", "page");
        assertEquals("space", doc.getSpaceName());
        assertEquals("page", doc.getPageName());
        assertEquals("xwiki", doc.getWikiName());

        doc = new XWikiDocument("wiki", "space", "page");
        assertEquals("space", doc.getSpaceName());
        assertEquals("page", doc.getPageName());
        assertEquals("wiki", doc.getWikiName());

        doc = new XWikiDocument("wiki", "notused", "notused:space.page");
        assertEquals("space", doc.getSpaceName());
        assertEquals("page", doc.getPageName());
        assertEquals("wiki", doc.getWikiName());
    }

    public void testGetRenderedTitleWhenMatchingTitleHeaderDepth()
    {
        this.document.setContent("=== level3");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        // Overwrite the title heading depth.
        this.mockDisplayConfiguration.stubs().method("getTitleHeadingDepth").will(returnValue(3));

        assertEquals("level3", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));
    }

    public void testGetRenderedTitleWhenNotMatchingTitleHeaderDepth()
    {
        this.document.setContent("=== level3");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("Page", this.document.getRenderedTitle(Syntax.XHTML_1_0, getContext()));
    }

    public void testMinorMajorVersions()
    {
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

    public void testGetPreviousVersion() throws XWikiException
    {
        this.mockXWiki.stubs().method("getEncoding").will(returnValue("UTF-8"));
        this.mockXWiki.stubs().method("getConfig").will(returnValue(new XWikiConfig()));
        XWikiContext context = this.getContext();
        Date now = new Date();
        XWikiDocumentArchive archiveDoc = new XWikiDocumentArchive(this.document.getId());
        this.document.setDocumentArchive(archiveDoc);

        assertEquals("1.1", this.document.getVersion());
        assertNull(this.document.getPreviousVersion());

        this.document.incrementVersion();
        archiveDoc.updateArchive(this.document, "Admin", now, "", this.document.getRCSVersion(), context);
        assertEquals("1.1", this.document.getVersion());
        assertNull(this.document.getPreviousVersion());

        this.document.setMinorEdit(true);
        this.document.incrementVersion();
        archiveDoc.updateArchive(this.document, "Admin", now, "", this.document.getRCSVersion(), context);
        assertEquals("1.2", this.document.getVersion());
        assertEquals("1.1", this.document.getPreviousVersion());

        this.document.setMinorEdit(false);
        this.document.incrementVersion();
        archiveDoc.updateArchive(this.document, "Admin", now, "", this.document.getRCSVersion(), context);
        assertEquals("2.1", this.document.getVersion());
        assertEquals("1.2", this.document.getPreviousVersion());

        this.document.setMinorEdit(true);
        this.document.incrementVersion();
        archiveDoc.updateArchive(this.document, "Admin", now, "", this.document.getRCSVersion(), context);
        assertEquals("2.2", this.document.getVersion());
        assertEquals("2.1", this.document.getPreviousVersion());

        archiveDoc.resetArchive();

        assertEquals("2.2", this.document.getVersion());
        assertNull(this.document.getPreviousVersion());
    }

    public void testAuthorAfterDocumentCopy() throws XWikiException
    {
        DocumentReference author = new DocumentReference("Wiki", "XWiki", "Albatross");
        this.document.setAuthorReference(author);
        XWikiDocument copy = this.document.copyDocument(this.document.getName() + " Copy", getContext());

        assertEquals(author, copy.getAuthorReference());
    }

    public void testCreatorAfterDocumentCopy() throws XWikiException
    {
        DocumentReference creator = new DocumentReference("Wiki", "XWiki", "Condor");
        this.document.setCreatorReference(creator);
        XWikiDocument copy = this.document.copyDocument(this.document.getName() + " Copy", getContext());

        assertEquals(creator, copy.getCreatorReference());
    }

    public void testCreationDateAfterDocumentCopy() throws Exception
    {
        Date sourceCreationDate = this.document.getCreationDate();
        Thread.sleep(1000);
        XWikiDocument copy = this.document.copyDocument(this.document.getName() + " Copy", getContext());

        assertEquals(sourceCreationDate, copy.getCreationDate());
    }

    public void testObjectGuidsAfterDocumentCopy() throws Exception
    {
        assertTrue(this.document.getXObjects().size() > 0);

        List<String> originalGuids = new ArrayList<String>();
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : this.document.getXObjects().entrySet()) {
            for (BaseObject baseObject : entry.getValue()) {
                originalGuids.add(baseObject.getGuid());
            }
        }

        XWikiDocument copy = this.document.copyDocument(this.document.getName() + " Copy", getContext());

        // Verify that the cloned objects have different GUIDs
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : copy.getXObjects().entrySet()) {
            for (BaseObject baseObject : entry.getValue()) {
                assertFalse("Non unique object GUID found!", originalGuids.contains(baseObject.getGuid()));
            }
        }
    }

    public void testRelativeObjectReferencesAfterDocumentCopy() throws Exception
    {
        XWikiDocument copy =
            this.document.copyDocument(new DocumentReference("copywiki", "copyspace", "copypage"), getContext());

        // Verify that the XObject's XClass reference points to the target wiki and not the old wiki.
        // This tests the XObject cache.
        DocumentReference targetXClassReference = new DocumentReference("copywiki", DOCSPACE, DOCNAME);
        assertNotNull(copy.getXObject(targetXClassReference));

        // Also verify that actual XObject's reference (not from the cache).
        assertEquals(1, copy.getXObjects().size());
        BaseObject bobject = copy.getXObjects().get(copy.getXObjects().keySet().iterator().next()).get(0);
        assertEquals(new DocumentReference("copywiki", DOCSPACE, DOCNAME), bobject.getXClassReference());
    }

    public void testCloneNullObjects() throws XWikiException
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

    public void testToStringReturnsFullName()
    {
        assertEquals("Space.Page", this.document.toString());
        assertEquals("Main.WebHome", new XWikiDocument().toString());
    }

    public void testCloneSaveVersions()
    {
        XWikiDocument doc1 = new XWikiDocument(new DocumentReference("qwe", "qwe", "qwe"));
        XWikiDocument doc2 = doc1.clone();
        doc1.incrementVersion();
        doc2.incrementVersion();
        assertEquals(doc1.getVersion(), doc2.getVersion());
    }

    public void testAddObject() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("test", "test", "document"));
        this.mockXWiki.stubs().method("getClass").will(returnValue(new BaseClass()));
        BaseObject object = BaseClass.newCustomClassInstance("XWiki.XWikiUsers", getContext());
        doc.addObject("XWiki.XWikiUsers", object);
        assertEquals("XWikiDocument.addObject does not set the object's name", doc.getFullName(), object.getName());
    }

    public void testObjectNumbersAfterXMLRoundrip() throws XWikiException
    {
        String classname = XWikiConstant.TAG_CLASS;
        BaseClass tagClass = new BaseClass();
        tagClass.setName(classname);
        tagClass.addStaticListField(XWikiConstant.TAG_CLASS_PROP_TAGS, "Tags", 30, true, "", "checkbox");

        XWikiDocument doc = new XWikiDocument(new DocumentReference("test", "test", "document"));
        this.mockXWiki.stubs().method("getXClass").will(returnValue(tagClass));
        this.mockXWiki.stubs().method("getEncoding").will(returnValue("iso-8859-1"));

        BaseObject object = BaseClass.newCustomClassInstance(classname, getContext());
        object.setClassName(classname);
        doc.addObject(classname, object);

        object = BaseClass.newCustomClassInstance(classname, getContext());
        object.setClassName(classname);
        doc.addObject(classname, object);

        object = BaseClass.newCustomClassInstance(classname, getContext());
        object.setClassName(classname);
        doc.addObject(classname, object);

        doc.setObject(classname, 1, null);

        String docXML = doc.toXML(getContext());
        XWikiDocument docFromXML = new XWikiDocument();
        docFromXML.fromXML(docXML);

        Vector<BaseObject> objects = doc.getObjects(classname);
        Vector<BaseObject> objectsFromXML = docFromXML.getObjects(classname);

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

    public void testGetUniqueLinkedPages10()
    {
        XWikiDocument contextDocument =
            new XWikiDocument(new DocumentReference("contextdocwiki", "contextdocspace", "contextdocpage"));
        getContext().setDoc(contextDocument);

        this.mockXWiki.stubs().method("exists").will(returnValue(true));

        this.document.setContent("[TargetPage][TargetLabel>TargetPage][TargetSpace.TargetPage]"
            + "[TargetLabel>TargetSpace.TargetPage?param=value#anchor][http://externallink][mailto:mailto][label>]");

        Set<String> linkedPages = this.document.getUniqueLinkedPages(getContext());

        assertEquals(new HashSet<String>(Arrays.asList("TargetPage", "TargetSpace.TargetPage")), new HashSet<String>(
            linkedPages));
    }

    public void testGetUniqueLinkedPages()
    {
        XWikiDocument contextDocument =
            new XWikiDocument(new DocumentReference("contextdocwiki", "contextdocspace", "contextdocpage"));
        getContext().setDoc(contextDocument);

        this.document.setContent("[[TargetPage]][[TargetLabel>>TargetPage]][[TargetSpace.TargetPage]]"
            + "[[TargetLabel>>TargetSpace.TargetPage?param=value#anchor]][[http://externallink]][[mailto:mailto]]"
            + "[[]][[#anchor]][[?param=value]][[targetwiki:TargetSpace.TargetPage]]");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        Set<String> linkedPages = this.document.getUniqueLinkedPages(getContext());

        assertEquals(
            new LinkedHashSet<String>(Arrays.asList("Space.TargetPage", "TargetSpace.TargetPage",
                "targetwiki:TargetSpace.TargetPage")), linkedPages);
    }

    public void testGetSections10() throws XWikiException
    {
        this.document.setContent("content not in section\n" + "1 header 1\nheader 1 content\n"
            + "1.1 header 2\nheader 2 content");

        List<DocumentSection> headers = this.document.getSections();

        assertEquals(2, headers.size());

        DocumentSection header1 = headers.get(0);
        DocumentSection header2 = headers.get(1);

        assertEquals("header 1", header1.getSectionTitle());
        assertEquals(23, header1.getSectionIndex());
        assertEquals(1, header1.getSectionNumber());
        assertEquals("1", header1.getSectionLevel());
        assertEquals("header 2", header2.getSectionTitle());
        assertEquals(51, header2.getSectionIndex());
        assertEquals(2, header2.getSectionNumber());
        assertEquals("1.1", header2.getSectionLevel());
    }

    public void testGetSections() throws XWikiException
    {
        this.document.setContent("content not in section\n" + "= header 1=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        List<DocumentSection> headers = this.document.getSections();

        assertEquals(2, headers.size());

        DocumentSection header1 = headers.get(0);
        DocumentSection header2 = headers.get(1);

        assertEquals("header 1", header1.getSectionTitle());
        assertEquals(-1, header1.getSectionIndex());
        assertEquals(1, header1.getSectionNumber());
        assertEquals("1", header1.getSectionLevel());
        assertEquals("header 2", header2.getSectionTitle());
        assertEquals(-1, header2.getSectionIndex());
        assertEquals(2, header2.getSectionNumber());
        assertEquals("1.1", header2.getSectionLevel());
    }

    public void testGetDocumentSection10() throws XWikiException
    {
        this.document.setContent("content not in section\n" + "1 header 1\nheader 1 content\n"
            + "1.1 header 2\nheader 2 content");

        DocumentSection header1 = this.document.getDocumentSection(1);
        DocumentSection header2 = this.document.getDocumentSection(2);

        assertEquals("header 1", header1.getSectionTitle());
        assertEquals(23, header1.getSectionIndex());
        assertEquals(1, header1.getSectionNumber());
        assertEquals("1", header1.getSectionLevel());
        assertEquals("header 2", header2.getSectionTitle());
        assertEquals(51, header2.getSectionIndex());
        assertEquals(2, header2.getSectionNumber());
        assertEquals("1.1", header2.getSectionLevel());
    }

    public void testGetDocumentSection() throws XWikiException
    {
        this.document.setContent("content not in section\n" + "= header 1=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        DocumentSection header1 = this.document.getDocumentSection(1);
        DocumentSection header2 = this.document.getDocumentSection(2);

        assertEquals("header 1", header1.getSectionTitle());
        assertEquals(-1, header1.getSectionIndex());
        assertEquals(1, header1.getSectionNumber());
        assertEquals("1", header1.getSectionLevel());
        assertEquals("header 2", header2.getSectionTitle());
        assertEquals(-1, header2.getSectionIndex());
        assertEquals(2, header2.getSectionNumber());
        assertEquals("1.1", header2.getSectionLevel());
    }

    public void testGetContentOfSection10() throws XWikiException
    {
        this.document.setContent("content not in section\n" + "1 header 1\nheader 1 content\n"
            + "1.1 header 2\nheader 2 content");

        String content1 = this.document.getContentOfSection(1);
        String content2 = this.document.getContentOfSection(2);

        assertEquals("1 header 1\nheader 1 content\n1.1 header 2\nheader 2 content", content1);
        assertEquals("1.1 header 2\nheader 2 content", content2);
    }

    public void testGetContentOfSection() throws XWikiException
    {
        this.document.setContent("content not in section\n" + "= header 1=\nheader 1 content\n"
            + "== header 2==\nheader 2 content\n" + "=== header 3===\nheader 3 content\n"
            + "== header 4==\nheader 4 content");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        String content1 = this.document.getContentOfSection(1);
        String content2 = this.document.getContentOfSection(2);
        String content3 = this.document.getContentOfSection(3);

        assertEquals("= header 1 =\n\nheader 1 content\n\n== header 2 ==\n\nheader 2 content\n\n"
            + "=== header 3 ===\n\nheader 3 content\n\n== header 4 ==\n\nheader 4 content", content1);
        assertEquals("== header 2 ==\n\nheader 2 content\n\n=== header 3 ===\n\nheader 3 content", content2);
        assertEquals("== header 4 ==\n\nheader 4 content", content3);

        // Validate that third level header is not skipped anymore
        this.mockXWiki.stubs().method("getSectionEditingDepth").will(returnValue(3L));

        content3 = this.document.getContentOfSection(3);
        String content4 = this.document.getContentOfSection(4);

        assertEquals("=== header 3 ===\n\nheader 3 content", content3);
        assertEquals("== header 4 ==\n\nheader 4 content", content4);
    }

    public void testSectionSplit10() throws XWikiException
    {
        List<DocumentSection> sections;
        // Simple test
        this.document.setContent("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
            + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
        sections = this.document.getSections();
        assertEquals(3, sections.size());
        assertEquals("Section 1", sections.get(0).getSectionTitle());
        assertEquals("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
            + "Content of second section\n", this.document.getContentOfSection(1));
        assertEquals("1.1", sections.get(1).getSectionLevel());
        assertEquals("1.1 Subsection 2\nContent of second section\n", this.document.getContentOfSection(2));
        assertEquals(3, sections.get(2).getSectionNumber());
        assertEquals(80, sections.get(2).getSectionIndex());
        assertEquals("1 Section 3\nContent of section 3", this.document.getContentOfSection(3));
        // Test comments don't break the section editing
        this.document.setContent("1 Section 1\n" + "Content of first section\n" + "## 1.1 Subsection 2\n"
            + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
        sections = this.document.getSections();
        assertEquals(2, sections.size());
        assertEquals("Section 1", sections.get(0).getSectionTitle());
        assertEquals("1", sections.get(1).getSectionLevel());
        assertEquals(2, sections.get(1).getSectionNumber());
        assertEquals(83, sections.get(1).getSectionIndex());
        // Test spaces are ignored
        this.document.setContent("1 Section 1\n" + "Content of first section\n" + "   1.1    Subsection 2  \n"
            + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
        sections = this.document.getSections();
        assertEquals(3, sections.size());
        assertEquals("Subsection 2  ", sections.get(1).getSectionTitle());
        assertEquals("1.1", sections.get(1).getSectionLevel());
        // Test lower headings are ignored
        this.document.setContent("1 Section 1\n" + "Content of first section\n" + "1.1.1 Lower subsection\n"
            + "This content is not important\n" + "   1.1    Subsection 2  \n" + "Content of second section\n"
            + "1 Section 3\n" + "Content of section 3");
        sections = this.document.getSections();
        assertEquals(3, sections.size());
        assertEquals("Section 1", sections.get(0).getSectionTitle());
        assertEquals("Subsection 2  ", sections.get(1).getSectionTitle());
        assertEquals("1.1", sections.get(1).getSectionLevel());
        // Test blank lines are preserved
        this.document
            .setContent("\n\n1 Section 1\n\n\n" + "Content of first section\n\n\n" + "   1.1    Subsection 2  \n\n"
                + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
        sections = this.document.getSections();
        assertEquals(3, sections.size());
        assertEquals(2, sections.get(0).getSectionIndex());
        assertEquals("Subsection 2  ", sections.get(1).getSectionTitle());
        assertEquals(43, sections.get(1).getSectionIndex());
    }

    public void testUpdateDocumentSection10() throws XWikiException
    {
        List<DocumentSection> sections;
        // Fill the document
        this.document.setContent("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
            + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
        String content = this.document.updateDocumentSection(3, "1 Section 3\n" + "Modified content of section 3");
        assertEquals("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
            + "Content of second section\n" + "1 Section 3\n" + "Modified content of section 3", content);
        this.document.setContent(content);
        sections = this.document.getSections();
        assertEquals(3, sections.size());
        assertEquals("Section 1", sections.get(0).getSectionTitle());
        assertEquals("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
            + "Content of second section\n", this.document.getContentOfSection(1));
        assertEquals("1.1", sections.get(1).getSectionLevel());
        assertEquals("1.1 Subsection 2\nContent of second section\n", this.document.getContentOfSection(2));
        assertEquals(3, sections.get(2).getSectionNumber());
        assertEquals(80, sections.get(2).getSectionIndex());
        assertEquals("1 Section 3\nModified content of section 3", this.document.getContentOfSection(3));
    }

    public void testUpdateDocumentSection() throws XWikiException
    {
        this.document.setContent("content not in section\n" + "= header 1=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        // Modify section content
        String content1 = this.document.updateDocumentSection(2, "== header 2==\nmodified header 2 content");

        assertEquals(
            "content not in section\n\n= header 1 =\n\nheader 1 content\n\n== header 2 ==\n\nmodified header 2 content",
            content1);

        String content2 =
            this.document.updateDocumentSection(1,
                "= header 1 =\n\nmodified also header 1 content\n\n== header 2 ==\n\nheader 2 content");

        assertEquals(
            "content not in section\n\n= header 1 =\n\nmodified also header 1 content\n\n== header 2 ==\n\nheader 2 content",
            content2);

        // Remove a section
        String content3 = this.document.updateDocumentSection(2, "");

        assertEquals("content not in section\n\n= header 1 =\n\nheader 1 content", content3);
    }

    public void testDisplay10()
    {
        this.mockXWiki.stubs().method("getCurrentContentSyntaxId").will(returnValue("xwiki/1.0"));

        assertEquals("string", this.document.display("string", "view", getContext()));
        assertEquals(
            "{pre}<input size='30' id='Space.Page_0_string' value='string' name='Space.Page_0_string' type='text'/>{/pre}",
            this.document.display("string", "edit", getContext()));

        this.mockXWikiRenderingEngine.expects(once()).method("renderText").with(eq("area"), ANYTHING, ANYTHING)
            .will(returnValue("area"));

        assertEquals("area", this.document.display("area", "view", getContext()));
    }

    public void testDisplay()
    {
        this.mockXWiki.stubs().method("getCurrentContentSyntaxId").will(returnValue("xwiki/2.0"));

        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("string", this.document.display("string", "view", getContext()));
        assertEquals(
            "{{html clean=\"false\" wiki=\"false\"}}<input size='30' id='Space.Page_0_string' value='string' name='Space.Page_0_string' type='text'/>{{/html}}",
            this.document.display("string", "edit", getContext()));

        assertEquals("{{html clean=\"false\" wiki=\"false\"}}<p>area</p>{{/html}}",
            this.document.display("area", "view", getContext()));
    }

    public void testDisplay1020()
    {
        this.mockXWiki.stubs().method("getCurrentContentSyntaxId").will(returnValue("xwiki/1.0"));

        XWikiDocument doc10 = new XWikiDocument();
        doc10.setSyntax(Syntax.XWIKI_1_0);
        getContext().setDoc(doc10);

        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("string", this.document.display("string", "view", getContext()));
        assertEquals(
            "{pre}<input size='30' id='Space.Page_0_string' value='string' name='Space.Page_0_string' type='text'/>{/pre}",
            this.document.display("string", "edit", getContext()));

        assertEquals("<p>area</p>", this.document.display("area", "view", getContext()));
    }

    public void testDisplay2010()
    {
        this.mockXWiki.stubs().method("getCurrentContentSyntaxId").will(returnValue("xwiki/2.0"));

        XWikiDocument doc10 = new XWikiDocument();
        doc10.setSyntax(Syntax.XWIKI_2_0);
        getContext().setDoc(doc10);

        assertEquals("string", this.document.display("string", "view", getContext()));
        assertEquals(
            "{{html clean=\"false\" wiki=\"false\"}}<input size='30' id='Space.Page_0_string' value='string' name='Space.Page_0_string' type='text'/>{{/html}}",
            this.document.display("string", "edit", getContext()));

        this.mockXWikiRenderingEngine.expects(once()).method("renderText").with(eq("area"), ANYTHING, ANYTHING)
            .will(returnValue("area"));

        assertEquals("area", this.document.display("area", "view", getContext()));
    }

    public void testDisplayTemplate10()
    {
        this.mockXWiki.stubs().method("getCurrentContentSyntaxId").will(returnValue("xwiki/1.0"));

        getContext().put("isInRenderingEngine", false);

        assertEquals("string", this.document.display("string", "view", getContext()));
        assertEquals(
            "<input size='30' id='Space.Page_0_string' value='string' name='Space.Page_0_string' type='text'/>",
            this.document.display("string", "edit", getContext()));

        this.mockXWikiRenderingEngine.expects(once()).method("renderText").with(eq("area"), ANYTHING, ANYTHING)
            .will(returnValue("area"));

        assertEquals("area", this.document.display("area", "view", getContext()));
    }

    public void testDisplayTemplate20()
    {
        this.mockXWiki.stubs().method("getCurrentContentSyntaxId").will(returnValue("xwiki/2.0"));

        getContext().put("isInRenderingEngine", false);

        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("string", this.document.display("string", "view", getContext()));
        assertEquals(
            "<input size='30' id='Space.Page_0_string' value='string' name='Space.Page_0_string' type='text'/>",
            this.document.display("string", "edit", getContext()));

        assertEquals("<p>area</p>", this.document.display("area", "view", getContext()));
    }

    public void testConvertSyntax() throws XWikiException
    {
        this.document.setContent("content not in section\n" + "1 header 1\nheader 1 content\n"
            + "1.1 header 2\nheader 2 content");
        this.baseObject.setLargeStringValue("area", "object content not in section\n"
            + "1 object header 1\nobject header 1 content\n" + "1.1 object header 2\nobject header 2 content");
        this.baseObject.setLargeStringValue("puretextarea", "object content not in section\n"
            + "1 object header 1\nobject header 1 content\n" + "1.1 object header 2\nobject header 2 content");

        this.document.convertSyntax("xwiki/2.0", getContext());

        assertEquals("content not in section\n\n" + "= header 1 =\n\nheader 1 content\n\n"
            + "== header 2 ==\n\nheader 2 content", this.document.getContent());
        assertEquals("object content not in section\n\n" + "= object header 1 =\n\nobject header 1 content\n\n"
            + "== object header 2 ==\n\nobject header 2 content", this.baseObject.getStringValue("area"));
        assertEquals("object content not in section\n" + "1 object header 1\nobject header 1 content\n"
            + "1.1 object header 2\nobject header 2 content", this.baseObject.getStringValue("puretextarea"));
        assertEquals("xwiki/2.0", this.document.getSyntaxId());
    }

    public void testGetRenderedContent10() throws XWikiException
    {
        this.document.setContent("*bold*");
        this.document.setSyntax(Syntax.XWIKI_1_0);

        this.mockXWikiRenderingEngine.expects(once()).method("renderText").with(eq("*bold*"), ANYTHING, ANYTHING)
            .will(returnValue("<b>bold</b>"));

        assertEquals("<b>bold</b>", this.document.getRenderedContent(getContext()));

        this.translatedDocument = new XWikiDocument();
        this.translatedDocument.setContent("~italic~");
        this.translatedDocument.setSyntax(Syntax.XWIKI_2_0);
        this.translatedDocument.setNew(false);

        this.mockXWiki.stubs().method("getLanguagePreference").will(returnValue("fr"));
        this.mockXWikiStoreInterface.stubs().method("loadXWikiDoc").will(returnValue(this.translatedDocument));
        this.mockXWikiRenderingEngine.expects(once()).method("renderText").with(eq("~italic~"), ANYTHING, ANYTHING)
            .will(returnValue("<i>italic</i>"));

        assertEquals("<i>italic</i>", this.document.getRenderedContent(getContext()));
    }

    public void testGetRenderedContent() throws XWikiException
    {
        this.document.setContent("**bold**");
        this.document.setSyntax(Syntax.XWIKI_2_0);

        assertEquals("<p><strong>bold</strong></p>", this.document.getRenderedContent(getContext()));

        this.translatedDocument = new XWikiDocument();
        this.translatedDocument.setContent("//italic//");
        this.translatedDocument.setSyntax(Syntax.XWIKI_1_0);
        this.translatedDocument.setNew(false);

        this.mockXWiki.stubs().method("getLanguagePreference").will(returnValue("fr"));
        this.mockXWikiStoreInterface.stubs().method("loadXWikiDoc").will(returnValue(this.translatedDocument));

        assertEquals("<p><em>italic</em></p>", this.document.getRenderedContent(getContext()));
    }

    public void testGetRenderedContentWithSourceSyntax() throws XWikiException
    {
        this.document.setSyntax(Syntax.XWIKI_1_0);

        assertEquals("<p><strong>bold</strong></p>",
            this.document.getRenderedContent("**bold**", "xwiki/2.0", getContext()));
    }

    public void testRename() throws XWikiException
    {
        // Possible ways to write parents, include documents, or make links:
        // "name" -----means-----> DOCWIKI+":"+DOCSPACE+"."+input
        // "space.name" -means----> DOCWIKI+":"+input
        // "database:space.name" (no change)

        DocumentReference sourceReference = new DocumentReference(this.document.getDocumentReference());
        this.document.setContent("[[pageinsamespace]]");
        this.document.setSyntax(Syntax.XWIKI_2_0);
        DocumentReference targetReference = new DocumentReference("newwikiname", "newspace", "newpage");
        XWikiDocument targetDocument = this.document.duplicate(targetReference);

        DocumentReference reference1 = new DocumentReference(DOCWIKI, DOCSPACE, "Page1");
        XWikiDocument doc1 = new XWikiDocument(reference1);
        doc1.setContent("[[" + DOCWIKI + ":" + DOCSPACE + "." + DOCNAME + "]] [[someName>>" + DOCSPACE + "." + DOCNAME
            + "]] [[" + DOCNAME + "]]");
        doc1.setSyntax(Syntax.XWIKI_2_0);

        DocumentReference reference2 = new DocumentReference("newwikiname", DOCSPACE, "Page2");
        XWikiDocument doc2 = new XWikiDocument(reference2);
        doc2.setContent("[[" + DOCWIKI + ":" + DOCSPACE + "." + DOCNAME + "]]");
        doc2.setSyntax(Syntax.XWIKI_2_0);

        DocumentReference reference3 = new DocumentReference("newwikiname", "newspace", "Page3");
        XWikiDocument doc3 = new XWikiDocument(reference3);
        doc3.setContent("[[" + DOCWIKI + ":" + DOCSPACE + "." + DOCNAME + "]]");
        doc3.setSyntax(Syntax.XWIKI_2_0);

        // Test to make sure it also drags children along.
        DocumentReference reference4 = new DocumentReference(DOCWIKI, DOCSPACE, "Page4");
        XWikiDocument doc4 = new XWikiDocument(reference4);
        doc4.setParent(DOCSPACE + "." + DOCNAME);

        DocumentReference reference5 = new DocumentReference("newwikiname", "newspace", "Page5");
        XWikiDocument doc5 = new XWikiDocument(reference5);
        doc5.setParent(DOCWIKI + ":" + DOCSPACE + "." + DOCNAME);

        this.mockXWiki.stubs().method("copyDocument").will(returnValue(true));
        this.mockXWiki.stubs().method("getDocument").with(eq(targetReference), ANYTHING)
            .will(returnValue(targetDocument));
        this.mockXWiki.stubs().method("getDocument").with(eq(reference1), ANYTHING).will(returnValue(doc1));
        this.mockXWiki.stubs().method("getDocument").with(eq(reference2), ANYTHING).will(returnValue(doc2));
        this.mockXWiki.stubs().method("getDocument").with(eq(reference3), ANYTHING).will(returnValue(doc3));
        this.mockXWiki.stubs().method("getDocument").with(eq(reference4), ANYTHING).will(returnValue(doc4));
        this.mockXWiki.stubs().method("getDocument").with(eq(reference5), ANYTHING).will(returnValue(doc5));
        this.mockXWiki.stubs().method("saveDocument").isVoid();
        this.mockXWiki.stubs().method("deleteDocument").isVoid();

        this.document.rename(new DocumentReference("newwikiname", "newspace", "newpage"),
            Arrays.asList(reference1, reference2, reference3), Arrays.asList(reference4, reference5), getContext());

        // Test links
        assertEquals("[[Wiki:Space.pageinsamespace]]", this.document.getContent());
        assertEquals("[[newwikiname:newspace.newpage]] " + "[[someName>>newwikiname:newspace.newpage]] "
            + "[[newwikiname:newspace.newpage]]", doc1.getContent());
        assertEquals("[[newspace.newpage]]", doc2.getContent());
        assertEquals("[[newpage]]", doc3.getContent());

        // Test parents
        assertEquals("newwikiname:newspace.newpage", doc4.getParent());
        assertEquals(new DocumentReference("newwikiname", "newspace", "newpage"), doc5.getParentReference());
    }

    /**
     * Validate rename does not crash when the document has 1.0 syntax (it does not support everything but it does not
     * crash).
     */
    public void testRename10() throws XWikiException
    {
        DocumentReference sourceReference = new DocumentReference(this.document.getDocumentReference());
        this.document.setContent("[pageinsamespace]");
        this.document.setSyntax(Syntax.XWIKI_1_0);
        DocumentReference targetReference = new DocumentReference("newwikiname", "newspace", "newpage");
        XWikiDocument targetDocument = this.document.duplicate(targetReference);

        this.mockXWiki.stubs().method("copyDocument").will(returnValue(true));
        this.mockXWiki.stubs().method("getDocument").with(eq(targetReference), ANYTHING)
            .will(returnValue(targetDocument));
        this.mockXWiki.stubs().method("saveDocument").isVoid();
        this.mockXWiki.stubs().method("deleteDocument").isVoid();

        this.document.rename(new DocumentReference("newwikiname", "newspace", "newpage"),
            Collections.<DocumentReference> emptyList(), Collections.<DocumentReference> emptyList(), getContext());

        // Test links
        assertEquals("[pageinsamespace]", this.document.getContent());
    }

    /**
     * Normally the xobject vector has the Nth object on the Nth position, but in case an object gets misplaced, trying
     * to remove it should indeed remove that object, and no other.
     */
    public void testRemovingObjectWithWrongObjectVector()
    {
        // Setup: Create a document and two xobjects
        BaseObject o1 = new BaseObject();
        BaseObject o2 = new BaseObject();
        o1.setClassName(CLASSNAME);
        o2.setClassName(CLASSNAME);

        // Test: put the second xobject on the third position
        // addObject creates the object vector and configures the objects
        // o1 is added at position 0
        // o2 is added at position 1
        XWikiDocument doc = new XWikiDocument();
        doc.addObject(CLASSNAME, o1);
        doc.addObject(CLASSNAME, o2);

        // Modify the o2 object's position to ensure it can still be found and removed by the removeObject method.
        assertEquals(1, o2.getNumber());
        o2.setNumber(0);
        // Set a field on o1 so that when comparing it with o2 they are different. This is needed so that the remove
        // will pick the right object to remove (since we've voluntarily set a wrong number of o2 it would pick o1
        // if they were equals).
        o1.addField("somefield", new StringProperty());

        // Call the tested method, removing o2 from position 2 which is set to null
        boolean result = doc.removeObject(o2);

        // Check the correct behavior:
        assertTrue(result);
        Vector<BaseObject> objects = doc.getObjects(CLASSNAME);
        assertTrue(objects.contains(o1));
        assertFalse(objects.contains(o2));
        assertNull(objects.get(1));

        // Second test: swap the two objects, so that the first object is in the position the second should have
        // Start over, re-adding the two objects
        doc = new XWikiDocument();
        doc.addObject(CLASSNAME, o1);
        doc.addObject(CLASSNAME, o2);
    }

    public void testCopyDocument() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setTitle("Some title");
        BaseObject o = new BaseObject();
        o.setClassName(CLASSNAME);
        doc.addObject(CLASSNAME, o);

        XWikiDocument newDoc = doc.copyDocument("newdoc", getContext());
        BaseObject newO = newDoc.getObject(CLASSNAME);

        assertNotSame(o, newDoc.getObject(CLASSNAME));
        assertFalse(newO.getGuid().equals(o.getGuid()));
        // Verify that the title is copied
        assertEquals("Some title", newDoc.getTitle());
    }

    /**
     * @see <a href="http://jira.xwiki.org/jira/browse/XWIKI-6743">XWIKI-6743</a>
     */
    public void testCopyDocumentSetsTitleToNewDocNameIfPreviouslySetToDocName() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("wiki1", "space1", "page1"));
        doc.setTitle(doc.getDocumentReference().getName());

        XWikiDocument newDoc = doc.copyDocument(new DocumentReference("wiki2", "space2", "page2"), getContext());

        // Verify that the title is modified
        assertEquals("page2", newDoc.getTitle());
    }

    public void testResolveClassReference() throws Exception
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
     * Test that the parent remain the same relative value whatever the context.
     */
    public void testGetParent()
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("docwiki", "docspace", "docpage"));

        assertEquals("", doc.getParent());
        doc.setParent(null);
        assertEquals("", doc.getParent());

        doc.setParent("page");
        assertEquals("page", doc.getParent());

        getContext().setDatabase("otherwiki");
        assertEquals("page", doc.getParent());

        doc.setDocumentReference(new DocumentReference("otherwiki", "otherspace", "otherpage"));
        assertEquals("page", doc.getParent());
    }

    public void testGetParentReference()
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

        doc.setFullName("docwiki2:docspace2.docpage2", getContext());
        assertEquals(new DocumentReference("docwiki2", "docspace2", "parentpage"), doc.getParentReference());

        doc.setParent("parentpage2");
        assertEquals(new DocumentReference("docwiki2", "docspace2", "parentpage2"), doc.getParentReference());
    }

    public void testSetAbsoluteParentReference()
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("docwiki", "docspace", "docpage"));

        doc.setParentReference(new DocumentReference("docwiki", "docspace", "docpage2"));
        assertEquals("docspace.docpage2", doc.getParent());
    }

    public void testSetRelativeParentReference()
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("docwiki", "docspace", "docpage"));

        doc.setParentReference(new EntityReference("docpage2", EntityType.DOCUMENT));
        assertEquals(new DocumentReference("docwiki", "docspace", "docpage2"), doc.getParentReference());
        assertEquals("docpage2", doc.getParent());
    }

    /**
     * Verify that cloning objects modify their references to point to the document in which they are cloned into.
     */
    public void testCloneObjectsHaveCorrectReference()
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
    public void testMergeObjectsHaveCorrectReferenceAndDifferentGuids()
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
                assertFalse("Non unique object GUID found!", originalGuids.contains(baseObject.getGuid()));
            }
        }
    }

    /**
     * Tests that objects are not copied again when {@link XWikiDocument#mergeXObjects(XWikiDocument)} is called twice.
     */
    public void testMergeObjectsTwice() throws XWikiException
    {
        // Make sure the target document and the template document are from different wikis.
        XWikiDocument targetDoc = new XWikiDocument(new DocumentReference("someWiki", "someSpace", "somePage"));

        // Merge the objects.
        targetDoc.mergeXObjects(this.document);

        assertEquals(1, targetDoc.getXObjects().size());
        assertEquals(0, targetDoc.getXObjectSize(CLASS_REFERENCE));
        DocumentReference classReference =
            CLASS_REFERENCE.replaceParent(CLASS_REFERENCE.getWikiReference(), targetDoc.getDocumentReference()
                .getWikiReference());
        assertEquals(1, targetDoc.getXObjectSize(classReference));

        // Try to merge the objects again.
        targetDoc.mergeXObjects(this.document);

        // Check that the object from the template document was not copied again.
        assertEquals(1, targetDoc.getXObjectSize(classReference));
    }

    /** Check that a new empty document has empty content (used to have a new line before 2.5). */
    public void testInitialContent()
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("somewiki", "somespace", "somepage"));
        assertEquals("", doc.getContent());
    }

    public void testGetXObjectWithObjectReference()
    {
        Assert.assertSame(this.baseObject, this.document.getXObject(this.baseObject.getReference()));

        Assert.assertSame(this.baseObject, this.document.getXObject(new ObjectReference(
            this.defaultEntityReferenceSerializer.serialize(this.baseObject.getXClassReference()), this.document
                .getDocumentReference())));
    }

    public void testSetXObjectswithPreviousObject()
    {
        BaseObject object = new BaseObject();
        object.setXClassReference(this.baseObject.getXClassReference());
        this.document.addXObject(object);

        this.document.setXObjects(this.baseObject.getXClassReference(), Arrays.asList(object));

        Assert.assertEquals(Arrays.asList(object), this.document.getXObjects(this.baseObject.getXClassReference()));
    }

    public void testSetXObjectWhithNoPreviousObject()
    {
        XWikiDocument document = new XWikiDocument(this.document.getDocumentReference());

        document.setXObject(this.baseObject.getXClassReference(), 0, this.baseObject);

        Assert.assertEquals(Arrays.asList(this.baseObject), document.getXObjects(this.baseObject.getXClassReference()));
    }

    /**
     * Verify that setting a new creator will create a new revision (we verify that that metadata dirty flag is set to
     * true).
     * 
     * @see <a href="http://jira.xwiki.org/jira/browse/XWIKI-7445">XWIKI-7445</a>
     */
    public void testSetCreatorReferenceSetsMetadataDirtyFlag()
    {
        // Make sure we set the flag to false to verify it's changed
        this.document.setMetaDataDirty(false);

        DocumentReference creator = new DocumentReference("Wiki", "XWiki", "Creator");
        this.document.setCreatorReference(creator);

        assertEquals(true, this.document.isMetaDataDirty());
    }

    /**
     * Verify that setting a new creator that is the same as the currenet creator doesn't create a new revision (we
     * verify that the metadata dirty flag is not set).
     * 
     * @see <a href="http://jira.xwiki.org/jira/browse/XWIKI-7445">XWIKI-7445</a>
     */
    public void testSetCreatorReferenceWithSameCreatorDoesntSetMetadataDirtyFlag()
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
     * @see <a href="http://jira.xwiki.org/jira/browse/XWIKI-7445">XWIKI-7445</a>
     */
    public void testSetAuthorReferenceSetsMetadataDirtyFlag()
    {
        // Make sure we set the flag to false to verify it's changed
        this.document.setMetaDataDirty(false);

        DocumentReference author = new DocumentReference("Wiki", "XWiki", "Author");
        this.document.setAuthorReference(author);

        assertEquals(true, this.document.isMetaDataDirty());
    }

    /**
     * Verify that setting a new author that is the same as the currenet creator doesn't create a new revision (we
     * verify that the metadata dirty flag is not set).
     * 
     * @see <a href="http://jira.xwiki.org/jira/browse/XWIKI-7445">XWIKI-7445</a>
     */
    public void testSetAuthorReferenceWithSameAuthorDoesntSetMetadataDirtyFlag()
    {
        // Make sure we set the metadata dirty flag to false to verify it's not changed thereafter
        DocumentReference author = new DocumentReference("Wiki", "XWiki", "Author");
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
     * @see <a href="http://jira.xwiki.org/jira/browse/XWIKI-7445">XWIKI-7445</a>
     */
    public void testSetContentAuthorReferenceSetsMetadataDirtyFlag()
    {
        // Make sure we set the flag to false to verify it's changed
        this.document.setMetaDataDirty(false);

        DocumentReference contentAuthor = new DocumentReference("Wiki", "XWiki", "ContentAuthor");
        this.document.setContentAuthorReference(contentAuthor);

        assertEquals(true, this.document.isMetaDataDirty());
    }

    /**
     * Verify that setting a new content author that is the same as the currenet creator doesn't create a new revision
     * (we verify that the metadata dirty flag is not set).
     * 
     * @see <a href="http://jira.xwiki.org/jira/browse/XWIKI-7445">XWIKI-7445</a>
     */
    public void testSetContentAuthorReferenceWithSameContentAuthorDoesntSetMetadataDirtyFlag()
    {
        // Make sure we set the metadata dirty flag to false to verify it's not changed thereafter
        DocumentReference contentAuthor = new DocumentReference("Wiki", "XWiki", "ContentAuthor");
        this.document.setContentAuthorReference(contentAuthor);
        this.document.setMetaDataDirty(false);

        // Set the content author with the same reference to verify it doesn't change the flag
        this.document.setContentAuthorReference(new DocumentReference("Wiki", "XWiki", "ContentAuthor"));

        assertEquals(false, this.document.isMetaDataDirty());
    }

    /**
     * @see XWIKI-7515: 'getIncludedPages' in class com.xpn.xwiki.api.Document threw java.lang.NullPointerException
     */
    public void testGetIncludedPages()
    {
        this.document.setSyntax(Syntax.XWIKI_2_1);

        this.document.setContent("no include");
        assertTrue(this.document.getIncludedPages(getContext()).isEmpty());

        this.document.setContent("bad {{include/}}");
        assertTrue(this.document.getIncludedPages(getContext()).isEmpty());

        this.document.setContent("good deprecated {{include document=\"Foo.Bar\"/}}");
        assertEquals(Arrays.asList("Foo.Bar"), this.document.getIncludedPages(getContext()));

        this.document.setContent("good {{include reference=\"One.Two\"/}}");
        assertEquals(Arrays.asList("One.Two"), this.document.getIncludedPages(getContext()));
    }

    /**
     * XWIKI-8024: XWikiDocument#setAsContextDoc doesn't set the 'cdoc' in the Velocity context
     */
    public void testSetAsContextDoc() throws Exception
    {
        VelocityContext velocityContext = new VelocityContext();
        this.mockVelocityManager.stubs().method("getVelocityContext").will(returnValue(velocityContext));

        assertNotSame(this.document, getContext().getDoc());
        this.document.setAsContextDoc(getContext());
        assertSame(this.document, getContext().getDoc());

        Assert.assertEquals(this.document.getDocumentReference(),
            ((Document) velocityContext.get("doc")).getDocumentReference());
        Assert.assertEquals(this.document.getDocumentReference(),
            ((Document) velocityContext.get("tdoc")).getDocumentReference());
        Assert.assertEquals(this.document.getDocumentReference(),
            ((Document) velocityContext.get("cdoc")).getDocumentReference());
    }

    /**
     * XWIKI-8025: XWikiDocument#backup/restoreContext doesn't update the reference to the Velocity context stored on
     * the XWiki context
     */
    public void testBackupRestoreContextUpdatesVContext() throws Exception
    {
        final Execution execution = getComponentManager().getInstance(Execution.class);
        this.mockVelocityManager.stubs().method("getVelocityContext")
            .will(new CustomStub("Implements VelocityManager.getVelocityContext")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    return execution.getContext().getProperty("velocityContext");
                }
            });

        VelocityContext oldVelocityContext = new VelocityContext();
        execution.getContext().setProperty("velocityContext", oldVelocityContext);

        Map<String, Object> backup = new HashMap<String, Object>();
        XWikiDocument.backupContext(backup, getContext());

        VelocityContext newVelocityContext = (VelocityContext) execution.getContext().getProperty("velocityContext");
        assertNotNull(newVelocityContext);
        assertNotSame(oldVelocityContext, newVelocityContext);
        assertSame(newVelocityContext, getContext().get("vcontext"));

        XWikiDocument.restoreContext(backup, getContext());

        assertSame(oldVelocityContext, execution.getContext().getProperty("velocityContext"));
        assertSame(oldVelocityContext, getContext().get("vcontext"));
    }

    public void testEqualsDatas()
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        XWikiDocument otherDocyment = document.clone();

        Assert.assertTrue(document.equals(otherDocyment));
        Assert.assertTrue(document.equalsData(otherDocyment));

        otherDocyment.setAuthorReference(new DocumentReference("wiki", "space", "otherauthor"));
        otherDocyment.setContentAuthorReference(otherDocyment.getAuthorReference());
        otherDocyment.setCreatorReference(otherDocyment.getAuthorReference());
        otherDocyment.setVersion("42.0");
        otherDocyment.setComment("other comment");
        otherDocyment.setMinorEdit(true);

        document.setMinorEdit(false);

        Assert.assertFalse(document.equals(otherDocyment));
        Assert.assertTrue(document.equalsData(otherDocyment));
    }
}
