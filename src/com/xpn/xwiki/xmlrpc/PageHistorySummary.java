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
 * Time: 16:35:57
 */
package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.doc.XWikiDocument;

import java.util.Date;
import java.util.Hashtable;

public class PageHistorySummary {
    private String id;
    private int version;
    private Date modified;
    private String modifier;

    public PageHistorySummary(String id, int version, Date modified, String modifier) {
        setId(id);
        setVersion(version);
        setModified(modified);
        setModifier(modifier);
    }

    public PageHistorySummary(XWikiDocument document) {
        this.id = document.getFullName();
        this.version = document.getRCSVersion().getNumbers()[1];
        this.modified = document.getDate();
        this.modifier = document.getAuthor();
    }

    public Hashtable getHashtable() {
        Hashtable ht = new Hashtable();
        ht.put("id", getId());
        ht.put("version", new Integer(getVersion()));
        ht.put("modified", getModified());
        ht.put("modifier", getModifier());
        return ht;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }
}
