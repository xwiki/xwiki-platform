/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 9 déc. 2003
 * Time: 13:51:00
 */
package com.xpn.xwiki.objects;



public class StringProperty extends BaseProperty {
    private String value;


    public StringProperty() {
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = (String)value;
    }

    public String toText() {
        String value = (String)getValue();
        if (value!=null)
         return value;
        else
         return "";
    }

    public boolean equals(Object obj) {
        if (!super.equals(obj))
         return false;

       if ((getValue()==null)
            && (((StringProperty)obj).getValue()==null))
         return true;

       return getValue().equals(((StringProperty)obj).getValue());
    }

    public Object clone() {
        StringProperty property = (StringProperty) super.clone();
        property.setValue(getValue());
        return property;
    }
}
