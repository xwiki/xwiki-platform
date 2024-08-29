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

package org.xwiki.security.authorization.testwikis;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;

/**
 * Public interface of test entities representing an access rule.
 *
 * @version $Id$
 * @since 5.0M2
 */
public interface TestAccessRule extends TestEntity
{
    /**
     * @return a reference to the user/group concerned by this access rule entity.
     */
    DocumentReference getUser();

    /**
     * @return the right defined by this access rule entity.
     */
    Right getRight();

    /**
     * @return the state defined by this access rule entity.
     */
    RuleState getState();

    /**
     * @return true if this is a user rule and false for a group rule.
     */
    boolean isUser();
}
