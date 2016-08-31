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
package com.xpn.xwiki.internal.model;

/**
 * Represents an abstract page.
 * 
 * @version $Id$
 * @since 8.3M2, 7.4.5
 */
public abstract class AbstractXWikiPage
{
    private String reference;

    private boolean terminal;

    private String name;

    private String parent;

    private boolean hidden;

    /**
     * @return the page reference
     */
    public String getReference()
    {
        return reference;
    }

    /**
     * @return whether the page is terminal or not
     */
    public boolean isTerminal()
    {
        return terminal;
    }

    /**
     * @return the page name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return the page parent
     */
    public String getParent()
    {
        return parent;
    }

    /**
     * @return whether the page is hidden or not
     */
    public boolean isHidden()
    {
        return hidden;
    }
}
