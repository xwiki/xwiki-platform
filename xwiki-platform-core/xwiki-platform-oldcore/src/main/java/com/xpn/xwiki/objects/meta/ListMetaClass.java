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

import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;

/**
 * Defines the default meta properties for all list XClass property types.
 *
 * @version $Id$
 */
public class ListMetaClass extends PropertyMetaClass
{
    /**
     * Default constructor. Initializes the default meta properties of all List XClass property.
     */
    public ListMetaClass()
    {
        addPresentationMetaProperties();

        BooleanClass relationalStorageClass = newCheckBox(false);
        relationalStorageClass.setName("relationalStorage");
        relationalStorageClass.setPrettyName("Relational Storage");
        safeput(relationalStorageClass.getName(), relationalStorageClass);

        BooleanClass cacheClass = newCheckBox(false);
        cacheClass.setName("cache");
        cacheClass.setPrettyName("Cache");
        safeput(cacheClass.getName(), cacheClass);

        StringClass defaultClass = new StringClass(this);
        defaultClass.setName("defaultValue");
        defaultClass.setPrettyName("Default value");
        defaultClass.setUnmodifiable(true);
        defaultClass.setSize(40);
        safeput(defaultClass.getName(), defaultClass);
    }

    /**
     * Adds the meta properties that control how the XClass property is displayed in edit and view mode.
     */
    private void addPresentationMetaProperties()
    {
        StaticListClass displayTypeClass = new StaticListClass(this);
        displayTypeClass.setName("displayType");
        displayTypeClass.setPrettyName("Display Type");
        displayTypeClass.setValues("input|select|radio|checkbox");
        safeput(displayTypeClass.getName(), displayTypeClass);

        BooleanClass multiSelectClass = newCheckBox(false);
        multiSelectClass.setName("multiSelect");
        multiSelectClass.setPrettyName("Multiple Select");
        safeput(multiSelectClass.getName(), multiSelectClass);

        BooleanClass pickerClass = newCheckBox(true);
        pickerClass.setName("picker");
        pickerClass.setPrettyName("Use Suggest");
        safeput(pickerClass.getName(), pickerClass);

        NumberClass sizeClass = new NumberClass(this);
        sizeClass.setName("size");
        sizeClass.setPrettyName("Size");
        sizeClass.setSize(5);
        sizeClass.setNumberType("integer");
        safeput(sizeClass.getName(), sizeClass);

        addValueSeparatorMetaProperties();

        StaticListClass sortClass = new StaticListClass(this);
        sortClass.setName("sort");
        sortClass.setPrettyName("Sort");
        sortClass.setValues("none|id|value");
        safeput(sortClass.getName(), sortClass);
    }

    /**
     * Adds the meta properties that control how list values are separated in edit and view mode.
     */
    private void addValueSeparatorMetaProperties()
    {
        StringClass separatorsClass = new StringClass(this);
        separatorsClass.setName("separators");
        separatorsClass.setPrettyName("Multiselect separators (for editing)");
        separatorsClass.setSize(5);
        safeput(separatorsClass.getName(), separatorsClass);

        StringClass separatorClass = new StringClass(this);
        separatorClass.setName("separator");
        separatorClass.setPrettyName("Join separator (for display)");
        separatorClass.setSize(5);
        safeput(separatorClass.getName(), separatorClass);
    }

    /**
     * Creates a new boolean property that is displayed as a check box.
     *
     * @param checked whether the check box is checked or not by default
     * @return a new {@link BooleanClass} instance that is displayed as a check box
     */
    private BooleanClass newCheckBox(boolean checked)
    {
        BooleanClass checkBox = new BooleanClass(this);
        checkBox.setDisplayType("yesno");
        checkBox.setDisplayFormType("checkbox");
        checkBox.setDefaultValue(checked ? 1 : 0);
        return checkBox;
    }
}
