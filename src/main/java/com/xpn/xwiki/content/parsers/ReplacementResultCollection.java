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
package com.xpn.xwiki.content.parsers;

import java.util.List;
import java.util.ArrayList;

/**
 * Provides accessors to find all elements that have been replaced successfully when parsing and
 * replacing content. Also returns the modified content after the replacements have been done.
 *
 * @version $Id$
 */
public class ReplacementResultCollection extends ParsingResultCollection
{
    /**
     * @see #getReplacedElements()
     */
    private List replacedElements = new ArrayList();

    /**
     * @see #getModifiedContent()
     */
    private Object modifiedContent;

    /**
     * @param replacedObject the object replaced successfully
     */
    void addReplacedElement(Object replacedObject)
    {
        this.replacedElements.add(replacedObject);
    }

    /**
     * @return the replaced elements. For example {@link com.xpn.xwiki.content.Link} objects.
     */
    public List getReplacedElements()
    {
        return this.replacedElements;
    }

    /**
     * @param modifiedContent see {@link #getModifiedContent()}
     */
    public void setModifiedContent(Object modifiedContent)
    {
        this.modifiedContent = modifiedContent;
    }

    /**
     * @return the modified content after the replacements have been done
     */
    public Object getModifiedContent()
    {
        return this.modifiedContent;
    }
}
