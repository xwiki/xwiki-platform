/*
 * 
 * ===================================================================
 *
 * Copyright (c) 2005 Jens Krämer, All rights reserved.
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
 *
 *
 * Created on 24.01.2005
 *
 */

package net.jkraemer.xwiki.plugins.lucene;

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

}
