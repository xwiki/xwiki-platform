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
 * Time: 17:23:05
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
