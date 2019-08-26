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
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.PropertyClassInterface;
import com.xpn.xwiki.objects.classes.StringClass;

/**
 * Defines the meta properties of a string XClass property.
 *
 * @version $Id$
 */
@Component
@Named("String")
@Singleton
public class StringMetaClass extends PropertyMetaClass
{
    /**
     * Default constructor. Initializes the default meta properties of a String XClass property.
     */
    public StringMetaClass()
    {
        setPrettyName("String");
        setName(getClass().getAnnotation(Named.class).value());

        NumberClass sizeClass = new NumberClass(this);
        sizeClass.setName("size");
        sizeClass.setPrettyName("Size");
        sizeClass.setSize(5);
        sizeClass.setNumberType("integer");
        // We set the hint here in order to avoid adding a new translation key with the same message for each property
        // type that extends String property.
        sizeClass.setHint("String_size_hint");
        safeput(sizeClass.getName(), sizeClass);

        BooleanClass pickerClass = new BooleanClass(this);
        pickerClass.setName("picker");
        pickerClass.setPrettyName("Use Suggest");
        pickerClass.setDisplayType("yesno");
        pickerClass.setDisplayFormType("checkbox");
        pickerClass.setDefaultValue(1);
        safeput(pickerClass.getName(), pickerClass);
    }

    @Override
    public PropertyClassInterface getInstance()
    {
        return new StringClass();
    }
}
