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

 * Created by
 * User: Ludovic Dubost
 * Date: 25 déc. 2003
 * Time: 13:32:37
 */
package com.xpn.xwiki.objects;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.StringWriter;

import com.xpn.xwiki.XWikiContext;

public abstract class BaseElement extends Object {
    private String name;
    private String prettyName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrettyName() {
        return prettyName;
    }

    public void setPrettyName(String name) {
        this.prettyName = name;
    }

    public boolean equals(Object el) {
        if (el==null)
            return false;

        BaseElement element = (BaseElement ) el;

        if (element.getName()==null) {
            if (getName()!=null)
                return false;
        } else if (!element.getName().equals(getName()))
            return false;

        if (element.getPrettyName()==null) {
            if (getPrettyName()!=null)
                return false;
        } else if (!element.getPrettyName().equals(getPrettyName()))
            return false;

        if (element.getClass()==null) {
            if (getClass()!=null)
                return false;
        } else if (!(element.getClass().equals(this.getClass())))
            return false;

        return true;
    }

    public Object clone()
    {
        BaseElement element = null;
        try {
            element = (BaseElement) getClass().newInstance();
        } catch (Exception e) {
            // This should not happen
        }
        element.setName(getName());
        element.setPrettyName(getPrettyName());
        return element;
    }

}
