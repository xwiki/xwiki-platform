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

import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.SecurityAccess;

/**
 * Stub implementation for usage in the test code.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class TestSecurityAccessEntry extends AbstractSecurityAccessEntry
{
    private final UserSecurityReference userReference;
    private final SecurityReference reference;
    private final SecurityAccess securityAccess;

    public TestSecurityAccessEntry(UserSecurityReference user, SecurityReference reference, SecurityAccess securityAccess)
    {
        this.userReference = user;
        this.reference = reference;
        this.securityAccess = securityAccess;
    }

    @Override
    public UserSecurityReference getUserReference()
    {
        return this.userReference;
    }

    @Override
    public SecurityAccess getAccess()
    {
        return this.securityAccess;
    }

    @Override
    public SecurityReference getReference()
    {
        return this.reference;
    }
}
