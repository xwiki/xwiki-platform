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

package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StaticListClass;

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
