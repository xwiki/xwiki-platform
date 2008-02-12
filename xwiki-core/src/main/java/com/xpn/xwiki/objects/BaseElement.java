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
 *
 */

package com.xpn.xwiki.objects;

import java.io.Serializable;


public abstract class BaseElement implements ElementInterface, Serializable {
    private String name;
    private String prettyName;
    private String wiki;

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
    
    /**
     * @return the name of the wiki where this element is stored. If null, the context's wiki is used.
     */
    public String getWiki()
    {
        return wiki;
    }
    
    /**
     * @param wiki the name of the wiki where this element is stored. If null, the context's wiki is used.
     */
    public void setWiki(String wiki)
    {
        this.wiki = wiki;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object el)
    {
        if (el == null)
            return false;

        BaseElement element = (BaseElement) el;

        if (element.getName() == null) {
            if (getName() != null)
                return false;
        } else if (!element.getName().equals(getName()))
            return false;

        if (element.getPrettyName() == null) {
            if (getPrettyName() != null)
                return false;
        } else if (!element.getPrettyName().equals(getPrettyName()))
            return false;

        if (element.getWiki() == null) {
            if (getWiki() != null)
                return false;
        } else if (!element.getWiki().equalsIgnoreCase(getWiki()))
            return false;

        if (!(element.getClass().equals(this.getClass())))
            return false;

        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#clone()
     */
    public Object clone()
    {
        BaseElement element = null;
        try {
            element = (BaseElement) getClass().newInstance();
            element.setName(getName());
            element.setPrettyName(getPrettyName());
            element.setWiki(getWiki());
            return element;
        } catch (Exception e) {
            // This should not happen
        }
        return null;
    }

}
