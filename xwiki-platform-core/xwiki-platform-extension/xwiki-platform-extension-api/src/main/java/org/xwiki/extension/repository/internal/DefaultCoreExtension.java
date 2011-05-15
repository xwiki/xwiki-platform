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

/**
 * Default implementation of {@link CoreExtension}.
 * 
 * @version $Id$
 */
public class DefaultCoreExtension extends AbstractExtension implements CoreExtension
{
    /**
     * @param repository the core extension repository
     * @param url the core extension URL
     * @param id the id/version combination which makes the extension unique
     * @param type the type of the extension
     */
    public DefaultCoreExtension(DefaultCoreExtensionRepository repository, URL url, ExtensionId id, String type)
    {
        super(repository, id, type);

        putProperty(PKEY_URL, url);
    }

    // Extension

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.Extension#download(java.io.File)
     */
    public void download(File file) throws ExtensionException
    {
        // TODO
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.AbstractExtension#setId(org.xwiki.extension.ExtensionId)
     */
    public void setId(ExtensionId id)
    {
        super.setId(id);
    }

    // CoreExtension

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.CoreExtension#getURL()
     */
    public URL getURL()
    {
        return getProperty(PKEY_URL, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.CoreExtension#isGuessed()
     */
    public boolean isGuessed()
    {
        return getProperty(PKEY_GUESSED, false);
    }

    /**
     * @param guessed true if the extension is "guessed"
     */
    public void setGuessed(boolean guessed)
    {
        putProperty(PKEY_GUESSED, guessed);
    }

    // Object

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.AbstractExtension#toString()
     */
    @Override
    public String toString()
    {
        return getId().toString();
    }
}
