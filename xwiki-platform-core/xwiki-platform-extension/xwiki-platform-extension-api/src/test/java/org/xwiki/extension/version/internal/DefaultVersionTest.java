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

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.extension.version.Version;

public class DefaultVersionTest
{
    @Test
    public void testCompareTo()
    {
        Assert.assertTrue(new DefaultVersion("1.1").compareTo(new DefaultVersion("1.1")) == 0);
        Assert.assertTrue(new DefaultVersion("1.2").compareTo(new DefaultVersion("1.1")) > 0);
        Assert.assertTrue(new DefaultVersion("1.1").compareTo(new DefaultVersion("1.2")) < 0);

        Assert.assertTrue(new DefaultVersion("1.1").compareTo(new DefaultVersion("1.1w")) < 0);

        Assert.assertTrue(new DefaultVersion("1.1").compareTo(new DefaultVersion("1.1-milestone-1")) > 0);
    }

    @Test
    public void testType()
    {
        Assert.assertEquals(Version.Type.SNAPSHOT, new DefaultVersion("1.1-SNAPSHOT").getType());
        Assert.assertEquals(Version.Type.BETA, new DefaultVersion("1.1-milestone-1").getType());
        Assert.assertEquals(Version.Type.STABLE, new DefaultVersion("1.1").getType());
        
    }
}
