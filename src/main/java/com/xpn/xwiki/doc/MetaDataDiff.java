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
