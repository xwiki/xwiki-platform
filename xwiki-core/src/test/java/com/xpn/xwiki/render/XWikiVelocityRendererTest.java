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
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.AbstractXWikiTestCase;

import java.util.Collections;

/**
 * Unit tests for {@link com.xpn.xwiki.render.XWikiVelocityRenderer}.
 *
 * @version $Id: $
 */
public class XWikiVelocityRendererTest extends AbstractXWikiTestCase
{
    private XWikiContext context;
    private XWikiVelocityRenderer renderer;
    private Mock mockXWiki;
    private Mock mockDocument;
    private Mock mockContentDocument;
    private XWikiDocument document;
    private XWikiDocument contentDocument;

    protected void setUp() throws Exception
    {
        this.renderer = new XWikiVelocityRenderer();
        this.context = new XWikiContext();

        // We need to initialize the Component Manager so that the Velocity component can be
        // looked up
        this.context.put(ComponentManager.class.getName(), getComponentManager());

        this.mockXWiki = mock(XWiki.class, new Class[] {XWikiConfig.class, XWikiContext.class},
            new Object[] {new XWikiConfig(), context});
        this.mockXWiki.stubs().method("getSkin").will(returnValue("default"));
        this.mockXWiki.stubs().method("getSkinFile").will(returnValue(null));
        this.mockXWiki.stubs().method("getResourceContent").will(returnValue(null));
        this.context.setWiki((XWiki) this.mockXWiki.proxy());

        this.mockContentDocument = mock(XWikiDocument.class);
        this.contentDocument = (XWikiDocument) this.mockContentDocument.proxy();

        this.mockDocument = mock(XWikiDocument.class);
        this.document = (XWikiDocument) this.mockDocument.proxy();

        Mock mockApiDocument = mock(Document.class,
            new Class[] {XWikiDocument.class, XWikiContext.class},
            new Object[] {this.document, this.context});
        this.mockDocument.stubs().method("newDocument").will(returnValue(mockApiDocument.proxy()));
    }

    public void testRenderWithSimpleText()
    {
        this.mockXWiki.stubs().method("getIncludedMacros").will(
            returnValue(Collections.EMPTY_LIST));
        this.mockContentDocument.stubs().method("getSpace").will(returnValue("Space1"));
        this.mockDocument.stubs().method("getFullName").will(returnValue("Space2.Document"));

        String result = renderer.render("Simple content", contentDocument, document, context);

        assertEquals("Simple content", result);
    }

    public void testRenderWithVelocityContent()
    {
        this.mockXWiki.stubs().method("getIncludedMacros").will(
            returnValue(Collections.EMPTY_LIST));
        this.mockContentDocument.stubs().method("getSpace").will(returnValue("Space1"));
        this.mockDocument.stubs().method("getFullName").will(returnValue("Space2.Document"));

        String result = renderer.render("#set ($test = \"hello\")\n$test world\n## comment",
            contentDocument, document, context);

        assertEquals("hello world\n", result);
    }
}
