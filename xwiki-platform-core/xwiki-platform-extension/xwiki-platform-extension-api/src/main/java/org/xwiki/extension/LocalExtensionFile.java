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

/**
 * Extension {@link ExtensionFile} with some more informations related local extensions.
 * 
 * @version $Id$
 */
public interface LocalExtensionFile extends ExtensionFile
{
    /**
     * Returns the name of the file. This is just the last name in the pathname's name sequence.
     * 
     * @return the name of the file
     * @see java.io.File#getName()
     */
    String getName();

    /**
     * Returns the absolute pathname string of this file.
     * 
     * @return The absolute pathname string of the file
     * @see java.io.File#getAbsolutePath()
     */
    String getAbsolutePath();
}
