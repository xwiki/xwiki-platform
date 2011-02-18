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
package org.xwiki.rendering.internal.renderer.xhtml;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jmock.Expectations;
import org.junit.*;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link XHTMLRenderer}.
 */
public class XHTMLRendererTest extends AbstractComponentTestCase
{
    private PrintRenderer renderer;

    private WikiModel mockWikiModel;

    @Override
    protected void registerComponents() throws Exception
    {
        // Register a mock implementation of WikiModel in order to perform expectations on the generation of
        // document and image URLs.
        this.mockWikiModel = getMockery().mock(WikiModel.class);
        DefaultComponentDescriptor<WikiModel> cd = new DefaultComponentDescriptor<WikiModel>();
        cd.setRole(WikiModel.class);
        getComponentManager().registerComponent(cd, this.mockWikiModel);

        this.renderer = getComponentManager().lookup(PrintRenderer.class, "xhtml/1.0");
    }

    /**
     * Verify that when an XDOM contains a MetaDataBlock with a "source" metaData specified, then this "source" is
     * set in the ResourceReference passed to the WikiModel call when getting a document link URL.
     */
    @Test
    public void testBeginLinkHasBaseResourceReferencePassedWhenSourceMetaDataAdded()
    {
        final ResourceReference blockReference = new ResourceReference("reference", ResourceType.DOCUMENT);
        List<Block> linkBlocks = Arrays.asList((Block) new LinkBlock(Arrays.asList((Block) new WordBlock("label")),
            blockReference, true));
        MetaData metaData1 = new MetaData();
        metaData1.addMetaData(MetaData.SOURCE, "base1");
        MetaData metaData2 = new MetaData();
        metaData2.addMetaData(MetaData.SOURCE, "base2");
        XDOM xdom = new XDOM(Arrays.asList((Block) new MetaDataBlock(
            Arrays.asList((Block) new MetaDataBlock(linkBlocks, metaData2)), metaData1)));

        getMockery().checking(new Expectations() {{
            // This is the part of the test verification: we verify that the passed Resource Reference has its base
            // reference set.
            ResourceReference reference = new ResourceReference("reference", ResourceType.DOCUMENT);
            reference.addBaseReference("base1");
            reference.addBaseReference("base2");
            oneOf(mockWikiModel).isDocumentAvailable(reference);
            will(returnValue(true));
            oneOf(mockWikiModel).getDocumentViewURL(reference);
            will(returnValue("viewurl"));
        }});

        this.renderer.setPrinter(new DefaultWikiPrinter());
        xdom.traverse(this.renderer);
    }

    /**
     * Verify that when an XDOM contains a MetaDataBlock with a "source" metaData specified, then this "source" is
     * not used if the ResourceReference passed to the WikiModel already has a base reference specified.
     */
    @Test
    public void testBeginLinkDoesntUseSourceMetaDataIfBaseReferenceSpecified()
    {
        final ResourceReference blockReference = new ResourceReference("reference", ResourceType.DOCUMENT);
        blockReference.addBaseReference("original base");

        List<Block> linkBlocks = Arrays.asList((Block) new LinkBlock(Arrays.asList((Block) new WordBlock("label")),
            blockReference, true));
        MetaData metaData = new MetaData();
        metaData.addMetaData(MetaData.SOURCE, "base");
        XDOM xdom = new XDOM(Arrays.asList((Block) new MetaDataBlock(linkBlocks, metaData)));

        getMockery().checking(new Expectations() {{
            // This is the part of the test verification: we verify that the passed Resource Reference has its base
            // reference set.
            ResourceReference reference = new ResourceReference("reference", ResourceType.DOCUMENT);
            reference.addBaseReference("original base");
            oneOf(mockWikiModel).isDocumentAvailable(reference);
            will(returnValue(true));
            oneOf(mockWikiModel).getDocumentViewURL(reference);
            will(returnValue("viewurl"));
        }});

        this.renderer.setPrinter(new DefaultWikiPrinter());
        xdom.traverse(this.renderer);
    }

    /**
     * Verify that when an XDOM contains a MetaDataBlock with a "source" metaData specified, then this "source" is
     * set in the ResourceReference passed to the WikiModel call when getting an image link URL.
     */
    @Test
    public void testOnImageHasBaseResourceReferencePassedWhenSourceMetaDataAdded()
    {
        final ResourceReference blockReference = new ResourceReference("reference", ResourceType.ATTACHMENT);
        List<Block> imageBlocks = Arrays.asList((Block) new ImageBlock(blockReference, true));
        MetaData metaData = new MetaData();
        metaData.addMetaData(MetaData.SOURCE, "base");
        XDOM xdom = new XDOM(Arrays.asList((Block) new MetaDataBlock(imageBlocks, metaData)));

        getMockery().checking(new Expectations() {{
            // This is the part of the test verification: we verify that the passed Resource Reference has its base
            // reference set.
            ResourceReference reference = new ResourceReference("reference", ResourceType.ATTACHMENT);
            reference.addBaseReference("base");
            oneOf(mockWikiModel).getImageURL(reference, Collections.<String, String>emptyMap());
            will(returnValue("imageurl"));
        }});

        this.renderer.setPrinter(new DefaultWikiPrinter());
        xdom.traverse(this.renderer);
    }

    /**
     * Verify that when an XDOM contains a MetaDataBlock with a "source" metaData specified, then this "source" is
     * not used if the ResourceReference passed to the WikiModel already has a base reference specified.
     */
    @Test
    public void testOnImageDoesntUseSourceMetaDataIfBaseReferenceSpecified()
    {
        final ResourceReference blockReference = new ResourceReference("reference", ResourceType.ATTACHMENT);
        blockReference.addBaseReference("original base");

        List<Block> imageBlocks = Arrays.asList((Block) new ImageBlock(blockReference, true));
        MetaData metaData = new MetaData();
        metaData.addMetaData(MetaData.SOURCE, "base");
        XDOM xdom = new XDOM(Arrays.asList((Block) new MetaDataBlock(imageBlocks, metaData)));

        getMockery().checking(new Expectations() {{
            // This is the part of the test verification: we verify that the passed Resource Reference has its base
            // reference set.
            ResourceReference reference = new ResourceReference("reference", ResourceType.ATTACHMENT);
            reference.addBaseReference("original base");
            oneOf(mockWikiModel).getImageURL(reference, Collections.<String, String>emptyMap());
            will(returnValue("imageurl"));
        }});

        this.renderer.setPrinter(new DefaultWikiPrinter());
        xdom.traverse(this.renderer);
    }
}
