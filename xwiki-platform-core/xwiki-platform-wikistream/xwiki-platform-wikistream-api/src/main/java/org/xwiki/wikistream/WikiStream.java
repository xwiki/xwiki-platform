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

import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.descriptor.WikiStreamDescriptor;
import org.xwiki.wikistream.type.WikiStreamType;

/**
 * <p>
 * WikiStream class should be inherited by all the stream based classes to implement the type and descriptor which
 * describes a wiki stream with list of bean class parameters.
 * </p>
 * 
 * @version $Id$
 * @since 5.2M2
 */
@Unstable
public interface WikiStream
{
    /**
     * @return The {@link WikiStreamType}, which identifies a wiki stream input and output components using a role hint.
     */
    WikiStreamType getType();

    /**
     * @return The WikiStreamDescriptor describes a WikiStream and has the list of bean class parameters or properties.
     */
    WikiStreamDescriptor getDescriptor();
}
