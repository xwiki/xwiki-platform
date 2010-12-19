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
package org.xwiki.rendering.internal.macro.context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.*;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Unit tests for {@link DefaultXDOMResourceReferenceResolver}.
 *
 * @version $Id$
 * @since 3.0M1
 */
public class DefaultXDOMResourceReferenceResolverTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement(exceptions = { DocumentReferenceResolver.class, EntityReferenceSerializer.class,
        AttachmentReferenceResolver.class })
    private DefaultXDOMResourceReferenceResolver resolver;

    @Test
    public void testResolveDocumentLinkReference() throws Exception
    {
        List<Block> blocks = Arrays.asList((Block) new ParagraphBlock(Arrays.asList((Block) new LinkBlock(
            Collections.<Block>emptyList(), new ResourceReference("", ResourceType.DOCUMENT), false))));

        final DocumentReference docReference = new DocumentReference("wiki", "space", "page");

        this.resolver.resolve(blocks, docReference);

        // Verify the Link block has been modified
        LinkBlock linkBlock = new XDOM(blocks).getChildrenByType(LinkBlock.class, true).get(0);
        Assert.assertEquals(new ResourceReference("wiki:space.page", ResourceType.DOCUMENT), linkBlock.getReference());
    }

    @Test
    public void testResolveAttachmentLinkReference() throws Exception
    {
        List<Block> blocks = Arrays.asList((Block) new ParagraphBlock(Arrays.asList((Block) new LinkBlock(
            Collections.<Block>emptyList(), new ResourceReference("test.png", ResourceType.ATTACHMENT), false))));

        final DocumentReference docReference = new DocumentReference("wiki", "space", "page");

        this.resolver.resolve(blocks, docReference);

        // Verify the Link block has been modified
        LinkBlock linkBlock = new XDOM(blocks).getChildrenByType(LinkBlock.class, true).get(0);
        Assert.assertEquals(new ResourceReference("wiki:space.page@test.png", ResourceType.ATTACHMENT),
            linkBlock.getReference());
    }

    @Test
    public void testResolveAttachmentImageReference() throws Exception
    {
        List<Block> blocks = Arrays.asList((Block) new ParagraphBlock(Arrays.asList((Block) new ImageBlock(
            new ResourceReference("test.png", ResourceType.ATTACHMENT), false))));

        final DocumentReference docReference = new DocumentReference("wiki", "space", "page");

        this.resolver.resolve(blocks, docReference);

        // Verify the Image block has been modified
        ImageBlock imageBlock = new XDOM(blocks).getChildrenByType(ImageBlock.class, true).get(0);
        Assert.assertEquals(new ResourceReference("wiki:space.page@test.png", ResourceType.ATTACHMENT),
            imageBlock.getReference());
    }

    @Test
    public void testResolveDoesntChangeOtherLinkTypes() throws Exception
    {
        List<Block> blocks = Arrays.asList((Block) new ParagraphBlock(Arrays.asList((Block) new LinkBlock(
            Collections.<Block>emptyList(), new ResourceReference("url", ResourceType.URL), false))));

        final DocumentReference docReference = new DocumentReference("wiki", "space", "page");

        this.resolver.resolve(blocks, docReference);

        // Verify the Link block has not been modified
        LinkBlock linkBlock = new XDOM(blocks).getChildrenByType(LinkBlock.class, true).get(0);
        Assert.assertEquals(new ResourceReference("url", ResourceType.URL), linkBlock.getReference());
    }

    @Test
    public void testResolveDoesntChangeOtherImageTypes() throws Exception
    {
        List<Block> blocks = Arrays.asList((Block) new ParagraphBlock(Arrays.asList((Block) new ImageBlock(
            new ResourceReference("url", ResourceType.URL), false))));

        final DocumentReference docReference = new DocumentReference("wiki", "space", "page");

        this.resolver.resolve(blocks, docReference);

        // Verify the Image block has been modified
        ImageBlock imageBlock = new XDOM(blocks).getChildrenByType(ImageBlock.class, true).get(0);
        Assert.assertEquals(new ResourceReference("url", ResourceType.URL), imageBlock.getReference());
    }
}
