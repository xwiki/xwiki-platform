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
package org.xwiki.wikistream.test.internal;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;

/**
 * @version $Id$
 * @since 6.0M2
 */
public class StringFileAssertComparator implements FileAssertComparator
{
    @Override
    public void assertEquals(String message, File expected, File actual) throws IOException
    {
        String expectedString = FileUtils.readFileToString(expected, "UTF8");
        String actualString = FileUtils.readFileToString(actual, "UTF8");

        Assert.assertEquals(message, expectedString, actualString);
    }

    @Override
    public void assertEquals(String message, byte[] expected, byte[] actual) throws IOException
    {
        String expectedString = IOUtils.toString(expected, "UTF8");
        String actualString = IOUtils.toString(actual, "UTF8");

        Assert.assertEquals(message, expectedString, actualString);
    }
}
