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
import com.xpn.xwiki.objects.classes.StringClass;

public class StringMetaClass extends PropertyMetaClass
{
    public StringMetaClass()
    {
        super();
        // setType("stringmetaclass");
        setPrettyName("String");
        setName(StringClass.class.getName());

        NumberClass size_class = new NumberClass(this);
        size_class.setName("size");
        size_class.setPrettyName("Size");
        size_class.setSize(5);
        size_class.setNumberType("integer");
        safeput("size", size_class);

        BooleanClass picker_class = new BooleanClass(this);
        picker_class.setName("picker");
        picker_class.setPrettyName("Use Suggest");
        picker_class.setDisplayType("yesno");
        picker_class.setDisplayFormType("checkbox");
        picker_class.setDefaultValue(1);
        safeput("picker", picker_class);
    }

    @Override
    public BaseCollection newObject(XWikiContext context)
    {
        return new StringClass();
    }
}
