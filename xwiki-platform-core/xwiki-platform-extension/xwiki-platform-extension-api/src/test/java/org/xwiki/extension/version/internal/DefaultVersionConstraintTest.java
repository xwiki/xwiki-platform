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
package org.xwiki.extension.version.internal;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.extension.version.IncompatibleVersionConstraintException;
import org.xwiki.extension.version.InvalidVersionRangeException;

public class DefaultVersionConstraintTest
{
    @Test
    public void testMerge() throws IncompatibleVersionConstraintException
    {
        Assert.assertEquals("2.0", new DefaultVersionConstraint("1.0").merge(new DefaultVersionConstraint("2.0"))
            .getValue());
        Assert.assertEquals("{[1.0,2.0]},{[2.0]}",
            new DefaultVersionConstraint("[1.0,2.0]").merge(new DefaultVersionConstraint("[2.0]")).getValue());
        Assert.assertEquals("2.0", new DefaultVersionConstraint("[1.0,2.0]").merge(new DefaultVersionConstraint("2.0"))
            .getValue());

        // Invalid

        try {
            new DefaultVersionConstraint("[1.0]").merge(new DefaultVersionConstraint("[2.0]")).getValue();
            Assert.fail("Should have failed");
        } catch (IncompatibleVersionConstraintException expected) {
            // expected
        }

        try {
            new DefaultVersionConstraint("1.0").merge(new DefaultVersionConstraint("[2.0]")).getValue();
            Assert.fail("Should have failed");
        } catch (IncompatibleVersionConstraintException expected) {
            // expected
        }
    }

    @Test
    public void testParse() throws InvalidVersionRangeException
    {
        Assert.assertEquals("1.0", new DefaultVersionConstraint("1.0").getVersion().getValue());
        Assert.assertEquals(Arrays.asList(new DefaultVersionRangeCollection("[1.0]")), new DefaultVersionConstraint(
            "[1.0]").getRanges());
        Assert.assertEquals(Arrays.asList(new DefaultVersionRangeCollection("[1.0]")), new DefaultVersionConstraint(
            "{[1.0]}").getRanges());
        Assert.assertEquals(Arrays.asList(new DefaultVersionRangeCollection("[1.0]"),
            new DefaultVersionRangeCollection("[1.0,2.0],[1.0]")), new DefaultVersionConstraint(
            "{[1.0]},{[1.0,2.0],[1.0]}").getRanges());

        // Invalid goes to version

        Assert.assertEquals("[1.0", new DefaultVersionConstraint("[1.0").getVersion().getValue());
    }
}
