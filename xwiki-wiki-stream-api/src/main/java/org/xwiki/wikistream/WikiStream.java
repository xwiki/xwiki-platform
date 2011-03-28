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
package org.xwiki.wikistream;

import org.xwiki.wikistream.descriptor.WikiStreamDescriptor;
import org.xwiki.wikistream.listener.Listener;
import org.xwiki.wikistream.type.WikiStreamType;
import org.xwiki.wikistream.type.WikiType;

/**
 * WikiStream parses and generates listener events for various wiki types.
 * 
 * @version $Id$
 */
public interface WikiStream<P>
{
    /**
     * @return The WikiStream type.
     */
    WikiStreamType getType();

    /**
     * @return The WikiStreamDescriptor describes a WikiStream and has the list of bean class parameters required for the WikiStream.convert() method.
     */
    WikiStreamDescriptor getDescriptor();

    /**
     * This uses default {@link Listener} implemented for the given {@link WikiType} type.
     * 
     * @param parameters The list of parameters required for the stream parser.
     * @throws WikiStreamException
     */
    void convert(P parameters) throws WikiStreamException;
    
    /**
     * @param parameters The list of parameters required for the stream parser.
     * @param listener The listener has the list of events which are called by the parser.
     * @throws WikiStreamException
     */
    void convert(P parameters, Listener listener) throws WikiStreamException;
}
