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
package org.xwiki.test.docker.junit5;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convert Maven timestamped version of the format {@code XXX-<DATE>.<HOUR>-<ID>} into {@code XXX-SNAPSHOT}.
 *
 * For example: {@code 10.11-20181128.193513-21} gets converted to {@code 10.11-SNAPSHOT}.
 *
 * @version $Id$
 * @since 10.11RC1
 */
public class MavenTimestampVersionConverter
{
    private static final Pattern PATTERN = Pattern.compile("(.*)-[0-9]{8}+\\.[0-9]{6}+-[0-9]+");

    /**
     * @param version the version to convert
     * @return the SNAPSHOT version if it's a timestamp or the original passed version otherwise
     */
    public String convert(String version)
    {
        String convertedVersion = version;
        Matcher matcher = PATTERN.matcher(convertedVersion);
        if (matcher.matches()) {
            convertedVersion = String.format("%s-SNAPSHOT", matcher.group(1));
        }
        return convertedVersion;
    }
}
