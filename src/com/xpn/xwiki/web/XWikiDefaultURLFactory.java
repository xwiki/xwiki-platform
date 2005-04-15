/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 25 mai 2004
 * Time: 12:24:27
 */
package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;

import java.net.URL;

public abstract class XWikiDefaultURLFactory implements XWikiURLFactory {
    public URL createURL(String web, String name, XWikiContext context) {
        return createURL(web, name, "view", null, null, context);
    }

    public URL createExternalURL(String web, String name, String action, String querystring, String anchor, XWikiContext context) {
        return createExternalURL(web, name, action, querystring, anchor, context.getDatabase(), context);
    }

    public URL createURL(String web, String name, String action, XWikiContext context) {
        return createURL(web, name, action, null, null, context);
    }

    public URL createURL(String web, String name, String action, String querystring, String anchor, XWikiContext context) {
        return createURL(web, name, action, querystring, anchor, context.getDatabase(), context);
    }

    public URL createSkinURL(String filename, String web, String name, XWikiContext context) {
        return createSkinURL(filename, web, name, context.getDatabase(), context);
    }

    public URL createAttachmentURL(String filename, String web, String name, String action, XWikiContext context) {
        return createAttachmentURL(filename, web, name, action, context.getDatabase(), context);
    }

    public String getURL(URL url, XWikiContext context) {
        return url.toString();
    }
}
