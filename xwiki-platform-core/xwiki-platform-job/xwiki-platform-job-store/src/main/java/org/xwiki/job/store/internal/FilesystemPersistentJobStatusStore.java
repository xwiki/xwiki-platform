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
package org.xwiki.job.store.internal;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.internal.DefaultPersistentJobStatusStore;
import org.xwiki.job.internal.PersistentJobStatusStore;

/**
 * Provide the filesystem-based implementation of {@link PersistentJobStatusStore} as an explicitly named component.
 *
 * <p>The database-backed job status store {@link DatabaseJobStatusStore} overrides the default implementation. To
 * be able to still inject the filesystem-backed implementation, we need to explicitly name it.</p>
 * @version $Id$
 * @since 18.1.0RC1
 */
@Component
@Named("filesystem")
@Singleton
public class FilesystemPersistentJobStatusStore extends DefaultPersistentJobStatusStore
{
}
