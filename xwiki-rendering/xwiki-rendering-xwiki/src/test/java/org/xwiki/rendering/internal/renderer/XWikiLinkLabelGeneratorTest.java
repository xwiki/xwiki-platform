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
package org.xwiki.rendering.internal.renderer;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.listener.Link;

/**
 * Unit tests for {@link XWikiLinkLabelGenerator}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class XWikiLinkLabelGeneratorTest extends MockObjectTestCase
{
    private XWikiLinkLabelGenerator generator;

    private Mock mockModelBridge;

    private Mock mockAccessBridge;

    /**
     * {@inheritDoc}
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp()
    {
        this.generator = new XWikiLinkLabelGenerator();

        Mock mockConfiguration = mock(RenderingConfiguration.class);
        mockConfiguration.stubs().method("getLinkLabelFormat").will(returnValue("[%w:%s.%p] %P (%t) [%w:%s.%p] %P (%t)"));
        this.generator.setRenderingConfiguration((RenderingConfiguration) mockConfiguration.proxy());

        this.mockModelBridge = mock(DocumentModelBridge.class);

        this.mockAccessBridge = mock(DocumentAccessBridge.class);
        this.mockAccessBridge.stubs().method("getDocumentName").will(
            returnValue(new DocumentName("xwiki", "Main", "HelloWorld")));
        this.generator.setDocumentAccessBridge((DocumentAccessBridge) mockAccessBridge.proxy());
    }

    public void testGenerate()
    {
        Link link = new Link();
        link.setReference("HelloWorld");

        this.mockModelBridge.stubs().method("getTitle").will(returnValue("My title"));
        this.mockAccessBridge.stubs().method("getDocument").will(returnValue(this.mockModelBridge.proxy()));
        assertEquals("[xwiki:Main.HelloWorld] Hello World (My title) [xwiki:Main.HelloWorld] Hello World (My title)", this.generator.generate(link));
    }

    public void testGenerateWhenDocumentFailsToLoad()
    {
        this.mockAccessBridge.stubs().method("getDocument").will(throwException(new Exception("error")));

        assertEquals("HelloWorld", this.generator.generate(new Link()));
    }

    public void testGenerateWhenDocumentTitleIsNull()
    {
        Link link = new Link();
        link.setReference("HelloWorld");

        this.mockModelBridge.stubs().method("getTitle").will(returnValue(null));
        this.mockAccessBridge.stubs().method("getDocument").will(returnValue(this.mockModelBridge.proxy()));

        assertEquals("HelloWorld", this.generator.generate(new Link()));
    }

    public void testGenerateWhithRegexpSyntax()
    {
        this.mockModelBridge.stubs().method("getTitle").will(returnValue("$0"));
        this.mockAccessBridge.stubs().method("getDocument").will(returnValue(this.mockModelBridge.proxy()));

        this.mockAccessBridge.stubs().method("getDocumentName").will(returnValue(new DocumentName("$0", "\\", "$0")));

        assertEquals("[$0:\\.$0] $0 ($0) [$0:\\.$0] $0 ($0)", this.generator.generate(new Link()));
    }
}
