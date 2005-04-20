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
 * Time: 12:16:56
 */
package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;

import java.net.URL;

public interface XWikiURLFactory {
    public URL createURL(String web, String name, XWikiContext context);
    public URL createURL(String web, String name, String action, XWikiContext context);
    public URL createURL(String web, String name, String action, boolean redirect, XWikiContext context);
    public URL createURL(String web, String name, String action, String querystring, String anchor,
                         XWikiContext context);
    public URL createExternalURL(String web, String name, String action, String querystring, String anchor,
                         XWikiContext context);
    public URL createURL(String web, String name, String action, String querystring, String anchor,
                         String xwikidb, XWikiContext context);
    public URL createExternalURL(String web, String name, String action, String querystring, String anchor,
                         String xwikidb, XWikiContext context);
    public URL createSkinURL(String filename, String skin, XWikiContext context);
    public URL createSkinURL(String filename, String web, String name, XWikiContext context);
    public URL createSkinURL(String filename, String web, String name, String xwikidb, XWikiContext context);
    public URL createAttachmentURL(String filename, String web, String name, String action, XWikiContext context);
    public URL createAttachmentURL(String filename, String web, String name, String action, String xwikidb, XWikiContext context);
    public String getURL(URL url, XWikiContext context);
}
