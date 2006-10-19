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

import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.*;

public class PropertyMetaClass extends BaseClass implements PropertyInterface {

    public PropertyMetaClass() {
        super();
        StringClass type_class = new StringClass(this);
        type_class.setName("classType");
        type_class.setPrettyName("Class Type");
        type_class.setSize(40);
        type_class.setUnmodifiable(true);
        // This should not be touched
        // safeput("classType", type_class);
        StringClass name_class = new StringClass(this);
        name_class.setName("name");
        name_class.setPrettyName("Name");
        name_class.setUnmodifiable(true);
        name_class.setSize(40);
        safeput("name", name_class);

        StringClass prettyname_class = new StringClass(this);
        prettyname_class.setName("prettyName");
        prettyname_class.setPrettyName("Pretty Name");
        prettyname_class.setSize(40);
        safeput("prettyName", prettyname_class);

        TextAreaClass customdisplay_class = new TextAreaClass(this);
        customdisplay_class.setName("customDisplay");
        customdisplay_class.setPrettyName("Custom Display");
        customdisplay_class.setRows(5);
        customdisplay_class.setSize(80);
        safeput("customDisplay", customdisplay_class);

        BooleanClass unmodif_class = new BooleanClass(this);
        unmodif_class.setName("unmodifiable");
        unmodif_class.setPrettyName("Unmodifiable");
        unmodif_class.setDisplayType("yesno");
        safeput("unmodifiable", unmodif_class);

        NumberClass number_class = new NumberClass(this);
        number_class.setName("number");
        number_class.setPrettyName("Number");
        number_class.setNumberType("integer");
        safeput("number", number_class);

        StringClass validationRegExp_class = new StringClass(this);
        validationRegExp_class.setName("validationRegExp");
        validationRegExp_class.setPrettyName("Validation Regular Expression");
        validationRegExp_class.setSize(40);
        safeput("validationRegExp", validationRegExp_class);
    }

    public BaseCollection getObject() {
        return null;
    }

    public void setObject(BaseCollection object) {
    }

    public String toFormString() {
        return null;
    }
}
