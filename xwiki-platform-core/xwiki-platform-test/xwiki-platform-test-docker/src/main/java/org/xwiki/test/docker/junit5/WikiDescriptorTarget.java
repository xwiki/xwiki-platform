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

/**
 * What host/port is the default (and so, used to generate external URL in background threads) in the wiki descriptor.
 *
 * @version $Id$
 * @since 16.10.19
 * @since 17.10.11
 * @since 18.4.4
 * @since 18.7.0RC1
 */
public enum WikiDescriptorTarget
{
    /**
     * Use the host/port through which the browser accesses XWiki.
     */
    BROWSER,

    /**
     * Use the host/port through which the HTTP client running on the host, that is outside of Docker, accesses XWiki
     * (default).
     */
    HTTP_CLIENT
}
