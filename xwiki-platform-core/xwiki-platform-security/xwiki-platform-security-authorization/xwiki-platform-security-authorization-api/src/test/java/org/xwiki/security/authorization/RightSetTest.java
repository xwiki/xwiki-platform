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
package org.xwiki.security.authorization;

import java.util.Arrays;
import java.util.Set;

import org.apache.commons.collections4.set.AbstractTypedSetTest;

/**
 * Test Set interface of RightSet.
 *
 * @version $Id$
 * @since 4.0M2
 */
class RightSetTest extends AbstractTypedSetTest<Right>
{
    @Override
    public RightSet makeObject()
    {
        return new RightSet();
    }

    @Override
    public Set<Right> makeConfirmedCollection()
    {
        return new RightSet();
    }

    @Override
    public Set<Right> makeConfirmedFullCollection()
    {
        return new RightSet(Arrays.asList(getFullElements()));
    }

    @Override
    public Right[] getFullElements()
    {
        return new Right[] {Right.VIEW, Right.EDIT, Right.DELETE, Right.COMMENT, Right.ADMIN};
    }

    @Override
    public Right[] getOtherElements()
    {
        return new Right[] {Right.CREATE_WIKI, Right.CREATOR, Right.ILLEGAL};
    }

    @Override
    public boolean isNullSupported()
    {
        return false;
    }

    @Override
    public boolean isTestSerialization()
    {
        return false;
    }
}
