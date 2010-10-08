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
package org.xwiki.rendering.internal.parser.reference;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.wiki.WikiModel;

/**
 * @version $Id$
 * @since 2.5RC1
 */
@Component("xwiki/2.0/image")
public class XWiki20ImageReferenceParser implements ResourceReferenceParser
{
    /**
     * Used to verify if we're in wiki mode or not by looking up an implementation of
     * {@link org.xwiki.rendering.wiki.WikiModel}. In non wiki mode all image references are considered as URLs.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.parser.ResourceReferenceParser#parse(String)
     */
    public ResourceReference parse(String rawReference)
    {
        ResourceType type;
        if (rawReference.startsWith("http://") || !isInWikiMode()) {
            type = ResourceType.URL;
        } else {
            type = ResourceType.ATTACHMENT;
        }
        ResourceReference result = new ResourceReference(rawReference, type);
        result.setTyped(false);
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
