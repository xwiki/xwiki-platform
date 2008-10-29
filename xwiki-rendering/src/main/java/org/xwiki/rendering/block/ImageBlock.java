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

/**
 * Represents an image.
 * 
 * @version $Id$
 * @since 1.7M2
 */
public class ImageBlock extends AbstractBlock
{
    private String imageLocation;
    
    /**
     * If true then the image is defined as a free standing URI directly in the text.
     */
    private boolean isFreeStandingURI;

    public ImageBlock(String imageLocation, boolean isFreeStandingURI)
    {
        this(imageLocation, isFreeStandingURI, Collections.<String, String>emptyMap());
    }

    public ImageBlock(String imageLocation, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        super(parameters);
        this.imageLocation = imageLocation;
        this.isFreeStandingURI = isFreeStandingURI;
    }

    public String getImageLocation()
    {
        return this.imageLocation;
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
        listener.onImage(getImageLocation(), isFreeStandingURI(), getParameters());
    }
}
