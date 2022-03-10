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
package com.xpn.xwiki.web;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;

public abstract class XWikiDefaultURLFactory implements XWikiURLFactory
{
    @Override
    public URL createURL(String spaces, String name, XWikiContext context)
    {
        return createURL(spaces, name, "view", null, null, context);
    }

    @Override
    public URL createExternalURL(String spaces, String name, String action, String querystring, String anchor,
        XWikiContext context)
    {
        return createExternalURL(spaces, name, action, querystring, anchor, context.getWikiId(), context);
    }

    @Override
    public URL createURL(String spaces, String name, String action, XWikiContext context)
    {
        return createURL(spaces, name, action, null, null, context);
    }

    @Override
    public URL createURL(String spaces, String name, String action, String querystring, String anchor, XWikiContext context)
    {
        return createURL(spaces, name, action, querystring, anchor, context.getWikiId(), context);
    }

    @Override
    public URL createSkinURL(String filename, String spaces, String name, XWikiContext context)
    {
        return createSkinURL(filename, spaces, name, context, Collections.emptyMap());
    }

    @Override
    public URL createSkinURL(String filename, String spaces, String name, XWikiContext context,
        Map<String, Object> queryParameters)
    {
        return createSkinURL(filename, spaces, name, context.getWikiId(), context, queryParameters);
    }

    @Override
    public URL createSkinURL(String filename, String spaces, String name, String xwikidb, XWikiContext context)
    {
        return this.createSkinURL(filename, spaces, name, xwikidb, context, Collections.emptyMap());
    }

    @Override
    public URL createSkinURL(String filename, String skin, XWikiContext context)
    {
        return createSkinURL(filename, skin, context, Collections.emptyMap());
    }

    @Override
    public URL createResourceURL(String filename, boolean forceSkinAction, XWikiContext context)
    {
        return this.createResourceURL(filename, forceSkinAction, context, Collections.emptyMap());
    }

    @Override
    public URL createAttachmentURL(String filename, String spaces, String name, String action, String querystring,
        XWikiContext context)
    {
        return createAttachmentURL(filename, spaces, name, action, querystring, context.getWikiId(), context);
    }

    @Override
    public URL createAttachmentRevisionURL(String filename, String spaces, String name, String revision,
        String querystring, XWikiContext context)
    {
        return createAttachmentRevisionURL(filename, spaces, name, revision, querystring, context.getWikiId(), context);
    }

    public URL createAttachmentRevisionURL(String filename, String spaces, String name, String revision,
        XWikiContext context)
    {
        return createAttachmentRevisionURL(filename, spaces, name, revision, null, context.getWikiId(), context);
    }

    @Override
    public URL getRequestURL(XWikiContext context)
    {
        return context.getURL();
    }

    @Override
    public String getURL(URL url, XWikiContext context)
    {
        return url.toString();
    }
}
