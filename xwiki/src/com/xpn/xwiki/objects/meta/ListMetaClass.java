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
 * Date: 3 févr. 2004
 * Time: 19:14:30
 */
package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.objects.classes.*;
import com.xpn.xwiki.objects.BaseCollection;

public class ListMetaClass extends PropertyMetaClass {

  public ListMetaClass() {
    super();
    setPrettyName("List Class");
    setName(ListClass.class.getName());

    StaticListClass type_class = new StaticListClass(this);
    type_class.setName("displayType");
    type_class.setPrettyName("Display Type");
    type_class.setValues("input|select|radio|checkbox");
    safeput("displayType", type_class);

    BooleanClass multi_class = new BooleanClass(this);
    multi_class.setName("multiSelect");
    multi_class.setPrettyName("Multiple Select");
    multi_class.setDisplayType("yesno");
    multi_class.setUnmodifiable(true);
    safeput("multiSelect", multi_class);

    BooleanClass relational_class = new BooleanClass(this);
    relational_class.setName("relationalStorage");
    relational_class.setPrettyName("Relational Storage");
    relational_class.setDisplayType("yesno");
    relational_class.setUnmodifiable(true);
    safeput("relationalStorage", relational_class);

    NumberClass size_class = new NumberClass(this);
    size_class.setName("size");
    size_class.setPrettyName("Size");
    size_class.setSize(5);
    size_class.setNumberType("integer");
    safeput("size", size_class);
  }
}
