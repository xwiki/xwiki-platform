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

import java.util.Arrays;

import org.jmock.Mock;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Unit tests for {@link XWikiDocument}.
 * 
 * @version $Id$
 */
public class XWikiDocumentRenderingTest extends AbstractBridgedXWikiComponentTestCase
{
    private static final String DOCWIKI = "xwiki";

    private static final String DOCSPACE = "Space";

    private static final String DOCNAME = "Page";

    private static final String DOCFULLNAME = DOCSPACE + "." + DOCNAME;

    private static final String CLASSNAME = DOCFULLNAME;

    private XWikiDocument document;

    private XWikiDocument translatedDocument;

    private Mock mockXWiki;

    private Mock mockXWikiRenderingEngine;

    private Mock mockXWikiVersioningStore;

    private Mock mockXWikiStoreInterface;

    private Mock mockXWikiRightService;

    private BaseClass baseClass;

    private BaseObject baseObject;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.document = new XWikiDocument(DOCWIKI, DOCSPACE, DOCNAME);
        this.document.setSyntaxId("xwiki/1.0");
        this.document.setLanguage("en");
        this.document.setDefaultLanguage("en");
        this.document.setNew(false);

        getContext().setDoc(this.document);

        this.translatedDocument = new XWikiDocument();
        this.translatedDocument.setSyntaxId("xwiki/1.0");
        this.translatedDocument.setLanguage("fr");
        this.translatedDocument.setNew(false);

        getContext().put("isInRenderingEngine", true);

        this.mockXWiki = mock(XWiki.class);
        this.mockXWiki.stubs().method("Param").will(returnValue(null));

        this.mockXWikiRenderingEngine = mock(XWikiRenderingEngine.class);

        this.mockXWikiVersioningStore = mock(XWikiVersioningStoreInterface.class);
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(returnValue(null));

        this.mockXWikiRightService = mock(XWikiRightService.class);
        this.mockXWikiRightService.stubs().method("hasProgrammingRights").will(returnValue(true));

        this.mockXWikiStoreInterface = mock(XWikiStoreInterface.class);
        this.document.setStore((XWikiStoreInterface) this.mockXWikiStoreInterface.proxy());

        this.mockXWiki.stubs().method("getRenderingEngine").will(returnValue(this.mockXWikiRenderingEngine.proxy()));
        this.mockXWiki.stubs().method("getVersioningStore").will(returnValue(this.mockXWikiVersioningStore.proxy()));
        this.mockXWiki.stubs().method("getStore").will(returnValue(this.mockXWikiStoreInterface.proxy()));
        this.mockXWiki.stubs().method("getRightService").will(returnValue(this.mockXWikiRightService.proxy()));
        this.mockXWiki.stubs().method("getDocument").will(returnValue(this.document));
        this.mockXWiki.stubs().method("getLanguagePreference").will(returnValue("en"));

        getContext().setWiki((XWiki) this.mockXWiki.proxy());

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

        this.baseObject = this.document.newObject(CLASSNAME, getContext());
        this.baseObject.setStringValue("string", "string");
        this.baseObject.setLargeStringValue("area", "area");
        this.baseObject.setStringValue("passwd", "passwd");
        this.baseObject.setIntValue("boolean", 1);
        this.baseObject.setIntValue("int", 42);
        this.baseObject.setStringListValue("stringlist", Arrays.asList("VALUE1", "VALUE2"));
    }

    public void testScriptContext() throws XWikiException
    {
        this.document.setContent("{{groovy}}print(doc);{{/groovy}}");
        this.document.setSyntaxId("xwiki/2.0");

        assertEquals("<!--startmacro:groovy|-||-|print(doc);--><p>Space.Page</p><!--stopmacro-->", this.document
            .getRenderedContent(getContext()));
    }
}
