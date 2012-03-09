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
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StringClass;

public class DateMetaClass extends PropertyMetaClass
{
    public DateMetaClass()
    {
        super();
        // setType("numbermetaclass");
        setPrettyName("Date");
        setName(DateClass.class.getName());

        NumberClass size_class = new NumberClass(this);
        size_class.setName("size");
        size_class.setPrettyName("Size");
        size_class.setSize(5);
        size_class.setNumberType("integer");

        NumberClass emptyistoday_class = new NumberClass(this);
        emptyistoday_class.setName("emptyIsToday");
        emptyistoday_class.setPrettyName("Empty Is Today");
        emptyistoday_class.setSize(5);
        emptyistoday_class.setNumberType("integer");

        BooleanClass picker_class = new BooleanClass(this);
        picker_class.setName("picker");
        picker_class.setPrettyName("Picker");
        picker_class.setDefaultValue(1);

        StringClass dateformat_class = new StringClass(this);
        dateformat_class.setName("dateFormat");
        dateformat_class.setPrettyName("Date Format");
        dateformat_class.setSize(20);

        safeput("size", size_class);
        safeput("emptyIsToday", emptyistoday_class);
        safeput("dateFormat", dateformat_class);
    }

    @Override
    public BaseCollection newObject(XWikiContext context)
    {
        return new DateClass();
    }
}
