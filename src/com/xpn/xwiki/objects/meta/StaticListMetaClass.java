/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
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
 * Date: 3 févr. 2004
 * Time: 19:20:59
 */
package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;

public class StaticListMetaClass extends ListMetaClass {


    public StaticListMetaClass() {
        super();
        setPrettyName("Static List Class");
        setName(StaticListClass.class.getName());

        StringClass values_class = new StringClass(this);
        values_class.setName("values");
        values_class.setPrettyName("Values");
        values_class.setSize(40);
        safeput("values", values_class);
    }

    public BaseCollection newObject() {
        return new StaticListClass();
    }
}
