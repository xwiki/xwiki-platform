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
 * Time: 09:21:13
 */
package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.BaseClass;

public class StringMetaClass extends PropertyMetaClass {

   public void StringMetaClass() {
    setType("stringmetaclass");
    setName("stringclass");
    setPrettyName("String Class");

    NumberClass size_class = new NumberClass(this);
    size_class.setSize(5);
    size_class.setNumberType("integer");
    safeput("size", size_class);
  }
}
