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
 *
 * User: ludovic
 * Date: 18 mars 2004
 * Time: 14:04:34
 */

package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;


public class PropertyClass extends Collection {

    public PropertyClass(com.xpn.xwiki.objects.classes.PropertyClass property, XWikiContext context) {
        super(property, context);
    }

    protected com.xpn.xwiki.objects.classes.PropertyClass getBasePropertyClass() {
        return (com.xpn.xwiki.objects.classes.PropertyClass) getCollection();
    }

    public String getClassName() {
        return getCollection().getClassName();
    }

    protected com.xpn.xwiki.objects.classes.PropertyClass getPropertyClass() {
        if (checkProgrammingRights())
         return (com.xpn.xwiki.objects.classes.PropertyClass) getCollection();
        else
         return null;
    }
}
