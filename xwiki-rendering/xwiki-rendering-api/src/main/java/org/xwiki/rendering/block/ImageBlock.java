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
import java.util.Map;

import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.ResourceReference;

/**
 * Represents an image.
 * 
 * @version $Id$
 * @since 1.7M2
 */
public class ImageBlock extends AbstractBlock
{
    /**
     * A reference to the image target. See {@link org.xwiki.rendering.listener.ResourceReference} for more details.
     */
    private ResourceReference reference;

    /**
     * If true then the image is defined as a free standing URI directly in the text.
     */
    private boolean isFreeStandingURI;

    /**
     * @param reference the image reference
     * @param isFreeStandingURI indicate if the image syntax is simple a full descriptive syntax (detail depending of
     *            the syntax)
     * @since 2.5RC1
     */
    public ImageBlock(ResourceReference reference, boolean isFreeStandingURI)
    {
        this(reference, isFreeStandingURI, Collections.<String, String> emptyMap());
    }

    /**
     * @param reference the image reference
     * @param isFreeStandingURI indicate if the image syntax is simple a full descriptive syntax (detail depending of
     *            the syntax)
     * @param parameters the custom parameters
     * @since 2.5RC1
     */
    public ImageBlock(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        super(parameters);

        this.reference = reference;
        this.isFreeStandingURI = isFreeStandingURI;
    }

    /**
     * @return the reference to the image
     * @see org.xwiki.rendering.listener.ResourceReference
     * @since 2.5RC1
     */
    public ResourceReference getReference()
    {
        return this.reference;
    }

    /**
     * @return true if the image is defined as a free standing URI directly in the text, false otherwise
     */
    public boolean isFreeStandingURI()
    {
        return this.isFreeStandingURI;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.Block#traverse(org.xwiki.rendering.listener.Listener)
     */
    public void traverse(Listener listener)
    {
        listener.onImage(getReference(), isFreeStandingURI(), getParameters());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.AbstractBlock#clone(org.xwiki.rendering.block.BlockFilter)
     * @since 1.8RC2
     */
    @Override
    public ImageBlock clone(BlockFilter blockFilter)
    {
        ImageBlock clone = (ImageBlock) super.clone(blockFilter);
        clone.reference = getReference().clone();
        return clone;
    }
}
