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
package org.xwiki.extension.internal;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class DefaultVersionManagerTest
{
    private VersionManager versionManager;
    
    @Before
    public void setUp() throws Exception
    {
        this.versionManager = new DefaultVersionManager();
    }
    
    @Test
    public void testWithIntegers()
    {
        Assert.assertEquals(1, versionManager.compareVersions("1.1", "1.0"));
        Assert.assertEquals(8, versionManager.compareVersions("1.10", "1.2"));
    }
    
    @Test
    public void testWithStrings()
    {
        Assert.assertEquals(8, versionManager.compareVersions("1.10-sometext", "1.2"));
        Assert.assertEquals(1, versionManager.compareVersions("1.1-sometext", "1.1"));
        Assert.assertEquals(67, versionManager.compareVersions("1.sometext", "1.0"));
    }
}
