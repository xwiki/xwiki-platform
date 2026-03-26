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
package org.xwiki.store;

import org.xwiki.stability.Unstable;
import org.xwiki.store.blob.Blob;

/**
 * An entity that serializes embedded data to a {@link Blob}.
 *
 * @version $Id$
 * @since 17.10.0RC1
 */
@Unstable
public interface BlobSerializer
{
    /**
     * @param blob the file to write to
     * @throws Exception when failing to serialize the data to the passed blob
     */
    void serialize(Blob blob) throws Exception;
}
