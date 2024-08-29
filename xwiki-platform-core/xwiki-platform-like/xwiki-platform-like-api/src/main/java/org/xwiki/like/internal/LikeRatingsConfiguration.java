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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
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

    // Note that we only use the xwiki.properties configuration for now, since we're not yet sure we want to expose
    // those configurations to end users. It might change in the future to a composite configuration source.
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    /**
     * {@inheritDoc}
     *
     * We specifically don't store votes that are equals to zero for likes, since it doesn't really make sense:
     * we want to store Likes (i.e. votes equals to 1). Moreover it allows us to rely properly on count method to
     * count likes (i.e. we'll never count votes equals to zero).
     *
     * @return {@code false}
     */
    @Override
    public boolean isZeroStored()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * The upper-bound scale is 1 for Likes and since we don't store 0 we actually only store votes of value 1.
     *
     * @return {@code 1}
     */
    @Override
    public int getScaleUpperBound()
    {
        return 1;
    }

    /**
     * {@inheritDoc}
     *
     * We allow to store Likes information along with other Ratings: there's no real necessity to have another Solr
     * core for Likes.
     *
     * @return {@code false}
     */
    @Override
    public boolean hasDedicatedCore()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * We don't store average ratings for Likes: the average score would be always 1 since we only store values of 1.
     *
     * @return {@code false}
     */
    @Override
    public boolean isAverageStored()
    {
        return this.configurationSource.getProperty("like.averagerating.isStored", false);
    }

    /**
     * {@inheritDoc}
     *
     * We store Ratings in the Solr core for Likes.
     *
     * @return {@code solr}.
     */
    @Override
    public String getRatingsStorageHint()
    {
        return "solr";
    }

    /**
     * {@inheritDoc}
     *
     * Since we don't store average ratings this value should never be used.
     *
     * @return {@code null}.
     */
    @Override
    public String getAverageRatingStorageHint()
    {
        return this.configurationSource.getProperty("like.averagerating.hint", "xobject");
    }

    /**
     * {@inheritDoc}
     *
     * By default we don't exclude any references to be rated.
     *
     * @return an empty set.
     */
    @Override
    public Set<EntityReference> getExcludedReferencesFromRatings()
    {
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     *
     * By default this is enabled for Likes.
     *
     * @return {@code true}.
     */
    @Override
    public boolean isEnabled()
    {
        return true;
    }
}
