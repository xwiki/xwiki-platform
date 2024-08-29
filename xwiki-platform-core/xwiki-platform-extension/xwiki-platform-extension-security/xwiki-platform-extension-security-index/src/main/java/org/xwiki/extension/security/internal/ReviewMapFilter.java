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
package org.xwiki.extension.security.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.index.security.review.Review;
import org.xwiki.extension.index.security.review.ReviewsMap;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.security.analyzer.ReviewsFetcher;

/**
 * Provides the operations to filter the review map returned by {@link ReviewsFetcher#fetch()} and filter it to only
 * keep reviews relevant to the current installation.
 *
 * @version $Id$
 * @since 15.6RC1
 */
@Component(roles = ReviewMapFilter.class)
@Singleton
public class ReviewMapFilter
{
    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    /**
     * @param reviewsMap the review map to filter
     * @return the filtered review map
     */
    public ReviewsMap filter(ReviewsMap reviewsMap)
    {
        Collection<InstalledExtension> installedExtensions = this.installedExtensionRepository.getInstalledExtensions();
        Collection<CoreExtension> coreExtensions = this.coreExtensionRepository.getCoreExtensions();
        ArrayList<Extension> extensions = new ArrayList<>();
        extensions.addAll(installedExtensions);
        extensions.addAll(coreExtensions);

        Map<String, List<Review>> map = reviewsMap.getReviewsMap();
        Map<String, List<Review>> filteredMap = new HashMap<>();
        for (Map.Entry<String, List<Review>> stringListEntry : map.entrySet()) {
            filteredMap.put(stringListEntry.getKey(), stringListEntry.getValue().stream().filter(review -> {
                boolean match = true;
                if (review.getFilter() != null) {
                    Pattern pattern = Pattern.compile(review.getFilter());
                    // If a filter exists, we look for an extension id matching it. 
                    match = extensions.stream()
                        .map(it -> it.getId().toString())
                        .anyMatch(it -> pattern.matcher(it).matches());
                }
                return match;
            }).collect(Collectors.toList()));
        }

        // Replace the content with the filtered content.
        reviewsMap.getReviewsMap().clear();
        reviewsMap.getReviewsMap().putAll(filteredMap);
        return reviewsMap;
    }
}
