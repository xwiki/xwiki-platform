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
package org.xwiki.extension.xar.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.extension.xar.internal.security.XarSecurityRule;
import org.xwiki.extension.xar.internal.security.XarSecurityTool;
import org.xwiki.security.authorization.Right;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

class XarSecurityRuleTest
{
    private XarSecurityTool xarSecurityTool;

    private XarSecurityRule adminSimple;
    private XarSecurityRule adminSimple2;
    private XarSecurityRule commentSimple;
    private XarSecurityRule adminExtended;

    @BeforeEach
    void setUp()
    {
        xarSecurityTool = mock(XarSecurityTool.class);

        // Create some XarSecurityRules that will be compared
        adminSimple = new XarSecurityRule(Right.ADMIN, true, xarSecurityTool);
        adminSimple2 = new XarSecurityRule(Right.ADMIN, true, xarSecurityTool);
        commentSimple = new XarSecurityRule(Right.COMMENT, true, xarSecurityTool);
        adminExtended = new XarSecurityRule(Right.ADMIN, false, xarSecurityTool);
    }

    @Test
    void testEquals()
    {
        // Test the identity
        assertEquals(adminSimple, adminSimple);
        assertEquals(adminSimple, adminSimple2);

        // Test with different rights
        assertNotEquals(adminSimple, commentSimple);

        // Test with a simple / extended rule
        assertNotEquals(adminSimple, adminExtended);
    }

    @Test
    void testHashCode()
    {
        // Test the identity
        assertEquals(adminSimple.hashCode(), adminSimple.hashCode());
        assertEquals(adminSimple.hashCode(), adminSimple2.hashCode());

        // Test with different rights
        assertNotEquals(adminSimple.hashCode(), commentSimple.hashCode());

        // Test with a simple / extended rule
        assertNotEquals(adminSimple.hashCode(), adminExtended.hashCode());
    }
}
