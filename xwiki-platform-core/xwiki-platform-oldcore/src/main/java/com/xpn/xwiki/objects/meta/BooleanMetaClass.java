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
 */
package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;

public class BooleanMetaClass extends PropertyMetaClass
{
    public BooleanMetaClass()
    {
        super();
        setPrettyName("Boolean");
        setName(BooleanClass.class.getName());

        StaticListClass type_class = new StaticListClass(this);
        type_class.setName("displayFormType");
        type_class.setPrettyName("Display Form Type");
        type_class.setValues("radio|checkbox|select");
        safeput("displayFormType", type_class);

        StringClass value_class = new StringClass(this);
        value_class.setName("displayType");
        value_class.setPrettyName("Display Type");
        value_class.setSize(20);
        safeput("displayType", value_class);

        NumberClass default_value_class = new NumberClass(this);
        default_value_class.setName("defaultValue");
        default_value_class.setPrettyName("Default Value");
        default_value_class.setSize(5);
        default_value_class.setClassType("integer");
        safeput("defaultValue", default_value_class);
    }

    @Override
    public BaseCollection newObject(XWikiContext context)
    {
        return new BooleanClass();
    }
}
