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
 * Contains results data when parsing text in wiki syntax. The reason is that there can be syntax
 * errors found while parsing and we don't want to stop the parsing when these happen. Instead we
 * want to let the caller code be handle those errors as it sees fit. This is why we're returning
 * both the valid parsed elements in addition to the parsing errors.
 *
 * @version $Id$
 */
public class ParsingResultCollection
{
    /**
     * @see #getValidElements()
     */
    private List validElements = new ArrayList();

    /**
     * @see #getInvalidElementIds() 
     */
    private List invalidElementIds = new ArrayList();

    /**
     * @param validObject the object parsed when successful
     */
    void addValidElement(Object validObject)
    {
        this.validElements.add(validObject);
    }

    /**
     * @param elementId the id representing the source content which lead to a parsing error.
     *        Usually this is the source content itself.
     */
    void addInvalidElementId(String elementId)
    {
        this.invalidElementIds.add(elementId);
    }

    /**
     * @return the valid parsed elements. For example {@link com.xpn.xwiki.content.Link} objects.
     */
    public List getValidElements()
    {
        return this.validElements;
    }

    /**
     * @return the parsing errors returned as ids. Usually the id is the source content that was
     *         supposed to be parsed
     */
    public List getInvalidElementIds()
    {
        return this.invalidElementIds;
    }

    /**
     * @return true if there are parsing errors or false otherwise
     */
    public boolean hasInvalidElements()
    {
        return (this.invalidElementIds.size() > 0);
    }
}
