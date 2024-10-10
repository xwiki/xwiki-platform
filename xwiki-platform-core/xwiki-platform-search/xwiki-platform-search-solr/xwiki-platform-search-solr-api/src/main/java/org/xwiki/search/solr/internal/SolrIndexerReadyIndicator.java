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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import org.xwiki.store.ReadyIndicator;

/**
 * An implementation of the {@link ReadyIndicator} for the {@link DefaultSolrIndexer}.
 *
 * @since 16.9.0RC1
 * @since 15.10.13
 * @since 16.4.5
 * @version $Id$
 */
class SolrIndexerReadyIndicator extends CompletableFuture<Void> implements ReadyIndicator
{
    private final long initialResolveQueueCounter;

    private final long initialResolveQueueSize;

    private final AtomicLong initialIndexQueueCounter;

    private final AtomicLong initialIndexQueueSize;

    private final LongSupplier resolveQueueCounter;

    private final LongSupplier indexQueueCounter;

    private final IntSupplier indexQueueSize;

    private final int resolveQueuePercentage;

    /**
     * Constructor, initializes the ready indicator to provide progress based on the resolve queue.
     *
     * @param resolveQueueCounter supplies the value of the counter that counts removal operations on the resolve queue
     * @param resolveQueueSize supplies the size of the resolve queue
     * @param indexQueueCounter supplies the value of the counter that counts removal operations on the indexer queue
     * @param indexQueueSize supplies the size of the indexer queue
     * @param indexQueueLimit supplies the size limit of the indexer queue
     */
    SolrIndexerReadyIndicator(LongSupplier resolveQueueCounter, IntSupplier resolveQueueSize,
        LongSupplier indexQueueCounter, IntSupplier indexQueueSize, IntSupplier indexQueueLimit)
    {
        this.resolveQueueCounter = resolveQueueCounter;
        this.indexQueueCounter = indexQueueCounter;
        this.indexQueueSize = indexQueueSize;

        int indexQueueSizeValue = indexQueueSize.getAsInt();
        this.initialResolveQueueCounter = resolveQueueCounter.getAsLong();
        this.initialResolveQueueSize = Math.max(resolveQueueSize.getAsInt(), 1);
        this.initialIndexQueueCounter = new AtomicLong(-1);
        this.initialIndexQueueSize = new AtomicLong(-1);

        // If the index queue is almost full or the resolve queue is non-empty, give resolving 50% of the share.
        if (indexQueueSizeValue >= 0.9 * indexQueueLimit.getAsInt() || this.initialResolveQueueSize > 1) {
            this.resolveQueuePercentage = 50;
        } else {
            // Resolving should be instant, so give it just 10% of the share.
            this.resolveQueuePercentage = 10;
        }
    }

    /**
     * Mark the ready indicator as being part of the indexer queue.
     */
    void switchToIndexQueue()
    {
        this.initialIndexQueueCounter.set(this.indexQueueCounter.getAsLong());
        this.initialIndexQueueSize.set(Math.max(this.indexQueueSize.getAsInt(), 1));
    }

    @Override
    public int getProgressPercentage()
    {
        long initialIndexQueueSizeValue = this.initialIndexQueueSize.get();

        if (initialIndexQueueSizeValue < 0) {
            long currentResolveQueueCounterValue = this.resolveQueueCounter.getAsLong();
            long removedElements = currentResolveQueueCounterValue - this.initialResolveQueueCounter;
            return Math.min(this.resolveQueuePercentage,
                (int) (removedElements * this.resolveQueuePercentage / this.initialResolveQueueSize));
        } else {
            long currentIndexQueueCounterValue = this.indexQueueCounter.getAsLong();
            long removedElements = currentIndexQueueCounterValue - this.initialIndexQueueCounter.get();
            // Never report 100%, full completion should only be set when the marker is removed from the queue.
            return Math.min(99,
                this.resolveQueuePercentage + (int) (removedElements * (100 - this.resolveQueuePercentage)
                    / initialIndexQueueSizeValue));
        }
    }
}
