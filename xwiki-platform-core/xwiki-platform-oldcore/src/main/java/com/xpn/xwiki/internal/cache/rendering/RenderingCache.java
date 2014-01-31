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
package com.xpn.xwiki.internal.cache.rendering;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Cache rendering result.
 * 
 * @version $Id$
 * @since 2.4M1
 */
@Role
public interface RenderingCache
{
    /**
     * The stored result of the rendering of the provided source.
     * 
     * @param documentReference the reference of the document being rendered
     * @param source the source to render
     * @param context the XWiki context
     * @return the cached result, null if it has not been yet cached
     */
    String getRenderedContent(DocumentReference documentReference, String source, XWikiContext context);

    /**
     * Stored the result of the provided source rendering if the cache is enabled for the provided document.
     * 
     * @param documentReference the reference of the document being rendered
     * @param source the source to render
     * @param renderedContent rendering result to cache
     * @param context the XWiki context
     */
    void setRenderedContent(DocumentReference documentReference, String source, String renderedContent,
        XWikiContext context);
}
