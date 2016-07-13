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
package org.xwiki.vfs.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.url.ExtendedURL;
import org.xwiki.vfs.VfsException;
import org.xwiki.vfs.VfsManager;
import org.xwiki.vfs.VfsResourceReference;

/**
 * Default implementation of the {@link VfsManager} API.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Singleton
public class DefaultVfsManager implements VfsManager
{
    @Inject
    private ResourceReferenceSerializer<VfsResourceReference, ExtendedURL> serializer;

    @Override
    public String getURL(VfsResourceReference reference) throws VfsException
    {
        try {
            return this.serializer.serialize(reference).toString();
        } catch (Exception e) {
            throw new VfsException("Failed to compute URL for [%s]", e, reference);
        }
    }
}
