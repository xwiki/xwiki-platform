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
import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfiguration;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiDocument}.
 *
 * @version $Id$
 */
@OldcoreTest
@AllComponents
class XWikiDocumentTest
{
    private static final String DOCWIKI = "WikiDescriptor";

    private static final String DOCSPACE = "Space";

    private static final String DOCNAME = "Page";

    private static final String DOCFULLNAME = DOCSPACE + "." + DOCNAME;

    private static final String CLASSNAME = DOCFULLNAME;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Inject
    private RenderingConfiguration renderingConfiguration;

    @Mock
    private XWikiMessageTool xWikiMessageTool;

    @Mock
    private XWiki xWiki;

    private XWikiStoreInterface xWikiStoreInterface;

    private XWikiDocument document;

    private XWikiDocument translatedDocument;

    private BaseClass baseClass;

    private BaseObject baseObject;

    @BeforeEach
    public void setUp() throws Exception
    {
        XWikiVersioningStoreInterface mockXWikiVersioningStore =
            this.componentManager.registerMockComponent(XWikiVersioningStoreInterface.class);
        this.xWikiStoreInterface = this.componentManager.registerMockComponent(XWikiStoreInterface.class);
        VelocityManager velocityManager = this.componentManager.registerMockComponent(VelocityManager.class);
        VelocityEngine mockVelocityEngine = mock();
        when(velocityManager.getVelocityEngine()).thenReturn(mockVelocityEngine);

        Answer<Boolean> invocationVelocity = invocationOnMock -> {
            // Output the given text without changes.
            StringWriter writer = invocationOnMock.getArgument(0);
            String text = invocationOnMock.getArgument(2);
            writer.append(text);
            return true;
        };
        when(mockVelocityEngine.evaluate(any(), any(), any(), any(String.class))).then(invocationVelocity);

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

        this.oldcore.getXWikiContext().put("isInRenderingEngine", true);

        this.document.setStore(this.xWikiStoreInterface);

        when(this.xWikiMessageTool.get(any())).thenReturn("message");
        when(this.oldcore.getMockRightService().hasProgrammingRights(any())).thenReturn(true);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.PROGRAM)).thenReturn(true);

        when(this.xWiki.getVersioningStore()).thenReturn(mockXWikiVersioningStore);
        when(this.xWiki.getStore()).thenReturn(xWikiStoreInterface);
        when(this.xWiki.getDocument(any(DocumentReference.class), any())).thenReturn(this.document);
        when(this.xWiki.getDocumentReference(any(XWikiRequest.class), any())).thenReturn(documentReference);
        when(this.xWiki.getDocumentReference(any(EntityReference.class), any()))
            .then(i -> new DocumentReference(i.getArgument(0)));
        when(this.xWiki.getLanguagePreference(any())).thenReturn("en");
        when(this.xWiki.getSectionEditingDepth()).thenReturn(2L);
        when(this.xWiki.getRightService()).thenReturn(this.oldcore.getMockRightService());
        when(this.xWiki.exists(any(DocumentReference.class), any())).thenReturn(false);
        when(this.xWiki.evaluateTemplate(any(), any())).thenReturn("");

        this.oldcore.getXWikiContext().setWiki(this.xWiki);
        this.oldcore.getXWikiContext().put("msg", this.xWikiMessageTool);

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

        when(this.xWiki.getClass(any(), any())).thenReturn(this.baseClass);
        when(this.xWiki.getXClass(any(), any())).thenReturn(this.baseClass);

        this.baseObject = this.document.newObject(CLASSNAME, this.oldcore.getXWikiContext());
        this.baseObject.setStringValue("string", "string");
        this.baseObject.setLargeStringValue("area", "area");
        this.baseObject.setStringValue("passwd", "passwd");
        this.baseObject.setIntValue("boolean", 1);
        this.baseObject.setIntValue("int", 42);
        this.baseObject.setStringListValue("stringlist", Arrays.asList("VALUE1", "VALUE2"));

        // Set the default link label generator format to %np for some tests below.
        // We need to do this since we don't depend on xwiki-platform-rendering-configuration-default (which contains
        // an overridden RenderingConfiguration impl that sets the format to %np by default).
        ((DefaultRenderingConfiguration) this.renderingConfiguration).setLinkLabelFormat("%np");

        // Setup display configuration.
        this.oldcore.getConfigurationSource().setProperty("display.documentDisplayerHint", "default");
    }

    @Test
    void testGetDisplayTitleWhenNoTitleAndNoContent()
    {
        this.document.setContent("Some content");

        assertEquals("Page", this.document.getDisplayTitle(this.oldcore.getXWikiContext()));
    }

    @Test
    void testGetDisplayWhenTitleExists()
    {
        this.document.setContent("Some content");
        this.document.setTitle("Title");

        assertEquals("Title", this.document.getDisplayTitle(this.oldcore.getXWikiContext()));
    }

    @Test
    void testGetDisplayWhenNoTitleButSectionExists()
    {
        this.oldcore.registerMockXWikiCfg().setProperty("xwiki.title.compatibility", "1");

        this.document.setContent("Some content\n= Title");

        assertEquals("Title", this.document.getDisplayTitle(this.oldcore.getXWikiContext()));
    }

    @Test
    void testGetxWikiClass()
    {
        assertNotNull(this.document.getxWikiClass());
        assertNotNull(((PropertyClass) this.baseObject.getxWikiClass(this.oldcore.getXWikiContext()).get("string"))
            .getxWikiClass(this.oldcore.getXWikiContext()));
    }

    @Test
    void testExtractTitle()
    {
        this.document.setSyntax(Syntax.XWIKI_2_0);

        this.document.setContent("content not in section\n" + "= header 1=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("header 1", this.document.extractTitle());

        this.document.setContent("content not in section\n" + "= **header 1**=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("<strong>header 1</strong>", this.document.extractTitle());

        this.document.setContent("content not in section\n" + "= [[Space.Page]]=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("<span class=\"wikiexternallink\"><a href=\"Space.Page\"><span class=\"wikigeneratedlinkcontent\">"
            + "Space.Page" + "</span></a></span>", this.document.extractTitle());

        this.document.setContent("content not in section\n" + "= #set($var ~= \"value\")=\nheader 1 content\n"
            + "== header 2==\nheader 2 content");

        assertEquals("#set($var = \"value\")", this.document.extractTitle());

        this.document.setContent("content not in section\n"
            + "= {{groovy}}print \"value\"{{/groovy}}=\nheader 1 content\n" + "== header 2==\nheader 2 content");

        assertEquals("value", this.document.extractTitle());

        this.document.setContent("content not in section\n=== header 3===");

        assertEquals("", this.document.extractTitle());
    }

    @Test
    void testSetAbsoluteParentReference()
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("docwiki", "docspace", "docpage"));

        doc.setParentReference(new DocumentReference("docwiki", "docspace", "docpage2"));
        assertEquals("docspace.docpage2", doc.getParent());
    }

    /**
     * Validate rename does not crash when the document has 1.0 syntax (it does not support everything but it does not
     * crash).
     */
    @Test
    void testRename10() throws XWikiException
    {
        this.document.setContent("[pageinsamespace]");
        this.document.setSyntax(Syntax.XWIKI_1_0);
        DocumentReference targetReference = new DocumentReference("newwikiname", "newspace", "newpage");
        XWikiDocument targetDocument = this.document.duplicate(targetReference);

        when(this.xWiki.copyDocument(any(), any(), any())).thenReturn(true);
        //FIXME: this.mockXWiki.stubs().method("copyDocument").will(returnValue(true));
        when(this.xWiki.getDocument(targetReference, this.oldcore.getXWikiContext())).thenReturn(targetDocument);

        this.document.rename(targetReference,
                Collections.emptyList(), Collections.emptyList(),
                this.oldcore.getXWikiContext());

        verify(this.xWiki).renameByCopyAndDelete(this.document, targetReference,
            Collections.emptyList(), Collections.emptyList(), this.oldcore.getXWikiContext());

        // Test links
        assertEquals("[pageinsamespace]", this.document.getContent());
    }
}
