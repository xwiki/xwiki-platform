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
package org.xwiki.ratings.internal;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.RatingsConfiguration;

/**
 * Default configuration for Ratings.
 * By default the configuration considers a scale of 5, doesn't use a dedicated core, stores the zero values and the
 * average. And it uses the solr manager.
 * FIXME: Change this to be based on a RatingConfiguration document.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Singleton
public class DefaultRatingsConfiguration implements RatingsConfiguration
{
    @Inject
    @Named("ratings")
    private ConfigurationSource configurationSource;

    @Override
    public boolean isZeroStored()
    {
        return this.configurationSource.getProperty("zeroStored", true);
    }

    @Override
    public int getScaleUpperBound()
    {
        return this.configurationSource.getProperty("scaleUpperBound", 5);
    }

    @Override
    public boolean hasDedicatedCore()
    {
        return this.configurationSource.getProperty("dedicatedCore", false);
    }

    @Override
    public boolean isAverageStored()
    {
        return this.configurationSource.getProperty("averageStored", true);
    }

    @Override
    public String getRatingsStorageHint()
    {
        return this.configurationSource.getProperty("ratingsStorageHint", "solr");
    }

    @Override
    public String getAverageRatingStorageHint()
    {
        return this.configurationSource.getProperty("averageRatingStorageHint", "xobject");
    }

    @Override
    public Set<EntityReference> getExcludedReferencesFromRatings()
    {
        return this.configurationSource.getProperty("excludedReferences", new HashSet<>());
    }

    @Override
    public boolean isEnabled()
    {
        return this.configurationSource.getProperty("enabled", true);
    }
}
