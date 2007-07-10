/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
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

import java.io.Serializable;


public abstract class BaseElement implements ElementInterface, Serializable {
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

        if (!(element.getClass().equals(this.getClass())))
            return false;

        return true;
    }

    public Object clone()
    {
        BaseElement element = null;
        try {
            element = (BaseElement) getClass().newInstance();
            element.setName(getName());
            element.setPrettyName(getPrettyName());
            return element;
        } catch (Exception e) {
            // This should not happen
        }
        return null;
    }

}
