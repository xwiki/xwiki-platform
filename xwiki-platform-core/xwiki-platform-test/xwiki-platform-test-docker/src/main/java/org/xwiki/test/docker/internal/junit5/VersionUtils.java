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

import java.lang.reflect.Method;

/**
 * Utility methods for parsing versions.
 *
 * @version $Id$
 * @since 13.10RC1
 */
public final class VersionUtils
{
    /*
     * Minimal version supported.
     */
    private static final int MINIMAL_VERSION = 8;

    private VersionUtils()
    {
        // Prevents instantiation.
    }

    /**
     * @return the major java version (e.g. {@code 17} for Java 17).
     */
    public static int getJavaMajorVersion()
    {
        String javaVersionString = System.getProperty("java.version");
        return getJavaMajorVersion(javaVersionString);
    }

    private static int getJavaMajorVersion(String versionString)
    {
        if (versionString == null) {
            return MINIMAL_VERSION;
        }
        if (versionString.startsWith("1.")) {
            return versionString.charAt(2) - '0';
        } else {
            // We are running under Java 9 or later
            try {
                Method versionMethod = Runtime.class.getMethod("version");
                Object versionObj = versionMethod.invoke(null);
                Method majorMethod = versionObj.getClass().getMethod("major");
                Integer resultInteger = (Integer) majorMethod.invoke(versionObj);
                return resultInteger.intValue();
            } catch (Exception e) {
                return MINIMAL_VERSION;
            }
        }
    }
}
