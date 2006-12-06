/*
 * 
 * ===================================================================
 *
 * Copyright (c) 2005 Jens Krämer, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created on 24.01.2005
 *
 */

package com.xpn.xwiki.plugin.lucene;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @author <a href="mailto:jk@jkraemer.net">Jens Krämer </a>
 */
public class XWikiDocumentQueue
{
    /** maps names of documents to the document instances itself */
    private Map    documentsByName = new HashMap ();
    /** maintains fifo order */
    private Buffer namesQueue      = new UnboundedFifoBuffer ();

    public synchronized IndexData remove ()
    {
        return (IndexData) documentsByName.remove (namesQueue.remove ());
    }

    public synchronized void add (IndexData data)
    {
        final String key = data.toString ();
        if (!documentsByName.containsKey (key))
        {
            // document with this name not yet in Queue, so add it
            namesQueue.add (key);
        }
        // in any case put new version of this document in the map, overwriting
        // possibly existing older version
        documentsByName.put (key, data);
    }

    public synchronized boolean isEmpty ()
    {
        return namesQueue.isEmpty ();
    }

    public long getSize() {
        return namesQueue.size();
    }
}
