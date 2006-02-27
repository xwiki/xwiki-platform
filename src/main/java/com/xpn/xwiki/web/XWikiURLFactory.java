/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author wr0ngway
 * @author sdumitriu
 */

package com.xpn.xwiki.web;

import java.net.URL;

import com.xpn.xwiki.XWikiContext;

public interface XWikiURLFactory {
    public void init(XWikiContext context);
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
    public URL getRequestURL(XWikiContext context);
    public String getURL(URL url, XWikiContext context);
}
