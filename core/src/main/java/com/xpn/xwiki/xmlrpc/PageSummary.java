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
 * @author sdumitriu
 */

package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import java.util.Map;
import java.util.HashMap;

public class PageSummary  {
    private String id;
    private String space;
    private String parentId;
    private String title;
    private String url;
    private int locks;


   public PageSummary(String id, String space, String parentId, String title, String url, int locks) {
       this.setId(id);
       this.setSpace(space);
       this.setParentId(parentId);
       this.setTitle(title);
       this.setUrl(url);
       this.setLocks(locks);
   }

    public PageSummary(Map pageht) {
        this.setId((String)pageht.get("id"));
        this.setSpace((String)pageht.get("space"));
        this.setParentId((String)pageht.get("parentId"));
        this.setTitle((String)pageht.get("title"));
        this.setUrl((String)pageht.get("url"));
        this.setLocks(((Integer)pageht.get("locks")).intValue());
    }

    public PageSummary(XWikiDocument doc, XWikiContext context) {
        this.setId(doc.getFullName());
        this.setSpace(doc.getSpace());
        this.setParentId(doc.getParent());
        this.setTitle(doc.getFullName());
        this.setUrl(doc.getURL("view", context));
        this.setLocks(0);
    }

    Map getParameters() {
        Map params = new HashMap();
        params.put("id", getId());
        params.put("space", getSpace());
        params.put("parentId", getParentId());
        params.put("title", getTitle());
        params.put("url", getUrl());
        params.put("locks", new Integer(getLocks()));
        return params;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getLocks() {
        return locks;
    }

    public void setLocks(int locks) {
        this.locks = locks;
    }
}
