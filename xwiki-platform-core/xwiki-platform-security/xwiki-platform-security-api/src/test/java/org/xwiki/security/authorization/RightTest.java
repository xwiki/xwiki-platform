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

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.xwiki.model.EntityType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link Right}.
 * 
 * @version $Id$
 */
public class RightTest
{
    class CustomRight implements RightDescription
    {
        private String name;

        CustomRight(String name)
        {
            this.name = name;
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
    }

    @Test
    void toRight()
    {
        assertSame(Right.VIEW, Right.toRight("view"));
        assertSame(Right.VIEW, Right.toRight("VIEW"));
        assertSame(Right.ILLEGAL, Right.toRight("notexist"));
    }

    @Disabled("Disabled because it breaks the DefaultAuthorizationManagerIntegrationTest since Rights are static.")
    @Test
    void constructorWithImpliedByRight()
    {
        assertNull(Right.VIEW.getImpliedRights());

        Right myRight = new Right(new CustomRight("foo"), Collections.singleton(Right.VIEW));
        assertSame(Right.toRight("foo"), myRight);
        assertEquals(Collections.singleton(myRight), Right.VIEW.getImpliedRights());
    }

    @Disabled("Disabled because it breaks the DefaultAuthorizationManagerIntegrationTest since Rights are static.")
    @Test
    void like()
    {
        Right myRight = new Right(new CustomRight("foo"));
        assertTrue(myRight.like(new CustomRight("foo")));
    }
}
