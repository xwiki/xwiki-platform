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
package org.xwiki.platform.security.requiredrights.internal;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for velocity. This class is used to check if a string contains a velocity script.
 *
 * TODO move this class to a more appropriate place
 * @version $Id$
 */
public final class VelocityUtil
{
    private VelocityUtil()
    {
    }

    /**
     * Checks if a string contains a velocity script.
     *
     * @param input the string to check
     * @return true if the string contains a velocity script, false otherwise
     */
    public static boolean containsVelocityScript(String input)
    {
        return StringUtils.containsAny(input, "#", "$");
    }
}
