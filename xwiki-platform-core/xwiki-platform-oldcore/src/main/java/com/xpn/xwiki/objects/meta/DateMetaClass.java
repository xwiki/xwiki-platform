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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.PropertyClassInterface;
import com.xpn.xwiki.objects.classes.StringClass;

/**
 * Defines the meta properties of a date XClass property.
 *
 * @version $Id$
 */
@Component
@Named("Date")
@Singleton
public class DateMetaClass extends PropertyMetaClass
{
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor. Initializes the default meta properties of a Date XClass property.
     */
    public DateMetaClass()
    {
        setPrettyName("Date");
        setName(getClass().getAnnotation(Named.class).value());

        NumberClass sizeClass = new NumberClass(this);
        sizeClass.setName("size");
        sizeClass.setPrettyName("Size");
        sizeClass.setSize(5);
        sizeClass.setNumberType("integer");
        safeput(sizeClass.getName(), sizeClass);

        BooleanClass emptyIsTodayClass = new BooleanClass(this);
        emptyIsTodayClass.setName("emptyIsToday");
        emptyIsTodayClass.setPrettyName("Empty Is Today");
        emptyIsTodayClass.setDisplayType("yesno");
        emptyIsTodayClass.setDisplayFormType("checkbox");
        emptyIsTodayClass.setDefaultValue(1);
        safeput(emptyIsTodayClass.getName(), emptyIsTodayClass);

        BooleanClass pickerClass = new BooleanClass(this);
        pickerClass.setName("picker");
        pickerClass.setPrettyName("Use Date Picker");
        pickerClass.setDisplayType(emptyIsTodayClass.getDisplayType());
        pickerClass.setDisplayFormType(emptyIsTodayClass.getDisplayFormType());
        pickerClass.setDefaultValue(1);
        safeput(pickerClass.getName(), pickerClass);

        StringClass dateFormatClass = new StringClass(this);
        dateFormatClass.setName("dateFormat");
        dateFormatClass.setPrettyName("Date Format");
        dateFormatClass.setSize(20);
        safeput(dateFormatClass.getName(), dateFormatClass);
    }

    @Override
    public PropertyClassInterface getInstance()
    {
        return new DateClass();
    }
}
