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

 * Created by
 * User: Ludovic Dubost
 * Date: 19 déc. 2003
 * Time: 18:49:14
 */
package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.PropertyInterface;

public class MetaClass extends BaseClass {

    public void XWikiMetaClass() {
        NumberClass numberclass = new NumberClass(this);
        put("numberclass", numberclass);
        StringClass stringclass = new StringClass(this);
        put("stringclass", numberclass);
    }

    public PropertyInterface get(String name) {
        return (PropertyInterface)fields.get(name);
    }

    public void put(String name, PropertyInterface property) {
        fields.put(name, property);
    }
}
