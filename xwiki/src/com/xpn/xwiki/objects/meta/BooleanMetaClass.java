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
 * Date: 2 févr. 2004
 * Time: 17:18:12
 */
package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.BaseCollection;

public class BooleanMetaClass extends PropertyMetaClass {

    public BooleanMetaClass() {
        super();
        setPrettyName("Boolean Class");
        setName(BooleanClass.class.getName());

        StringClass type_class = new StringClass(this);
        type_class.setName("displayType");
        type_class.setPrettyName("Display Type");
        type_class.setSize(20);
        safeput("displayType", type_class);
    }

    public BaseCollection newObject() {
        return new BooleanClass();
    }

}
