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
package org.xwiki.test.docker.internal.junit5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link MavenTimestampVersionConverter}.
 *
 * @version $Id$
 * @since 10.11RC1
 */
class MavenTimestampVersionConverterTest
{
    @Test
    void convertWhenTimestamp()
    {
        MavenTimestampVersionConverter converter = new MavenTimestampVersionConverter();
        assertEquals("10.11-SNAPSHOT", converter.convert("10.11-20181128.193513-21"));
    }

    @Test
    void convertWhenNotTimestamp()
    {
        MavenTimestampVersionConverter converter = new MavenTimestampVersionConverter();
        assertEquals("10.11-20181128", converter.convert("10.11-20181128"));
    }

    @Test
    void convertWhenAlreadySNAPSHOT()
    {
        MavenTimestampVersionConverter converter = new MavenTimestampVersionConverter();
        assertEquals("10.11-SNAPSHOT", converter.convert("10.11-SNAPSHOT"));
    }
}
