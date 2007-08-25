/*
 * Copyright 2005-2007, XpertNet SARL, and individual contributors as
 * indicated by the contributors.txt.
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

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a Queue (FirstInFirstOut) for XWikiDocument objects. It is used during
 * indexing of the wiki. The rebuilding of the index is done, until the processing queue is empty.
 * 
 * @version $Id: $
 */
public class XWikiDocumentQueue
{
    /**
     * maps names of documents to the document instances itself
     */
    private Map documentsByName = new HashMap();

    /**
     * maintains fifo order
     */
    private Buffer namesQueue = new UnboundedFifoBuffer();

    /**
     * @return remove an item from our queue and return it.
     */
    public synchronized IndexData remove()
    {
        return (IndexData) documentsByName.remove(namesQueue.remove());
    }

    /**
     * @param data IndexData object to add to our queue
     */
    public synchronized void add(IndexData data)
    {
        final String key = data.getId();
        if (!documentsByName.containsKey(key)) {
            // document with this name not yet in Queue, so add it
            namesQueue.add(key);
        }
        // in any case put new version of this document in the map, overwriting
        // possibly existing older version
        documentsByName.put(key, data);
    }

    /**
     * @return true if our queue is empty.
     */
    public synchronized boolean isEmpty()
    {
        return namesQueue.isEmpty();
    }

    /**
     * @return number of elements in our queue.
     */
    public long getSize()
    {
        return namesQueue.size();
    }
}
