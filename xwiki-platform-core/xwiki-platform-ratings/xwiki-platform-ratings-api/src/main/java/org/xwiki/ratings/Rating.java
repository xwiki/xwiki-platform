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

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import java.util.Date;

/**
 * Represent a rating : a note given by a user to a container. A container can be: A Wiki Document A section of a wiki
 * document A sentence in a wiki document A comment A sentence in a comment etc...
 *
 * @version $Id$
 * @since 6.4M3
 */
@Unstable
public interface Rating
{
    /**
     * Gets the document reference to which the rating is associated to.
     * 
     * @return the document reference for which this rating applies.
     */
    DocumentReference getDocumentReference();

    /**
     * Retrieves the current rating as a BaseObject This method is used for compatibility.
     *
     * @return BaseObject rating object
     * @throws RatingsException when an error occurs while fetching this average rating.
     */
    BaseObject getAsObject() throws RatingsException;

    /**
     * Retrieves the rating unique ID allowing to distinguish it from other ratings of the same container.
     *
     * @return String rating ID
     */
    String getRatingId();

    /**
     * Retrieves the rating unique ID allowing to find the rating.
     *
     * @return String rating ID
     */
    String getGlobalRatingId();

    /**
     * Retrieves the current rating author.
     *
     * @return String author of the rating
     */
    DocumentReference getAuthor();

    /**
     * Retrieves the date of the rating.
     *
     * @return Date date of the rating
     */
    Date getDate();

    /**
     * Retrieves the rating value.
     *
     * @return integer value of rating
     */
    int getVote();

    /**
     * Retrieves additional properties.
     *
     * @param propertyName the name of the property for which to retrieve the value
     * @return Object property value
     */
    Object get(String propertyName);

    /**
     * Retrieves additional properties.
     *
     * @param propertyName the name of the property for which to retrieve the value
     * @param mode the mode in which to display the value
     * @return Object property value
     */
    String display(String propertyName, String mode);

    /**
     * Set the author to which the rating belongs to.
     *
     * @param author to which the rating belongs to
     */
    void setAuthor(DocumentReference author);

    /**
     * Set the date when the rating occurred.
     *
     * @param date when the rating occurred
     */
    void setDate(Date date);

    /**
     * Set the vote that the user gave.
     *
     * @param vote the number of selected stars ranging from 1 to 5
     */
    void setVote(int vote);

    /**
     * Store the rating information.
     *
     * @throws RatingsException when an error occurs while saving this average rating.
     */
    void save() throws RatingsException;

    /**
     * Remove the rating.
     * 
     * @return the status of the action
     * @throws RatingsException when an error occurs while removing this average rating
     */
    boolean remove() throws RatingsException;

    /**
     * The string representation of the vote.
     *
     * @return the string representation of the vote
     */
    String toString();
}
