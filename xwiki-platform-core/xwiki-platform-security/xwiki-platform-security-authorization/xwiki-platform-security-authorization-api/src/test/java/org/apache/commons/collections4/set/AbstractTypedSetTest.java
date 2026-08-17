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
package org.apache.commons.collections4.set;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Fix a limitation in {@link AbstractSetTest} where it uses Strings to validate the Set, which does not make any sense
 * for a typed Set.
 * 
 * @version $Id$
 * @since 18.7.0RC1
 */
// TODO: remove when https://issues.apache.org/jira/browse/COLLECTIONS-898 is fixed
public abstract class AbstractTypedSetTest<T> extends AbstractSetTest<T>
{
    /**
     * Tests {@link Set#equals(Object)}.
     */
    @Test
    @Override
    // This method verifies the equals() contract itself, so the assertions deliberately call equals()
    // explicitly: the boolean form is what makes visible which set is the receiver and which argument it
    // gets. Using assertNotEquals() would move that into JUnit's internals and would invite a later
    // SonarQube S3415 "swap these arguments" change that silently stops testing the contract.
    @SuppressWarnings("java:S5785")
    public void testSetEquals()
    {
        resetEmpty();
        assertEquals(getCollection(), getConfirmed(), "Empty sets should be equal");
        verify();

        final Collection<T> set2 = makeConfirmedCollection();
        set2.add(getFullElements()[0]);
        assertFalse(getCollection().equals(set2), "Empty set shouldn't equal nonempty set");

        resetFull();
        assertEquals(getCollection(), getConfirmed(), "Full sets should be equal");
        verify();

        set2.clear();
        set2.addAll(Arrays.asList(getOtherElements()));
        assertFalse(getCollection().equals(set2), "Sets with different contents shouldn't be equal");
    }
}
