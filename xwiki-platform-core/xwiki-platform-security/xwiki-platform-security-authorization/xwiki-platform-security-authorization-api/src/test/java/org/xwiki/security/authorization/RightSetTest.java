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
import java.util.Collection;
import java.util.Set;

import org.apache.commons.collections4.collection.AbstractCollectionTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test Set interface of RightSet.
 *
 * @version $Id$
 * @since 4.0M2
 */
// AbstractCollectionTest rather than AbstractSetTest: the set-specific tests of the latter feed a String to the
// collection under test, which a RightSet cannot hold, and they are package private and therefore not overridable from
// here. The two checks they bring are reproduced below, over Right elements.
class RightSetTest extends AbstractCollectionTest<Right>
{
    @Override
    public RightSet makeObject()
    {
        return new RightSet();
    }

    @Override
    public Collection<Right> makeConfirmedCollection()
    {
        return new RightSet();
    }

    @Override
    public Collection<Right> makeConfirmedFullCollection()
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

    @Override
    public boolean isEqualsCheckable()
    {
        return true;
    }

    /**
     * Adds to the collection level verifications the set specific ones: the two collections are equal, they have the
     * same hash code, and the iterator returns each element only once.
     */
    @Override
    public void verify()
    {
        super.verify();

        assertEquals(getConfirmed(), getCollection(), "Sets should be equal");
        assertEquals(getConfirmed().hashCode(), getCollection().hashCode(), "Sets should have equal hashCodes");
        Collection<Right> set = makeConfirmedCollection();
        for (Right element : getCollection()) {
            assertTrue(set.add(element), "Set.iterator should only return unique elements");
        }
    }

    /**
     * Tests {@link Set#equals(Object)}.
     */
    @Test
    // This method verifies the equals() contract itself, so the assertions deliberately call equals()
    // explicitly: the boolean form is what makes visible which set is the receiver and which argument it
    // gets. Using assertNotEquals() would move that into JUnit's internals and would invite a later
    // SonarQube S3415 "swap these arguments" change that silently stops testing the contract.
    @SuppressWarnings("java:S5785")
    void setEquals()
    {
        resetEmpty();
        assertEquals(getCollection(), getConfirmed(), "Empty sets should be equal");
        verify();

        Collection<Right> set2 = makeConfirmedCollection();
        set2.add(Right.VIEW);
        assertFalse(getCollection().equals(set2), "Empty set shouldn't equal nonempty set");

        resetFull();
        assertEquals(getCollection(), getConfirmed(), "Full sets should be equal");
        verify();

        set2.clear();
        set2.addAll(Arrays.asList(getOtherElements()));
        assertFalse(getCollection().equals(set2), "Sets with different contents shouldn't be equal");
    }

    /**
     * Tests {@link Set#hashCode()}.
     */
    @Test
    void setHashCode()
    {
        resetEmpty();
        assertEquals(getCollection().hashCode(), getConfirmed().hashCode(), "Empty sets have equal hashCodes");

        resetFull();
        assertEquals(getCollection().hashCode(), getConfirmed().hashCode(), "Equal sets have equal hashCodes");
    }
}
