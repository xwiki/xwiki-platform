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
package org.xwiki.extension.repository.internal;

import java.io.File;
import java.net.URL;

import org.xwiki.extension.AbstractExtension;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;

public class DefaultCoreExtension extends AbstractExtension implements CoreExtension
{
    private URL url;

    private boolean guessed;

    public DefaultCoreExtension(DefaultCoreExtensionRepository repository, URL url, ExtensionId id, String type)
    {
        super(repository, id, type);

        this.url = url;
    }

    // Extension

    public void download(File file) throws ExtensionException
    {
        // TODO
    }

    public void setId(ExtensionId id)
    {
        super.setId(id);
    }

    // CoreExtension

    public URL getURL()
    {
        return this.url;
    }

    public boolean isGuessed()
    {
        return this.guessed;
    }

    public void setGuessed(boolean guessed)
    {
        this.guessed = guessed;
    }
    
    // Object
    
    @Override
    public String toString()
    {
        return getId().toString();
    }
}
