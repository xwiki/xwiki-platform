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
 * Date: 17 juin 2004
 * Time: 11:41:26
 */
package com.xpn.xwiki.xmlrpc;

import java.util.Hashtable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

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

    public PageSummary(Hashtable pageht) {
        this.setId((String)pageht.get("id"));
        this.setSpace((String)pageht.get("space"));
        this.setParentId((String)pageht.get("parentId"));
        this.setTitle((String)pageht.get("title"));
        this.setUrl((String)pageht.get("url"));
        this.setLocks(((Integer)pageht.get("locks")).intValue());
    }

    public PageSummary(XWikiDocument doc, XWikiContext context) {
        this.setId(doc.getFullName());
        this.setSpace(doc.getWeb());
        this.setParentId(doc.getParent());
        this.setTitle(doc.getFullName());
        this.setUrl(doc.getURL("view", context));
        this.setLocks(0);
    }

    public Hashtable getHashtable() {
        Hashtable ht = new Hashtable();
        ht.put("id", getId());
        ht.put("space", getSpace());
        ht.put("parentId", getParentId());
        ht.put("title", getTitle());
        ht.put("url", getUrl());
        ht.put("locks", new Integer(getLocks()));
        return ht;
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
