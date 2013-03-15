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
package com.xpn.xwiki.doc;

public class MetaDataDiff
{
    private String field;

    private Object prevValue;

    private Object newValue;

    public MetaDataDiff(String field, Object prevValue, Object newValue)
    {
        this.setField(field);
        this.setPrevValue(prevValue);
        this.setNewValue(newValue);
    }

    public String getField()
    {
        return this.field;
    }

    public void setField(String field)
    {
        this.field = field;
    }

    public Object getPrevValue()
    {
        return this.prevValue;
    }

    public void setPrevValue(Object prevvalue)
    {
        this.prevValue = prevvalue;
    }

    public Object getNewValue()
    {
        return this.newValue;
    }

    public void setNewValue(Object newValue)
    {
        this.newValue = newValue;
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(this.field);
        buf.append(": ");
        buf.append(this.prevValue.toString());
        buf.append(" \u21E8 ");
        buf.append(this.newValue.toString());
        return buf.toString();
    }
}
