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
package org.xwiki.activeinstalls2;

import org.xwiki.stability.Unstable;

/**
 * Raised when {@link DataManager#countDistinctInstallsByExtension(String)} finds more extensions than it can count at
 * once. It is a distinct type so that a caller can tell that case apart from the generic {@link Exception} that a
 * failure to reach the data raises.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@Unstable
public class TooManyExtensionsException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * @param maxExtensionCountPerQuery the maximum number of extensions that can be counted at once
     */
    public TooManyExtensionsException(int maxExtensionCountPerQuery)
    {
        super(String.format("Found more extensions than the [%d] that can be counted at once.",
            maxExtensionCountPerQuery));
    }
}
