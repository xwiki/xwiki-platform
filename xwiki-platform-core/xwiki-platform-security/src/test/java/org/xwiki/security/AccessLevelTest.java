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
 *
 */
package org.xwiki.security;

import junit.framework.TestCase;

public class AccessLevelTest extends TestCase
{
    public static void assertDefaultAccessLevel()
    {
        assertTrue(AccessLevel.DEFAULT_ACCESS_LEVEL.equals(new AccessLevel() {
                {
                    allow(Right.VIEW);
                    allow(Right.EDIT);
                    allow(Right.COMMENT);
                    allow(Right.LOGIN);
                    allow(Right.REGISTER);
                    deny(Right.DELETE);
                    deny(Right.ADMIN);
                    deny(Right.PROGRAM);
                    deny(Right.ILLEGAL);
                }
            }));
    }

    public void testAccessLevel()
    {
        AccessLevel l = AccessLevel.DEFAULT_ACCESS_LEVEL.clone();

        assertTrue(l.get(Right.VIEW) == RightState.ALLOW);
        assertTrue(l.get(Right.EDIT) == RightState.ALLOW);
        assertTrue(l.get(Right.COMMENT) == RightState.ALLOW);
        assertTrue(l.get(Right.LOGIN) == RightState.ALLOW);
        assertTrue(l.get(Right.REGISTER) == RightState.ALLOW);
        assertTrue(l.get(Right.DELETE) == RightState.DENY);
        assertTrue(l.get(Right.ADMIN) == RightState.DENY);
        assertTrue(l.get(Right.PROGRAM) == RightState.DENY);
        assertTrue(l.get(Right.ILLEGAL) == RightState.DENY);

        l.clear(Right.VIEW);
        l.clear(Right.COMMENT);
        l.clear(Right.LOGIN);
        l.clear(Right.ADMIN);

        assertTrue(l.get(Right.VIEW) == RightState.UNDETERMINED);
        assertTrue(l.get(Right.EDIT) == RightState.ALLOW);
        assertTrue(l.get(Right.COMMENT) == RightState.UNDETERMINED);
        assertTrue(l.get(Right.LOGIN) == RightState.UNDETERMINED);
        assertTrue(l.get(Right.REGISTER) == RightState.ALLOW);
        assertTrue(l.get(Right.DELETE) == RightState.DENY);
        assertTrue(l.get(Right.ADMIN) == RightState.UNDETERMINED);
        assertTrue(l.get(Right.PROGRAM) == RightState.DENY);
        assertTrue(l.get(Right.ILLEGAL) == RightState.DENY);

        l.deny(Right.VIEW);
        l.deny(Right.LOGIN);
        l.allow(Right.ADMIN);
            
        assertTrue(l.get(Right.VIEW) == RightState.DENY);
        assertTrue(l.get(Right.EDIT) == RightState.ALLOW);
        assertTrue(l.get(Right.COMMENT) == RightState.UNDETERMINED);
        assertTrue(l.get(Right.LOGIN) == RightState.DENY);
        assertTrue(l.get(Right.REGISTER) == RightState.ALLOW);
        assertTrue(l.get(Right.DELETE) == RightState.DENY);
        assertTrue(l.get(Right.ADMIN) == RightState.ALLOW);
        assertTrue(l.get(Right.PROGRAM) == RightState.DENY);
        assertTrue(l.get(Right.ILLEGAL) == RightState.DENY);

        l.clear(Right.VIEW);
        l.clear(Right.EDIT);
        l.clear(Right.COMMENT);
        l.clear(Right.LOGIN);
        l.clear(Right.REGISTER);
        l.clear(Right.DELETE);
        l.clear(Right.ADMIN);
        l.clear(Right.PROGRAM);
        l.clear(Right.ILLEGAL);

        assertTrue(l.get(Right.VIEW) == RightState.UNDETERMINED);
        assertTrue(l.get(Right.EDIT) == RightState.UNDETERMINED);
        assertTrue(l.get(Right.COMMENT) == RightState.UNDETERMINED);
        assertTrue(l.get(Right.LOGIN) == RightState.UNDETERMINED);
        assertTrue(l.get(Right.REGISTER) == RightState.UNDETERMINED);
        assertTrue(l.get(Right.DELETE) == RightState.UNDETERMINED);
        assertTrue(l.get(Right.ADMIN) == RightState.UNDETERMINED);
        assertTrue(l.get(Right.PROGRAM) == RightState.UNDETERMINED);
        assertTrue(l.get(Right.ILLEGAL) == RightState.UNDETERMINED);

        l.allow(Right.VIEW);
        l.allow(Right.EDIT);
        l.allow(Right.COMMENT);
        l.allow(Right.LOGIN);
        l.allow(Right.REGISTER);
        l.allow(Right.DELETE);
        l.allow(Right.ADMIN);
        l.allow(Right.PROGRAM);
        l.allow(Right.ILLEGAL);

        assertTrue(l.get(Right.VIEW) == RightState.ALLOW);
        assertTrue(l.get(Right.EDIT) == RightState.ALLOW);
        assertTrue(l.get(Right.COMMENT) == RightState.ALLOW);
        assertTrue(l.get(Right.LOGIN) == RightState.ALLOW);
        assertTrue(l.get(Right.REGISTER) == RightState.ALLOW);
        assertTrue(l.get(Right.DELETE) == RightState.ALLOW);
        assertTrue(l.get(Right.ADMIN) == RightState.ALLOW);
        assertTrue(l.get(Right.PROGRAM) == RightState.ALLOW);
        assertTrue(l.get(Right.ILLEGAL) == RightState.ALLOW);

        assertDefaultAccessLevel();
    }

    public void testClone()
    {
        AccessLevel l = AccessLevel.DEFAULT_ACCESS_LEVEL.clone();
        AccessLevel k = l.clone();
        assertTrue(k.equals(l));
        assertFalse(k == l);
        l = l.getExistingInstance();
        k = k.getExistingInstance();
        assertTrue(k == l);

        assertDefaultAccessLevel();
    }
}
