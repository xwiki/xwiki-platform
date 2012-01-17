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
package org.xwiki.extension.wrap;

import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.CoreExtensionFile;

/**
 * Wrap a Core extension.
 * 
 * @param <T> the extension type
 * @version $Id$
 */
public class WrappingCoreExtension<T extends CoreExtension> extends WrappingExtension<T> implements CoreExtension
{
    /**
     * @param extension the wrapped core extension
     */
    public WrappingCoreExtension(T extension)
    {
        super(extension);
    }

    @Override
    public CoreExtensionFile getFile()
    {
        return (CoreExtensionFile) super.getFile();
    }

    @Override
    public boolean isGuessed()
    {
        return getWrapped().isGuessed();
    }
}
