/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 */

package com.xpn.xwiki.xmlrpc;

import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

public class SearchResult extends org.codehaus.swizzle.confluence.SearchResult
{
    public SearchResult()
    {
        super();
    }

    public SearchResult(Map data)
    {
        super(data);
    }

    public SearchResult(XWikiDocument document, XWikiContext context)
    {
        setTitle(document.getName());
        setId(document.getFullName());
        setUrl(document.getURL("view", context));
        setType("page");
        String content = document.getContent();
        // TODO is this a good way to generate excerpts?
        if (content.length() <= 256) {
            setExcerpt(content);
        } else {
            setExcerpt(content.substring(0, 256));
        }
    }
}
