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

package com.xpn.xwiki.objects;

public class ObjectDiff extends Object {
    private String className;
    private int number;
    private String propName;
    private Object prevValue;
    private Object newValue;
    private String action;

    public ObjectDiff(String className, int number, String action, String propName, Object prevValue, Object newValue) {
        this.setClassName(className);
        this.setNumber(number);
        this.setAction(action);
        this.setPropName(propName);
        this.setPrevValue(prevValue);
        this.setNewValue(newValue);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getPropName() {
        return propName;
    }

    public void setPropName(String propName) {
        this.propName = propName;
    }

    public Object getPrevValue() {
        return prevValue;
    }

    public void setPrevValue(Object prevValue) {
        this.prevValue = prevValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(getClassName());
        buffer.append(".");
        buffer.append(getPropName());
        buffer.append(": ");
        buffer.append(getPrevValue().toString());
        buffer.append(" &gt; ");
        buffer.append(getNewValue().toString());
        return buffer.toString();
    }
}
