/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
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
 *
 * User: ludovic
 * Date: 18 mars 2004
 * Time: 13:53:50
 */

package com.xpn.xwiki.api;

import com.xpn.xwiki.objects.BaseElement;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.XWikiContext;

public class Property extends Element {

    public Property(BaseProperty property, XWikiContext context) {
       super(property, context);
    }

    protected BaseElement getProperty() {
        return element;
    }

    public String getName() {
        return element.getName();
    }
}
