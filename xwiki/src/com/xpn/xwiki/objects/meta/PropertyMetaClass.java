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
 * Date: 22 déc. 2003
 * Time: 10:24:00
 */
package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StringClass;

public class PropertyMetaClass extends BaseClass {

    public PropertyMetaClass() {
        super();
        StringClass type_class = new StringClass(this);
        type_class.setName("classType");
        type_class.setPrettyName("Class Type");
        type_class.setSize(40);
        // This should not be touched
        // safeput("classType", type_class);
        StringClass name_class = new StringClass(this);
        name_class.setName("name");
        name_class.setPrettyName("Name");
        name_class.setSize(40);
        safeput("name", name_class);
        StringClass prettyname_class = new StringClass(this);
        prettyname_class.setName("prettyName");
        prettyname_class.setPrettyName("Pretty Name");
        prettyname_class.setSize(40);
        safeput("prettyName", prettyname_class);

    }
}
