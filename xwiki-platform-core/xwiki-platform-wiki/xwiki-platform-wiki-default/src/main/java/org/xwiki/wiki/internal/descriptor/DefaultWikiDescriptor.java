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
package org.xwiki.wiki.internal.descriptor;

import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.internal.descriptor.document.XWikiServerClassDocumentInitializer;

/**
 * Default implementation of the {@link WikiDescriptor}.
 * @version $Id$
 * @since 5.3M2
 */
public class DefaultWikiDescriptor extends WikiDescriptor
{
    /**
     * Relative reference to the XWiki.XWikiServerClass containing wiki descriptor metadata.
     */
    public static final LocalDocumentReference SERVER_CLASS = XWikiServerClassDocumentInitializer.SERVER_CLASS;

    /**
     * Used to associate to a cache key the fact that no descriptor exist.
     */
    public static final DefaultWikiDescriptor VOID = new DefaultWikiDescriptor(null, null);

    /**
     * Constructor.
     * @param wikiId ID of the wiki
     * @param wikiAlias Alias of the wiki
     */
    public DefaultWikiDescriptor(String wikiId, String wikiAlias)
    {
        super(wikiId, wikiAlias);
    }
}
