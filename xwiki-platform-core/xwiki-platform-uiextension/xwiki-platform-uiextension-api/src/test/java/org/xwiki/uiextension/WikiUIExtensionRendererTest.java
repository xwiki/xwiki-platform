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
package org.xwiki.uiextension;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.test.mockito.MockitoComponentManagerRule;
import org.xwiki.uiextension.internal.WikiUIExtensionRenderer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import org.junit.Assert;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WikiUIExtensionRendererTest
{
    private Execution execution;

    private Transformation macroTransformation;

    private ContentParser contentParser;

    private XDOM xdom;

    private static final DocumentReference DOC_REF = new DocumentReference("xwiki", "XWiki", "MyUIExtension");

    @Rule
    public MockitoComponentManagerRule cm = new MockitoComponentManagerRule();

    @Before
    public void setUp() throws Exception
    {
        execution = cm.registerMockComponent(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        macroTransformation = cm.registerMockComponent(Transformation.class, "macro");
        contentParser = cm.registerMockComponent(ContentParser.class);
        when(execution.getContext()).thenReturn(executionContext);
        xdom = mock(XDOM.class);
    }

    @Test
    public void executeWithEmptyContent() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        XWikiDocument xdoc = mock(XWikiDocument.class);
        XWiki xwiki = mock(XWiki.class);

        when(contentParser.parse(eq(""), eq(Syntax.XWIKI_2_1))).thenReturn(xdom);
        when(xdom.clone()).thenReturn(xdom);
        when(execution.getContext().getProperty("xwikicontext")).thenReturn(xcontext);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xwiki.getDocument(DOC_REF, xcontext)).thenReturn(xdoc);
        when(xcontext.getWiki().getDocument(DOC_REF, xcontext)).thenReturn(xdoc);
        when(xdoc.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        WikiUIExtensionRenderer renderer = new WikiUIExtensionRenderer("roleHint", "", DOC_REF, cm);

        Block block = renderer.execute();
        Assert.assertEquals(0, block.getChildren().size());
    }
}
