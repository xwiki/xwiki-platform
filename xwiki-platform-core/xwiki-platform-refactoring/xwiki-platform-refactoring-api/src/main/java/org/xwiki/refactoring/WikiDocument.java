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
package org.xwiki.refactoring;

import org.xwiki.rendering.block.XDOM;

/**
 * Represents an in-memory wiki page used by the {@link DocumentSplitter} interface.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class WikiDocument
{
    /**
     * Full name of the target wiki page.
     */
    private String fullName;

    /**
     * The {@link XDOM} of the target wiki page.
     */
    private XDOM xdom;

    /**
     * The parent {@link WikiDocument} of this document.
     */
    private WikiDocument parent;

    /**
     * Constructs a new {@link WikiDocument}.
     * 
     * @param fullName full name of the target wiki page.
     * @param xdom {@link XDOM} for the target wiki page.
     * @param parent the parent {@link WikiDocument} of this document.
     */
    public WikiDocument(String fullName, XDOM xdom, WikiDocument parent)
    {
        this.fullName = fullName;
        this.xdom = xdom;
        this.parent = parent;
    }

    /**
     * @return full name of the target wiki page.
     */
    public String getFullName()
    {
        return fullName;
    }

    /**
     * @return the {@link XDOM} of the target wiki page.
     */
    public XDOM getXdom()
    {
        return xdom;
    }

    /**
     * @return the parent {@link WikiDocument} of this document.
     */
    public WikiDocument getParent()
    {
        return parent;
    }

    @Override
    public boolean equals(Object obj)
    {
        boolean equals = false;
        if (obj instanceof WikiDocument) {
           equals = ((WikiDocument) obj).getFullName().equals(getFullName());
        }
        return equals;
    }

    @Override
    public int hashCode()
    {
        return getFullName().hashCode();
    }    
}
