/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
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

 * Created by
 * User: Ludovic Dubost
 * Date: 22 juin 2004
 * Time: 13:24:57
 */
package com.xpn.xwiki.doc;

public class MetaDataDiff extends Object {
    private String field;
    private Object prevvalue;
    private Object newvalue;

    public MetaDataDiff(String field, Object prevvalue, Object newvalue) {
        this.setField(field);
        this.setPrevvalue(prevvalue);
        this.setNewvalue(newvalue);
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Object getPrevvalue() {
        return prevvalue;
    }

    public void setPrevvalue(Object prevvalue) {
        this.prevvalue = prevvalue;
    }

    public Object getNewvalue() {
        return newvalue;
    }

    public void setNewvalue(Object newvalue) {
        this.newvalue = newvalue;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(field);
        buf.append(": ");
        buf.append(prevvalue.toString());
        buf.append(" &gt; ");
        buf.append(newvalue.toString());
        return buf.toString();
    }
}
