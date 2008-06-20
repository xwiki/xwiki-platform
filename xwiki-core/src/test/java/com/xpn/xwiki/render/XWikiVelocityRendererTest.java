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
package com.xpn.xwiki.render;

import org.jmock.Mock;
import org.xwiki.component.manager.ComponentManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractXWikiComponentTestCase;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.XWiki;

import java.util.Collections;

/**
 * Unit tests for {@link com.xpn.xwiki.render.XWikiVelocityRenderer}.
 * 
 * @version $Id$
 */
public class XWikiVelocityRendererTest extends AbstractXWikiComponentTestCase
{
    private XWikiVelocityRenderer renderer;

    private Mock mockXWiki;

    private Mock mockDocument;

    private Mock mockContentDocument;

    private XWikiDocument document;

    private XWikiDocument contentDocument;

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.test.AbstractXWikiComponentTestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        // Statically store the component manager in {@link Utils} to be able to access it without
        // the context.
        // @FIXME : move this initialization in AbstractXWikiComponentTestCase.setUp() when
        // shared-tests will depends on core 1.5 branch
        Utils.setComponentManager((ComponentManager) getContext().get(
            ComponentManager.class.getName()));

        this.renderer = new XWikiVelocityRenderer();

        this.mockXWiki = mock(XWiki.class);
        this.mockXWiki.stubs().method("getSkin").will(returnValue("default"));
        this.mockXWiki.stubs().method("getSkinFile").will(returnValue(null));
        this.mockXWiki.stubs().method("getResourceContent").will(returnValue(null));
        this.mockXWiki.stubs().method("prepareResources");
        getContext().setWiki((XWiki) this.mockXWiki.proxy());

        this.mockContentDocument = mock(XWikiDocument.class);
        this.contentDocument = (XWikiDocument) this.mockContentDocument.proxy();

        this.mockDocument = mock(XWikiDocument.class);
        this.document = (XWikiDocument) this.mockDocument.proxy();

        Mock mockApiDocument =
            mock(Document.class, new Class[] {XWikiDocument.class, XWikiContext.class},
                new Object[] {this.document, getContext()});
        this.mockDocument.stubs().method("newDocument")
            .will(returnValue(mockApiDocument.proxy()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.test.AbstractXWikiComponentTestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();

        // Makes sure tests are independents as Utils's ComponentManager is a static
        // @FIXME : move this initialization in AbstractXWikiComponentTestCase.setUp() when
        // shared-tests will depends on core 1.5 branch
        Utils.setComponentManager(null);
    }

    public void testRenderWithSimpleText()
    {
        this.mockXWiki.stubs().method("getIncludedMacros").will(
            returnValue(Collections.EMPTY_LIST));
        this.mockContentDocument.stubs().method("getSpace").will(returnValue("Space1"));
        this.mockDocument.stubs().method("getFullName").will(returnValue("Space2.Document"));

        String result =
            renderer.render("Simple content", contentDocument, document, getContext());

        assertEquals("Simple content", result);
    }

    public void testRenderWithVelocityContent()
    {
        this.mockXWiki.stubs().method("getIncludedMacros").will(
            returnValue(Collections.EMPTY_LIST));
        this.mockContentDocument.stubs().method("getSpace").will(returnValue("Space1"));
        this.mockDocument.stubs().method("getFullName").will(returnValue("Space2.Document"));

        String result =
            renderer.render("#set ($test = \"hello\")\n$test world\n## comment", contentDocument,
                document, getContext());

        assertEquals("hello world\n", result);
    }
}
