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
package org.xwiki.extension;

import java.io.IOException;
import java.io.InputStream;

/**
 * Contains the actual file of the extensions and some informations like the size.
 * 
 * @version $Id$
 */
public interface ExtensionFile
{
    /**
     * @return the size of the extension file, -1 if not provided
     */
    long getLength();

    /**
     * Open an input stream to allows reading the extensions.
     * <p>
     * Note that it's up to the user of this method to close the stream.
     * 
     * @return the stream to read
     * @throws IOException error when opening the stream
     */
    InputStream openStream() throws IOException;
}
