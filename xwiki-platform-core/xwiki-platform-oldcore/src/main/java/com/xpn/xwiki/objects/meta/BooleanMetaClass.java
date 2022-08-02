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
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;

/**
 * Defines the meta properties of a boolean XClass property.
 *
 * @version $Id$
 */
@Component
@Named("Boolean")
@Singleton
public class BooleanMetaClass extends PropertyMetaClass
{
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor. Initializes the default meta properties of a Boolean XClass property.
     */
    public BooleanMetaClass()
    {
        setPrettyName("Boolean");
        setName(getClass().getAnnotation(Named.class).value());

        StaticListClass typeClass = new StaticListClass(this);
        typeClass.setName("displayFormType");
        typeClass.setPrettyName("Display Form Type");
        typeClass.setValues("radio|checkbox|select");
        safeput(typeClass.getName(), typeClass);

        StringClass valueClass = new StringClass(this);
        valueClass.setName("displayType");
        valueClass.setPrettyName("Display Type");
        valueClass.setSize(20);
        safeput(valueClass.getName(), valueClass);

        NumberClass defaultValueClass = new NumberClass(this);
        defaultValueClass.setName("defaultValue");
        defaultValueClass.setPrettyName("Default Value");
        defaultValueClass.setSize(5);
        defaultValueClass.setNumberType("integer");
        safeput(defaultValueClass.getName(), defaultValueClass);
    }

    @Override
    public PropertyClassInterface getInstance()
    {
        return new BooleanClass();
    }
}
