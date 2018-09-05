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
package com.xpn.xwiki.test.mockito;

import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

/**
 * Various matchers for oldcore APIs.
 * 
 * @version $Id$
 * @since 7.0M1
 */
public class OldcoreMatchers
{
    public static XWikiDocument isDocument(DocumentReference expected)
    {
        return argThat(new XWikiDocumentMatcher(expected));
    }

    /**
     * @since 7.1M1
     */
    public static CacheConfiguration isCacheConfiguration(String id)
    {
        return argThat(new CacheConfigurationMatcher(id));
    }

    /**
     * Any <code>XWikiContext</code>.
     * 
     * @since 7.3RC1
     */
    public static XWikiContext anyXWikiContext()
    {
        return any(XWikiContext.class);
    }

    /**
     * Match a {@link XWikiContext} parameter containing the passed wiki identifier.
     * 
     * @param wikiId the wiki identifier to match
     * @return since 10.8RC1
     */
    public static XWikiContext isContextWiki(String wikiId)
    {
        return argThat(new XWikiContextMatcher(wikiId));
    }

    /**
     * Any <code>XWikiDocument</code>.
     * 
     * @since 7.3RC1
     */
    public static XWikiDocument anyXWikiDocument()
    {
        return any(XWikiDocument.class);
    }
}
