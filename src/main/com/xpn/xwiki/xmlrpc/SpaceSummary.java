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
 * Time: 11:41:33
 */
package com.xpn.xwiki.xmlrpc;

import java.util.Hashtable;

public class SpaceSummary {
    private String key;
    private String name;
    private String url;

    public SpaceSummary(String key, String name, String url) {
        this.setKey(key);
        this.setName(name);
        this.setUrl(url);
    }

    public Hashtable getHashtable() {
        Hashtable ht = new Hashtable();
        ht.put("key", getKey());
        ht.put("name", getName());
        ht.put("url", getUrl());
        return ht;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
