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
package org.xwiki.model.internal.document;

import java.util.Set;

import org.xwiki.model.document.RequiredRights;
import org.xwiki.security.authorization.Right;

import static org.xwiki.script.internal.safe.AbstractSafeObject.FORBIDDEN;

/**
 * A safe (i.e., read-only) version of the required rights, where update operations are disabled. The read and right
 * version is {@link DefaultRequiredRights}.
 *
 * @version $Id$
 * @since 15.3RC1
 */
public class SafeRequiredRights implements RequiredRights
{
    private final RequiredRights requiredRights;

    /**
     * @param requiredRights the wrapped required rights
     */
    public SafeRequiredRights(RequiredRights requiredRights)
    {
        this.requiredRights = requiredRights;
    }

    @Override
    public Set<Right> getRights()
    {
        return this.requiredRights.getRights();
    }

    @Override
    public void setRights(Set<Right> newRights)
    {
        throw new UnsupportedOperationException(FORBIDDEN);
    }

    @Override
    public boolean has(Right right)
    {
        return this.requiredRights.has(right);
    }

    @Override
    public boolean activated()
    {
        return this.requiredRights.activated();
    }
}
