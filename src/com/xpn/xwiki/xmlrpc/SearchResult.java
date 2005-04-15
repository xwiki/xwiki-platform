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
 * Time: 16:49:48
 */
package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.doc.XWikiDocument;

import java.util.Hashtable;

public class SearchResult {
   private String title;
   private String url;
   private String excerpt;
   private String id;
   private String type;


    public SearchResult(String title, String url, String excerpt, String id, String type) {
        setTitle(title);
        setUrl(url);
        setExcerpt(excerpt);
        setId(id);
        setType(type);
    }

    public SearchResult(XWikiDocument document) {
        setTitle(document.getFullName());
        setId(document.getFullName());
        setUrl("http://127.0.0.1:9080/xwiki/bin/view/" + document.getWeb() + "/" + document.getName());
        setType("page");
        String content = document.getContent();
        if (content.length()<=256)
         setExcerpt(content);
        else
         setExcerpt(content.substring(0,256));
    }

    public Hashtable getHashtable() {
        Hashtable ht = new Hashtable();
        ht.put("title", getTitle());
        ht.put("url", getUrl());
        ht.put("excerpt", getExcerpt());
        ht.put("id", getId());
        ht.put("type", getType());
        return ht;
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

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
