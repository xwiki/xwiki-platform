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

import com.xpn.xwiki.doc.XWikiDocument;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;

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

    Map getParameters() {
        Map params = new HashMap();
        params.put("id", getId());
        params.put("version", new Integer(getVersion()));
        params.put("modified", getModified());
        params.put("modifier", getModifier());
        return params;
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
