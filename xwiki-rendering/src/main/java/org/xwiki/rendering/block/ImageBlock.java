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

import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Listener;

/**
 * Represents an image.
 * 
 * @version $Id$
 * @since 1.7M2
 */
public class ImageBlock extends AbstractBlock
{
    private Image image;
    
    /**
     * If true then the image is defined as a free standing URI directly in the text.
     */
    private boolean isFreeStandingURI;

    public ImageBlock(Image image, boolean isFreeStandingURI)
    {
        this(image, isFreeStandingURI, Collections.<String, String>emptyMap());
    }

    public ImageBlock(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        super(parameters);
        this.image = image;
        this.isFreeStandingURI = isFreeStandingURI;
    }

    public Image getImage()
    {
        return this.image;
    }

    /**
     * @return true if the image is defined as a free standing URI directly in the text, false otherwise
     */
    public boolean isFreeStandingURI()
    {
        return this.isFreeStandingURI;
    }

    public void traverse(Listener listener)
    {
        listener.onImage(getImage(), isFreeStandingURI(), getParameters());
    }
    
    /**
     * {@inheritDoc}
     * @see Object#clone()
     */
    @Override
    public Block clone()
    {
        ImageBlock clone = (ImageBlock) super.clone();
        clone.image = getImage().clone();
        return clone;
    }
}
