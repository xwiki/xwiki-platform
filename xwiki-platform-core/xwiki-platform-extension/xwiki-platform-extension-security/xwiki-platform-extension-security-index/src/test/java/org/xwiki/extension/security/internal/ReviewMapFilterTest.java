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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.index.security.review.Review;
import org.xwiki.extension.index.security.review.ReviewsMap;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link ReviewMapFilter}.
 *
 * @version $Id$
 * @since 15.6RC1
 */
@ComponentTest
class ReviewMapFilterTest
{
    @InjectMockComponents
    private ReviewMapFilter filter;

    @MockComponent
    private InstalledExtensionRepository installedExtensionRepository;

    @MockComponent
    private CoreExtensionRepository coreExtensionRepository;

    @Test
    void filterEmpty()
    {
        when(this.installedExtensionRepository.getInstalledExtensions()).thenReturn(List.of());
        when(this.coreExtensionRepository.getCoreExtensions()).thenReturn(List.of());

        assertEquals(new ReviewsMap(), this.filter.filter(new ReviewsMap()));
    }

    @Test
    void filter()
    {
        InstalledExtension installedExtension = mock(InstalledExtension.class);
        when(installedExtension.getId()).thenReturn(new ExtensionId("group:artifact", "1.4"));
        when(this.installedExtensionRepository.getInstalledExtensions()).thenReturn(List.of(
            installedExtension
        ));
        when(this.coreExtensionRepository.getCoreExtensions()).thenReturn(List.of());

        ReviewsMap reviewsMap = new ReviewsMap();
        Review r0 = new Review();
        Review r1 = new Review();
        r1.setFilter("o.*");
        Review r2 = new Review();
        r2.setFilter(".*artifact/1.4");
        reviewsMap.getReviewsMap().put("a", List.of(
            r0,
            r1,
            r2
        ));
        ReviewsMap expected = new ReviewsMap();
        expected.getReviewsMap().put("a", List.of(
            r0,
            r2
        ));
        assertEquals(expected, this.filter.filter(reviewsMap));
    }
}
