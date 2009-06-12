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
package org.xwiki.rendering.internal.parser;

import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.listener.Attachment;
import org.xwiki.rendering.listener.DocumentImage;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.URLImage;
import org.xwiki.rendering.parser.ImageParser;
import org.xwiki.rendering.wiki.WikiModel;

/**
 * Common implementation for Image parsers. The implementation handles both cases when we're in wiki mode
 * and when we're not.
 *  
 * @version $Id$
 * @since 2.0M1
 */
public abstract class AbstractImageParser implements ImageParser
{
    /**
     * Used to verify if we're in wiki mode or not by looking up an implementation of {@link WikiModel}.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * @param attachment the attachment to parse as a string
     * @return the parsed attachment string as an {@link Attachment}Êobject
     */
    protected abstract Attachment parseAttachment(String attachment);

    /**
     * {@inheritDoc}
     * 
     * @see ImageParser#parse(String)
     */
    public Image parse(String imageLocation)
    {
        Image result;

        // TODO: Shouldn't we store a DocumentIdentity object instead in Image and make sure that it's never null
        // by using the current document when not specified?
        if (imageLocation.startsWith("http://") || !isInWikiMode()) {
            result = new URLImage(imageLocation);
        } else {
            result = new DocumentImage(parseAttachment(imageLocation));
        }

        return result;
    }
    
    /**
     * @return true if we're in wiki mode (ie there's no implementing class for {@link WikiModel})
     */
    private boolean isInWikiMode()
    {
        boolean result = true;
        try {
            this.componentManager.lookup(WikiModel.class);
        } catch (ComponentLookupException e) {
            result = false;
        }
        return result;
    }
}
