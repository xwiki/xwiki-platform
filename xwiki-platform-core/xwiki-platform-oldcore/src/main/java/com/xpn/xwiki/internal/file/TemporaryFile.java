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
package com.xpn.xwiki.internal.file;

import java.io.File;

/**
 * Helper to create file that are automatically deleted when not needed anymore.
 * 
 * @version $Id$
 * @since 9.0RC1
 */
public class TemporaryFile extends File
{
    /**
     * @param file the file to copy
     * @since 9.10RC1
     */
    public TemporaryFile(File file)
    {
        super(file.getPath());
    }

    /**
     * @param parent The parent abstract pathname
     * @param child The child pathname string
     */
    public TemporaryFile(File parent, String child)
    {
        super(parent, child);
    }

    @Override
    protected void finalize() throws Throwable
    {
        if (exists()) {
            delete();
        }

        super.finalize();
    }
}
