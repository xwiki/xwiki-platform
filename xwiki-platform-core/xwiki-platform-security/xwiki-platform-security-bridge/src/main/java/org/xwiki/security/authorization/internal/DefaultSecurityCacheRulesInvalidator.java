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
package org.xwiki.security.authorization.internal;

import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.security.authorization.cache.SecurityCacheRulesInvalidator;

/**
 * The instance of this class monitors updates and invalidates right cache entries whenever necessary.
 * 
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultSecurityCacheRulesInvalidator implements SecurityCacheRulesInvalidator
{
    /**
     * The role hint.
     */
    public static final String NAME = "org.xwiki.security.authorization.internal.DefaultSecurityCacheRulesInvalidator";

    /**
     * Fair read-write lock to suspend the delivery of cache updates while there are loads in progress.
     */
    @Inject
    @Named(NAME)
    private ReadWriteLock readWriteLock;

    @Override
    public void suspend()
    {
        readWriteLock.readLock().lock();
    }

    @Override
    public void resume()
    {
        readWriteLock.readLock().unlock();
    }
}
