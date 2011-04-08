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
package com.xpn.xwiki.plugin.lucene;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUnderflowException;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;

/**
 * This class represents a Queue (FirstInFirstOut) for XWikiDocument objects. It is used during indexing of the wiki.
 * The index is updated whenever the processing queue is not empty. This class is threadsafe, as usually several threads
 * add elements and one thread removes them for processing.
 * 
 * @version $Id$
 */
public class XWikiDocumentQueue
{
    /**
     * Maps names of documents to the document instances.
     */
    private Map<String, AbstractIndexData> documentsByName = new HashMap<String, AbstractIndexData>();

    /**
     * Maintains FIFO order.
     */
    private Buffer namesQueue = new UnboundedFifoBuffer();

    /**
     * Remove an item from the queue and return it. Since this is a FIFO, the element returned will be the oldes one in
     * the queue.
     * 
     * @return The oldest element in the queue.
     * @throws BufferUnderflowException If the queue is empty.
     */
    public synchronized AbstractIndexData remove() throws BufferUnderflowException
    {
        return this.documentsByName.remove(this.namesQueue.remove());
    }

    /**
     * Adds an item to the queue. Since this is a FIFO, it will be removed after all the other items already in the
     * queue have been processed. If the element was already in the queue, it will not be added again (as a duplicate),
     * its position will remain unchanged, but the associated data is updated, so the new version will replace the old
     * unprocessed one.
     * 
     * @param data IndexData object to add to the queue.
     */
    @SuppressWarnings("unchecked")
    public synchronized void add(AbstractIndexData data)
    {
        String key = data.getId();

        if (!this.documentsByName.containsKey(key)) {
            // Document with this name not yet in the Queue, so add it
            this.namesQueue.add(key);
        }

        // In any case put new version of this document in the map, overwriting
        // possibly existing older version
        this.documentsByName.put(key, data);
    }

    /**
     * Check if the queue is empty or not.
     * 
     * @return <code>true</code> if the queue is empty, <code>false</code> otherwise.
     */
    public synchronized boolean isEmpty()
    {
        return this.namesQueue.isEmpty();
    }

    /**
     * Returns the number of elements in the queue.
     * 
     * @return Number of elements in the queue.
     */
    public synchronized int getSize()
    {
        return this.namesQueue.size();
    }
}
