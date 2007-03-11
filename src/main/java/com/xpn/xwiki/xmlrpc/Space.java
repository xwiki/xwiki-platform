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
 */

package com.xpn.xwiki.xmlrpc;

import java.util.Map;

public class Space extends SpaceSummary {
    private String description;
    private String homepage;

    public Space(String key, String name, String url, String description, String homepage) {
        super(key, name, url);
        this.setDescription(description);
        this.setHomepage(homepage);
    }

    public Space(Map parameters) {
        super(parameters);
        this.setDescription(((String) parameters.get("description")));
        this.setHomepage(((String) parameters.get("homepage")));
    }    
    
    Map getParameters() {
        Map params = super.getParameters();
        params.put("description", getDescription());
        params.put("homepage", getHomepage());
        return params;
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
