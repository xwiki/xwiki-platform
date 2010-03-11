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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.listener.Link;

/**
 * Unit tests for {@link XWikiLinkLabelGenerator}.
 *
 * @version $Id$
 * @since 2.0M1
 */
public class XWikiLinkLabelGeneratorTest
{
    private Mockery mockery = new Mockery();

    private XWikiLinkLabelGenerator generator;

    private DocumentModelBridge mockDocumentModelBridge;

    private DocumentAccessBridge mockDocumentAccessBridge;

    private DocumentReferenceResolver mockDocumentReferenceResolver;

    @Before
    public void setUp()
    {
        this.generator = new XWikiLinkLabelGenerator();

        this.mockDocumentAccessBridge = mockery.mock(DocumentAccessBridge.class);
        ReflectionUtils.setFieldValue(this.generator, "documentAccessBridge", this.mockDocumentAccessBridge);

        final RenderingConfiguration mockRenderingConfiguration = mockery.mock(RenderingConfiguration.class);
        ReflectionUtils.setFieldValue(this.generator, "renderingConfiguration", mockRenderingConfiguration);

        this.mockDocumentReferenceResolver = mockery.mock(DocumentReferenceResolver.class);
        ReflectionUtils.setFieldValue(this.generator, "currentDocumentReferenceResolver",
            this.mockDocumentReferenceResolver);

        this.mockDocumentModelBridge = mockery.mock(DocumentModelBridge.class);

        mockery.checking(new Expectations() {{
            allowing(mockRenderingConfiguration).getLinkLabelFormat();
                will(returnValue("[%w:%s.%p] %P (%t) [%w:%s.%p] %P (%t)"));
        }});
    }

    @org.junit.Test
    public void testGenerate() throws Exception
    {
        Link link = new Link();
        link.setReference("HelloWorld");

        mockery.checking(new Expectations() {{
            allowing(mockDocumentReferenceResolver).resolve(with(any(String.class)), with(any(Object[].class)));
                will(returnValue(new DocumentReference("xwiki", "Main", "HelloWorld")));
            allowing(mockDocumentModelBridge).getTitle(); will(returnValue("My title"));
            allowing(mockDocumentAccessBridge).getDocument(with(any(DocumentReference.class)));
                will(returnValue(mockDocumentModelBridge));
        }});

        Assert.assertEquals("[xwiki:Main.HelloWorld] Hello World (My title) [xwiki:Main.HelloWorld] Hello World "
            + "(My title)", this.generator.generate(link));
    }

    @org.junit.Test
    public void testGenerateWhenDocumentFailsToLoad() throws Exception
    {
        mockery.checking(new Expectations() {{
            allowing(mockDocumentReferenceResolver).resolve(with(any(String.class)), with(any(Object[].class)));
                will(returnValue(new DocumentReference("xwiki", "Main", "HelloWorld")));
            allowing(mockDocumentAccessBridge).getDocument(with(any(DocumentReference.class)));
                will(throwException(new Exception("error")));
        }});

        Assert.assertEquals("HelloWorld", this.generator.generate(new Link()));
    }

    @org.junit.Test
    public void testGenerateWhenDocumentTitleIsNull() throws Exception
    {
        Link link = new Link();
        link.setReference("HelloWorld");

        mockery.checking(new Expectations() {{
            allowing(mockDocumentReferenceResolver).resolve(with(any(String.class)), with(any(Object[].class)));
                will(returnValue(new DocumentReference("xwiki", "Main", "HelloWorld")));
            allowing(mockDocumentModelBridge).getTitle(); will(returnValue(null));
            allowing(mockDocumentAccessBridge).getDocument(with(any(DocumentReference.class)));
                will(returnValue(mockDocumentModelBridge));
        }});

        Assert.assertEquals("HelloWorld", this.generator.generate(new Link()));
    }

    @org.junit.Test
    public void testGenerateWhithRegexpSyntax() throws Exception
    {
        mockery.checking(new Expectations() {{
            allowing(mockDocumentModelBridge).getTitle(); will(returnValue("$0"));
            allowing(mockDocumentAccessBridge).getDocument(with(any(DocumentReference.class)));
                will(returnValue(mockDocumentModelBridge));
            allowing(mockDocumentReferenceResolver).resolve(with(any(String.class)), with(any(Object[].class)));
                will(returnValue(new DocumentReference("$0", "\\", "$0")));

        }});

        Assert.assertEquals("[$0:\\.$0] $0 ($0) [$0:\\.$0] $0 ($0)", this.generator.generate(new Link()));
    }
}
