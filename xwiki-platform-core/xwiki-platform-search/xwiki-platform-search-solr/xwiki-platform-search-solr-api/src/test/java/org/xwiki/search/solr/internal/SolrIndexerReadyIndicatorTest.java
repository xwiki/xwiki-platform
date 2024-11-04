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
package org.xwiki.search.solr.internal;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for {@link SolrIndexerReadyIndicator}.
 *
 * @version $Id$
 */
class SolrIndexerReadyIndicatorTest
{
    @ParameterizedTest
    @CsvSource({
        "10, 1, 10000, 10, 10000",
        "10, 0, 42, 0, 42",
        "0, 0, 0, 0, 42",
        "100, 0, 100, 10, 10",
        "100, 100, 100, 10, 10"
    })
    void initialPercentage(long resolveQueueCounter, int resolveQueueSize, long indexQueueCounter, int indexQueueSize,
        int indexQueueLimit)
    {
        int actual = new SolrIndexerReadyIndicator(() -> resolveQueueCounter, () -> resolveQueueSize,
            () -> indexQueueCounter, () -> indexQueueSize, () -> indexQueueLimit).getProgressPercentage();
        assertEquals(0, actual);
    }

    @ParameterizedTest
    @CsvSource({
        "10, 1, 10000, 10, 10000, 11, 10",
        "10, 20, 1000, 10, 10000, 12, 5",
        "10, 0, 10000, 10, 10000, 12, 10",
        "1, 0, 10000, 100, 100, 2, 50"
    })
    void resolveQueuePercentage(long resolveQueueCounter, int resolveQueueSize, long indexQueueCounter, int indexQueueSize,
        int indexQueueLimit, long updatedResolveQueueCounter, int expected)
    {
        AtomicLong resolveQueueCount = new AtomicLong(resolveQueueCounter);
        SolrIndexerReadyIndicator readyIndicator = new SolrIndexerReadyIndicator(resolveQueueCount::getAcquire,
            () -> resolveQueueSize, () -> indexQueueCounter, () -> indexQueueSize, () -> indexQueueLimit);
        resolveQueueCount.set(updatedResolveQueueCounter);
        assertEquals(expected, readyIndicator.getProgressPercentage());
    }

    @ParameterizedTest
    @CsvSource({
        "10, 1, 20, 10, 10000, 20, 10",
        "10, 1, 20, 10, 10000, 21, 19",
        "10, 1, 20, 10, 10000, 30, 99",
    })
    void indexQueuePercentage(long resolveQueueCounter, int resolveQueueSize, long indexQueueCounter, int indexQueueSize,
        int indexQueueLimit, long updatedIndexQueueCounter, int expected)
    {
        AtomicLong indexQueueCount = new AtomicLong();
        SolrIndexerReadyIndicator readyIndicator = new SolrIndexerReadyIndicator(() -> resolveQueueCounter,
            () -> resolveQueueSize, indexQueueCount::getAcquire, () -> indexQueueSize, () -> indexQueueLimit);
        indexQueueCount.set(indexQueueCounter);
        readyIndicator.switchToIndexQueue();
        indexQueueCount.set(updatedIndexQueueCounter);
        assertEquals(expected, readyIndicator.getProgressPercentage());
    }
}
