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

import java.sql.Connection;

import org.hibernate.SharedSessionContract;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.xwiki.store.hibernate.internal.patcher.Mixin;

/**
 * Hibernate 5 {@link SharedSessionContract} APIs restored in the patched Hibernate.
 * <p>
 * Restored on the parent of both {@link org.hibernate.Session} and {@link org.hibernate.StatelessSession} so that the
 * session implementations inherit a single implementation.
 *
 * @version $Id$
 */
@Mixin
public interface SharedSessionContractMixin extends SharedSessionContract
{
    /**
     * @return the JDBC connection used by the session, which should not be closed (Hibernate 5 exposed it on
     *         {@link org.hibernate.StatelessSession} and on the session implementations)
     * @deprecated use {@link #doWork(org.hibernate.jdbc.Work)} or
     *             {@link #doReturningWork(org.hibernate.jdbc.ReturningWork)} instead
     */
    @Deprecated
    default Connection connection()
    {
        return ((SharedSessionContractImplementor) this).getJdbcCoordinator().getLogicalConnection()
            .getPhysicalConnection();
    }
}
