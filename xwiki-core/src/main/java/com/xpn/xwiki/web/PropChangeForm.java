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
package com.xpn.xwiki.web;

/**
 * Struts Form for the class {@link PropDeleteAction property delete}, {@link PropDisableAction property disable} and
 * {@link PropEnableAction property enable} actions.
 * 
 * @version $Id$
 * @since 2.4M2
 */
public class PropChangeForm extends XWikiForm
{
    /** The name of the class property to remove. */
    private String propertyName;

    /**
     * {@inheritDoc}
     */
    @Override
    public void readRequest()
    {
        setPropertyName(getRequest().getParameter("propname"));
    }

    /**
     * Getter for the {@link #propertyName property name}.
     * 
     * @return the property name specified in the request
     */
    public String getPropertyName()
    {
        return this.propertyName;
    }

    /**
     * Setter for the {@link #propertyName property name}.
     * 
     * @param propertyName the property name specified in the request
     */
    public void setPropertyName(String propertyName)
    {
        this.propertyName = propertyName;
    }
}
