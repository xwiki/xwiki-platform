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

import org.apache.commons.collections4.set.AbstractSetTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Test Set interface of RightSet.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class RightSetTest extends AbstractSetTest<Right>
{
    @Override
    public RightSet makeObject()
    {
        return new RightSet();
    }

    @Override
    public RightSet makeConfirmedCollection()
    {
        return new RightSet();
    }

    @Override
    public Right[] getFullNonNullElements()
    {
        return new Right[] {Right.VIEW, Right.EDIT, Right.DELETE, Right.COMMENT, Right.ADMIN};
    }

    @Override
    public Right[] getOtherNonNullElements()
    {
        return new Right[] {Right.CREATE_WIKI, Right.CREATOR, Right.ILLEGAL};
    }

    @Override
    public boolean isNullSupported()
    {
        return false;
    }

    @Override
    protected boolean skipSerializedCanonicalTests()
    {
        return true;
    }

    // Methods we need to override because AbstractSetTest use Strings to validate the Set

    /**
     * Tests {@link Set#equals(Object)}.
     */
    @Test
    @Override
    public void testSetEquals()
    {
        resetEmpty();
        assertEquals(getCollection(), getConfirmed(), "Empty sets should be equal");
        verify();

        final RightSet set2 = makeConfirmedCollection();
        // CUSTOM: the standard #testSetEquals add a String here, which does not make any sense for RightSet
        set2.add(Right.VIEW);
        assertFalse(getCollection().equals(set2), "Empty set shouldn't equal nonempty set");

        resetFull();
        assertEquals(getCollection(), getConfirmed(), "Full sets should be equal");
        verify();

        set2.clear();
        set2.addAll(Arrays.asList(getOtherElements()));
        assertFalse(getCollection().equals(set2), "Sets with different contents shouldn't be equal");
    }
}
