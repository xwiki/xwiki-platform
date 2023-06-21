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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link Right}.
 * 
 * @version $Id$
 */
class RightTest
{
    static class CustomRight implements RightDescription
    {
        static List<String> REGISTERED_RIGHT_NAMES = new ArrayList<>();

        String name;
        public CustomRight(String name)
        {
            this.name = name;
            REGISTERED_RIGHT_NAMES.add(name);
        }

        @Override
        public String getName()
        {
            return this.name;
        }

        @Override
        public RuleState getDefaultState()
        {
            return RuleState.ALLOW;
        }

        @Override
        public RuleState getTieResolutionPolicy()
        {
            return RuleState.DENY;
        }

        @Override
        public boolean getInheritanceOverridePolicy()
        {
            return false;
        }

        @Override
        public Set<Right> getImpliedRights()
        {
            return Collections.emptySet();
        }

        @Override
        public Set<EntityType> getTargetedEntityType()
        {
            return Collections.singleton(EntityType.SPACE);
        }

        @Override
        public boolean isReadOnly()
        {
            return true;
        }

        static void unregisterAll()
        {
            for (String registeredRightName : REGISTERED_RIGHT_NAMES) {
                Right fooRight = Right.toRight(registeredRightName);
                if (fooRight != Right.ILLEGAL) {
                    fooRight.unregister();
                }
            }

            REGISTERED_RIGHT_NAMES.clear();
        }
    }

    @AfterEach
    void afterEach()
    {
        CustomRight.unregisterAll();
    }

    @Test
    void toRight()
    {
        assertSame(Right.VIEW, Right.toRight("view"));
        assertSame(Right.VIEW, Right.toRight("VIEW"));
        assertSame(Right.ILLEGAL, Right.toRight("notexist"));
    }

    @Test
    void constructorWithImpliedByRight()
    {
        assertNull(Right.VIEW.getImpliedRights());

        Right myRight = new Right(new CustomRight("foo"), Collections.singleton(Right.VIEW));
        assertSame(Right.toRight("foo"), myRight);
        assertEquals(Collections.singleton(myRight), Right.VIEW.getImpliedRights());
    }

    @Test
    void like()
    {
        Right myRight = new Right(new CustomRight("like"));
        assertTrue(myRight.like(new CustomRight("like")));
    }

    /**
     * Check that calling unregister doesn't mess up getting the rights based on their ordinal.
     * See <a href="https://jira.xwiki.org/browse/XWIKI-21012">XWIKI-21012</a>
     */
    @Test
    void unregister()
    {
        Right myRight = new Right(new CustomRight("right1"));
        Right myOtherRight = new Right(new CustomRight("right2"), Collections.singleton(Right.VIEW));
        int ordinal = myRight.ordinal();
        int otherOrdinal = myOtherRight.ordinal();

        // We can find the rights based on their ordinal
        assertEquals(myRight, Right.get(ordinal));
        assertEquals(myOtherRight, Right.get(otherOrdinal));

        // Check that the ordinal are based on the number of rights
        assertEquals(ordinal + 2, Right.size());
        assertEquals(otherOrdinal + 1, Right.size());

        myRight.unregister();

        // The list of right has one less element
        assertEquals(otherOrdinal, Right.size());

        // We can still get the right based on its ordinal
        assertEquals(myOtherRight, Right.get(otherOrdinal));

        Right anotherCustomRight = new Right(new CustomRight("right3"), Collections.singleton(Right.EDIT));
        int anotherOrdinal = anotherCustomRight.ordinal();

        // Adding a new right will reuse the previous place:
        // this is related to the limitation of 64 rights max
        assertEquals(otherOrdinal + 1, Right.size());
        assertEquals(anotherOrdinal, ordinal);
        assertEquals(myOtherRight, Right.get(otherOrdinal));

        // Ensure that it's really a new right even if it's sharing same ordinal,
        // and that we can access the proper rights based on their ordinal
        assertNotEquals(anotherCustomRight, myRight);
        assertEquals(anotherCustomRight, Right.get(anotherOrdinal));
        assertEquals(myOtherRight, Right.get(otherOrdinal));
    }

    /**
     * Assess we have a limitation for the number of rights
     */
    @Test
    void maxSizeLimitation()
    {
        int size = Right.size();
        for (int i = 0; i < 64 - size; i++) {
            new Right(new CustomRight("right" + i));
        }
        IndexOutOfBoundsException indexOutOfBoundsException =
            assertThrows(IndexOutOfBoundsException.class, () -> {
                new Right(new CustomRight("right64"));
            });
        assertEquals("You cannot register more than [64] rights.", indexOutOfBoundsException.getMessage());
    }
}
