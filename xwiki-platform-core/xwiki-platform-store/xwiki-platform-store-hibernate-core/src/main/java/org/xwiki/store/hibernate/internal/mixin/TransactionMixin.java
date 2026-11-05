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
package org.xwiki.store.hibernate.internal.mixin;

import org.hibernate.Transaction;
import org.xwiki.store.hibernate.internal.patcher.BinaryOnly;
import org.xwiki.store.hibernate.internal.patcher.Mixin;

/**
 * Hibernate 5 {@link Transaction} APIs restored in the patched Hibernate.
 *
 * @version $Id$
 */
@Mixin
public interface TransactionMixin extends Transaction
{
    /**
     * Hibernate 5 {@code Transaction#getTimeout()} returned an {@code int}.
     *
     * @return the timeout, -1 if none is set
     */
    @BinaryOnly("getTimeout")
    default int getIntTimeout()
    {
        Integer timeout = getTimeout();

        return timeout != null ? timeout : -1;
    }
}
