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
package org.xwiki.store.legacy.store.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.store.internal.FileSystemStoreUtils;

import com.xpn.xwiki.store.AttachmentRecycleBinContentStore;
import com.xpn.xwiki.store.AttachmentRecycleBinStore;
import com.xpn.xwiki.store.hibernate.HibernateAttachmentRecycleBinStore;

/**
 * Realization of {@link AttachmentRecycleBinStore} for filesystem storage.
 *
 * @version $Id$
 * @since 3.0M3
 * @deprecated since 9.10RC1, use {@link FilesystemAttachmentRecycleBinContentStore} instead
 */
@Component
@Named(FileSystemStoreUtils.HINT)
@Singleton
@Deprecated
public class FilesystemAttachmentRecycleBinStore extends HibernateAttachmentRecycleBinStore
{
    @Inject
    @Named(FileSystemStoreUtils.HINT)
    private AttachmentRecycleBinContentStore contentStore;

    @Override
    protected AttachmentRecycleBinContentStore getAttachmentRecycleBinContentStore(String storeType)
    {
        return this.contentStore;
    }
}
