/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 28 déc. 2003
 * Time: 10:32:42
 */
package com.xpn.xwiki.web;



public class PropAddForm extends XWikiForm {

    private String propName;
    private String propType;

    public String getPropName() {
        return propName;
    }

    public void setPropName(String propName) {
        this.propName = propName.replaceAll(" ","");
    }

    public String getPropType() {
        return propType;
    }

    public void setPropType(String propType) {
        this.propType = propType;
    }

    public void readRequest() {
        XWikiRequest request = getRequest();
        setPropName(request.getParameter("propname"));
        setPropType(request.getParameter("proptype"));
    }
}
