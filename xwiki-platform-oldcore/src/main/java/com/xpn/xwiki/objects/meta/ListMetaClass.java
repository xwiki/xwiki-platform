/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
import com.xpn.xwiki.objects.classes.StringClass;

public class ListMetaClass extends PropertyMetaClass
{
    public ListMetaClass()
    {
        super();
        setPrettyName("List");
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
        multi_class.setDisplayFormType("checkbox");
        multi_class.setDefaultValue(0);
        safeput("multiSelect", multi_class);

        BooleanClass relational_class = new BooleanClass(this);
        relational_class.setName("relationalStorage");
        relational_class.setPrettyName("Relational Storage");
        relational_class.setDisplayType("yesno");
        relational_class.setDisplayFormType("checkbox");
        relational_class.setDefaultValue(0);
        safeput("relationalStorage", relational_class);

        BooleanClass picker_class = new BooleanClass(this);
        picker_class.setName("picker");
        picker_class.setPrettyName("Use Suggest");
        picker_class.setDisplayType("yesno");
        picker_class.setDisplayFormType("checkbox");
        picker_class.setDefaultValue(1);
        safeput("picker", picker_class);

        NumberClass size_class = new NumberClass(this);
        size_class.setName("size");
        size_class.setPrettyName("Size");
        size_class.setSize(5);
        size_class.setNumberType("integer");
        safeput("size", size_class);

        StringClass separators_class = new StringClass(this);
        separators_class.setName("separators");
        separators_class.setPrettyName("Multiselect separators (for editing)");
        separators_class.setSize(5);
        safeput("separators", separators_class);

        StringClass separator_class = new StringClass(this);
        separator_class.setName("separator");
        separator_class.setPrettyName("Join separator (for display)");
        separator_class.setSize(5);
        safeput("separator", separator_class);

        StaticListClass sort_class = new StaticListClass(this);
        sort_class.setName("sort");
        sort_class.setPrettyName("Sort");
        sort_class.setValues("none|id|value");
        safeput("sort", sort_class);

        BooleanClass cache_class = new BooleanClass(this);
        cache_class.setName("cache");
        cache_class.setPrettyName("Cache");
        cache_class.setDisplayType("yesno");
        cache_class.setDisplayFormType("checkbox");
        cache_class.setDefaultValue(0);
        safeput("cache", cache_class);
    }
}
