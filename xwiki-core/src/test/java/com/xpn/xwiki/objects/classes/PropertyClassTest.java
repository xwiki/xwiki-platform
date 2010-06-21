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
package com.xpn.xwiki.objects.classes;

import junit.framework.Assert;

import org.apache.commons.lang.math.RandomUtils;

import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit tests for the base {@link PropertyClass} class.
 * 
 * @version $Id$
 * @since 2.4M2
 */
public class PropertyClassTest extends AbstractBridgedXWikiComponentTestCase
{
    /** Test the {@link PropertyClass#compareTo(PropertyClass)} method. */
    public void testCompareTo()
    {
        PropertyClass one = new PropertyClass();
        PropertyClass two = new PropertyClass();
        // Random numbers to be used as property indexes.
        int n1, n2;

        one.setName("first");
        two.setName("second");

        // Since the test might randomly succeed, run it several times to be safer.
        for (int i = 0; i < 20; ++i) {
            n1 = RandomUtils.nextInt();
            n2 = RandomUtils.nextInt();
            one.setNumber(n1);
            two.setNumber(n2);

            if (n1 == n2) {
                Assert.assertEquals(Math.signum(one.compareTo(two)), -1.0, 0);
                Assert.assertEquals(Math.signum(two.compareTo(one)), 1.0, 0);
            } else {
                Assert.assertEquals(Math.signum(one.compareTo(two)), Math.signum(n1 - n2));
                Assert.assertEquals(Math.signum(two.compareTo(one)), Math.signum(n2 - n1));
            }
        }

        // Also test that the comparison takes into account the name in case the two numbers are identical
        one.setNumber(42);
        two.setNumber(42);
        Assert.assertEquals(Math.signum(one.compareTo(two)), -1.0, 0);
        Assert.assertEquals(Math.signum(two.compareTo(one)), 1.0, 0);
    }
}
