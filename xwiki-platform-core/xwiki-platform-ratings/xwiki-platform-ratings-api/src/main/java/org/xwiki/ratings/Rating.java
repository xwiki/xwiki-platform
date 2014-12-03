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
package org.xwiki.ratings;

import com.xpn.xwiki.objects.BaseObject;

import java.util.Date;

/**
 * Represent a rating : a note given by a user to a container. A container can be : <ul> <li>A Wiki Document</li> <li>A
 * section of a wiki document</li> <li>A sentence in a wiki document</li> <li>A comment</li> <li>A sentence in a
 * comment</li> <li>etc...</li> </ul>
 *
 * @version $Id$
 */
public interface Rating
{
    /**
     * @return the document name for which this rating applies.
     */
    String getDocumentName();

    /**
     * Retrives the current rating as a BaseObject This method is used for compatiblity
     *
     * @return BaseObject rating object
     */
    BaseObject getAsObject() throws RatingsException;

    /**
     * Retrieves the rating unique ID allowing to distinguish it from other ratings of the same container
     *
     * @return String rating ID
     */
    String getRatingId();

    /**
     * Retrieves the rating unique ID allowing to find the rating
     *
     * @return String rating ID
     */
    String getGlobalRatingId();

    /**
     * Retrives the current rating author
     *
     * @return String author of the rating
     */
    String getAuthor();

    /**
     * Retrieves the date of the rating
     *
     * @return Date date of the rating
     */
    Date getDate();

    /**
     * Retrieves the rating value
     *
     * @return int value of rating
     */
    int getVote();

    /**
     * Retrieves additional properties
     *
     * @return Object property value
     */
    Object get(String propertyName);

    /**
     * Retrieves additional properties
     *
     * @return Object property value
     */
    String display(String propertyName, String mode);

    void setAuthor(String author);

    void setDate(Date date);

    void setVote(int vote);

    void save() throws RatingsException;

    boolean remove() throws RatingsException;

    String toString();
}
