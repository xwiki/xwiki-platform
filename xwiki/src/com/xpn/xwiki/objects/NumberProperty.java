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
 * Time: 14:04:34
 */
package com.xpn.xwiki.objects;


public abstract class NumberProperty extends BaseProperty {
    private Number value;

    public NumberProperty() {
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = (Number)value;
    }

    public String toString() {
        Number nb = (Number)getValue();
        return (nb==null) ? "" : nb.toString();
    }

    public boolean equals(Object obj) {
        if (!super.equals(obj))
         return false;

       if ((getValue()==null)
            && (((NumberProperty)obj).getValue()==null))
         return true;

       return getValue().equals(((NumberProperty)obj).getValue());
    }

    public Object clone() {
        NumberProperty property = (NumberProperty) super.clone();
        property.setValue(getValue());
        return property;
    }

}
