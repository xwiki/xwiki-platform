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
 * Time: 11:40:59
 */
package com.xpn.xwiki.xmlrpc;

import java.util.Hashtable;

public class Space extends SpaceSummary {
    private String description;
    private String homepage;

    public Space(String key, String name, String url, String description, String homepage) {
        super(key, name, url);
        this.setDescription(description);
        this.setHomepage(homepage);
    }

    public Hashtable getHashtable() {
        Hashtable ht = super.getHashtable();
        ht.put("description", getDescription());
        ht.put("homepage", getHomepage());
        return ht;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

}
