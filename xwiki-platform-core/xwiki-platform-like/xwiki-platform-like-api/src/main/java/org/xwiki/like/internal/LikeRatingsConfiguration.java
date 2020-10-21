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
package org.xwiki.like.internal;

import java.util.Collections;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.RatingsConfiguration;

/**
 * Default {@link RatingsConfiguration} for Likes.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@Component
@Singleton
@Named(LikeRatingsConfiguration.RATING_MANAGER_HINT)
public class LikeRatingsConfiguration implements RatingsConfiguration
{
    /**
     * Default hint for Rating Manager.
     */
    public static final String RATING_MANAGER_HINT = "like";

    @Override
    public boolean isZeroStored()
    {
        return false;
    }

    @Override
    public int getScaleUpperBound()
    {
        return 1;
    }

    @Override
    public boolean hasDedicatedCore()
    {
        return false;
    }

    @Override
    public boolean isAverageStored()
    {
        return false;
    }

    @Override
    public String getRatingsStorageHint()
    {
        return "solr";
    }

    @Override
    public String getAverageRatingStorageHint()
    {
        return null;
    }

    @Override
    public Set<EntityReference> getExcludedReferencesFromRatings()
    {
        return Collections.emptySet();
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }
}
