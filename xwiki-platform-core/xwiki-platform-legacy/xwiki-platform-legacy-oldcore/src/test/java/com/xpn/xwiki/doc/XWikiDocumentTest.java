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
import java.util.ResourceBundle;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.display.internal.DisplayConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfiguration;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.Right;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
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
    private static final String DOCWIKI = "WikiDescriptor";

    private static final String DOCSPACE = "Space";

    private static final String DOCNAME = "Page";

    private static final String DOCFULLNAME = DOCSPACE + "." + DOCNAME;

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME);

    private static final String CLASSNAME = DOCFULLNAME;

    private static final DocumentReference CLASS_REFERENCE = DOCUMENT_REFERENCE;

    private XWikiDocument document;

    private XWikiDocument translatedDocument;

    private Mock mockXWiki;

    private Mock mockXWikiVersioningStore;

    private Mock mockXWikiStoreInterface;

    private Mock mockXWikiMessageTool;

    private Mock mockXWikiRightService;

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

        DocumentReference documentReference = new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME);
        this.document = new XWikiDocument(documentReference);
        this.document.setSyntax(Syntax.XWIKI_2_0);
        this.document.setLanguage("en");
        this.document.setDefaultLanguage("en");
        this.document.setNew(false);

        this.translatedDocument = new XWikiDocument();
        this.translatedDocument.setSyntax(Syntax.XWIKI_2_0);
        this.translatedDocument.setLanguage("fr");
        this.translatedDocument.setNew(false);

        getContext().put("isInRenderingEngine", true);

        this.mockXWiki = mock(XWiki.class);

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

        this.mockXWiki.stubs().method("getVersioningStore").will(returnValue(this.mockXWikiVersioningStore.proxy()));
        this.mockXWiki.stubs().method("getStore").will(returnValue(this.mockXWikiStoreInterface.proxy()));
        this.mockXWiki.stubs().method("getDocument").will(returnValue(this.document));
        this.mockXWiki.stubs().method("getDocumentReference").will(returnValue(documentReference));
        this.mockXWiki.stubs().method("getLanguagePreference").will(returnValue("en"));
        this.mockXWiki.stubs().method("getSectionEditingDepth").will(returnValue(2L));
        this.mockXWiki.stubs().method("getRightService").will(returnValue(this.mockXWikiRightService.proxy()));

        getContext().setWiki((XWiki) this.mockXWiki.proxy());
        getContext().put("msg", this.mockXWikiMessageTool.proxy());

        this.baseClass = this.document.getxWikiClass();
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

        // Set the default link label generator format to %np for some tests below.
        // We need to do this since we don't depend on xwiki-platform-rendering-configuration-default (which contains
        // an overridden RenderingConfiguration impl that sets the format to %np by default).
        DefaultRenderingConfiguration renderingConfiguration =
            getComponentManager().getInstance(RenderingConfiguration.class);
        renderingConfiguration.setLinkLabelFormat("%np");
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        getContextualAuthorizationManager().stubs()
            .method("hasAccess").with(eq(Right.PROGRAM)).will(returnValue(true));

        // Setup display configuration.
        this.mockDisplayConfiguration = registerMockComponent(DisplayConfiguration.class);
        this.mockDisplayConfiguration.stubs().method("getDocumentDisplayerHint").will(returnValue("default"));
        this.mockDisplayConfiguration.stubs().method("getTitleHeadingDepth").will(returnValue(2));

        // Setup the mock Velocity engine.
        Mock mockVelocityManager = registerMockComponent(VelocityManager.class);
        this.mockVelocityEngine = mock(VelocityEngine.class);
        mockVelocityManager.stubs().method("getVelocityContext").will(returnValue(null));
        mockVelocityManager.stubs().method("getVelocityEngine").will(returnValue(this.mockVelocityEngine.proxy()));
        velocityEngineEvaluateStub = new CustomStub("Implements VelocityEngine.evaluate")
        {
            @Override
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

    public void testGetDisplayTitleWhenNoTitleAndNoContent()
    {
        this.document.setContent("Some content");

        assertEquals("Page", this.document.getDisplayTitle(getContext()));
    }

    public void testGetDisplayWhenTitleExists()
    {
        this.document.setContent("Some content");
        this.document.setTitle("Title");
        this.mockVelocityEngine.expects(once()).method("evaluate").with(null, ANYTHING, ANYTHING, eq("Title"))
            .will(velocityEngineEvaluateStub);

        assertEquals("Title", this.document.getDisplayTitle(getContext()));
    }

    public void testGetDisplayWhenNoTitleButSectionExists()
    {
        getConfigurationSource().setProperty("xwiki.title.compatibility", "1");

        this.document.setContent("Some content\n= Title");

        assertEquals("Title", this.document.getDisplayTitle(getContext()));
    }

    /**
     * Verify that if an error happens when evaluation the title, we fallback to the computed title.
     */
    public void testGetDisplayTitleWhenVelocityError()
    {
        this.document.setContent("Some content");
        this.document.setTitle("some content that generate a velocity error");
        this.mockVelocityEngine.expects(once()).method("evaluate")
            .will(throwException(new XWikiVelocityException("message")));

        assertEquals("Page", this.document.getDisplayTitle(getContext()));
    }

    public void testGetxWikiClass()
    {
        this.document.getxWikiClass();
        ((PropertyClass)this.baseObject.getxWikiClass(getContext()).get("string")).getxWikiClass(getContext());
    }

    public void testExtractTitle()
    {
        this.mockXWiki.stubs().method("exists").will(returnValue(false));
        this.document.setSyntax(Syntax.XWIKI_2_0);

        this.document.setContent("content not in section\n" + "= header 1=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("header 1", this.document.extractTitle());

        this.document.setContent("content not in section\n" + "= **header 1**=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("<strong>header 1</strong>", this.document.extractTitle());

        this.document.setContent("content not in section\n" + "= [[Space.Page]]=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        this.mockXWiki.stubs().method("getURL").will(returnValue("/reference"));

        assertEquals("<span class=\"wikicreatelink\"><a href=\"/reference\"><span class=\"wikigeneratedlinkcontent\">"
            + "Page" + "</span></a></span>", this.document.extractTitle());

        this.document.setContent("content not in section\n" + "= #set($var ~= \"value\")=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("#set($var = \"value\")", this.document.extractTitle());

        this.document.setContent("content not in section\n"
            + "= {{groovy}}print \"value\"{{/groovy}}=\nheader 1 content\n" + "== header 2==\nheader 2 content");

        assertEquals("value", this.document.extractTitle());

        this.document.setContent("content not in section\n=== header 3===");

        assertEquals("", this.document.extractTitle());
    }

    public void testSetAbsoluteParentReference()
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("docwiki", "docspace", "docpage"));

        doc.setParentReference(new DocumentReference("docwiki", "docspace", "docpage2"));
        assertEquals("docspace.docpage2", doc.getParent());
    }
}
