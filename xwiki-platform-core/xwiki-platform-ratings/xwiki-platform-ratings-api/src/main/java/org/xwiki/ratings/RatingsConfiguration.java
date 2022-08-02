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

import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;

/**
 * Configuration of {@link RatingsManager}.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Role
public interface RatingsConfiguration
{
    /**
     * @return {@code false} if a rating set to 0 shoud lead to the deletion of a previously made vote.
     *         {@code true} means that all ratings noted to 0 are stored. Note that this option will impact the
     *         average rating.
     */
    boolean isZeroStored();

    /**
     * @return the upper bound of the scale. The lower bound is always 0.
     */
    int getScaleUpperBound();

    /**
     * @return {@code true} to create - if possible, depending on the storage implementation - a dedicated store.
     *         {@code false} means that all ratings from any applications will be stored on the same storage.
     */
    boolean hasDedicatedCore();

    /**
     * @return {@code true} to store the average rating. {@code false} means that the average rating is never stored
     * nor computed.
     */
    boolean isAverageStored();

    /**
     * @return the storage hint to be used for Ratings data (e.g. solr).
     */
    String getRatingsStorageHint();

    /**
     * @return the storage hint to be used for Average Ratings data (e.g. solr or xobject)
     */
    String getAverageRatingStorageHint();

    /**
     * Allow to define a set of references that cannot be rated.
     * This list is mainly used in the UI and in the script service. Note that the provided references are actually
     * also excluding their children, so excluding a wiki reference will excludes all elements from this wiki to be
     * rated.
     *
     * @return the set of references to be excluded from ratings.
     */
    Set<EntityReference> getExcludedReferencesFromRatings();

    /**
     * @return {@code true} if the ratings is enabled. This configuration is only used to control the UI.
     */
    boolean isEnabled();
}
