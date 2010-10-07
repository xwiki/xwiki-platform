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
package org.xwiki.rendering.block;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.ResourceReference;

/**
 * Represents a Link element in a page.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class LinkBlock extends AbstractFatherBlock
{
    /**
     * A link. See {@link org.xwiki.rendering.listener.ResourceReference} for more details.
     */
    private ResourceReference reference;

    /**
     * If true then the link is a free standing URI directly in the text.
     */
    private boolean isFreeStandingURI;

    /**
     * @param childrenBlocks the nested children blocks
     * @param reference the reference to the target resource to link to
     * @param isFreeStandingURI if true then the link is a free standing URI directly in the text
     */
    public LinkBlock(List<Block> childrenBlocks, ResourceReference reference, boolean isFreeStandingURI)
    {
        this(childrenBlocks, reference, isFreeStandingURI, Collections.<String, String> emptyMap());
    }

    /**
     * @param childrenBlocks the nested children blocks
     * @param reference the reference to the target resource to link to
     * @param isFreeStandingURI if true then the link is a free standing URI directly in the text
     * @param parameters the parameters to set
     */
    public LinkBlock(List<Block> childrenBlocks, ResourceReference reference, boolean isFreeStandingURI,
        Map<String, String> parameters)
    {
        super(childrenBlocks, parameters);
        this.reference = reference;
        this.isFreeStandingURI = isFreeStandingURI;
    }

    /**
     * @return the link
     * @see org.xwiki.rendering.listener.ResourceReference
     */
    public ResourceReference getReference()
    {
        return this.reference;
    }

    /**
     * @return true if the link is a free standing URI directly in the text, false otherwise
     */
    public boolean isFreeStandingURI()
    {
        return this.isFreeStandingURI;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.AbstractFatherBlock#before(org.xwiki.rendering.listener.Listener)
     */
    public void before(Listener listener)
    {
        listener.beginLink(getReference(), isFreeStandingURI(), getParameters());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.AbstractFatherBlock#after(org.xwiki.rendering.listener.Listener)
     */
    public void after(Listener listener)
    {
        listener.endLink(getReference(), isFreeStandingURI(), getParameters());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.AbstractBlock#clone(org.xwiki.rendering.block.BlockFilter)
     * @since 1.8RC2
     */
    @Override
    public LinkBlock clone(BlockFilter blockFilter)
    {
        LinkBlock clone = (LinkBlock) super.clone(blockFilter);
        clone.reference = getReference().clone();
        return clone;
    }
}
