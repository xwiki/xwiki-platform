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
package com.xpn.xwiki.web.sx;

/**
 * Skin extension source. Can be a document, a resource file, or anything else.
 * 
 * @since 1.7M2
 * @version $Id$
 */
public interface SxSource
{
    /** Cache policies available for extensions. */
    enum CachePolicy
    {
        /**
         * Cache for a long time.
         * 
         * @see AbstractSxAction#LONG_CACHE_DURATION
         */
        LONG,

        /**
         * Cache for a short time.
         * 
         * @see AbstractSxAction#SHORT_CACHE_DURATION
         */
        SHORT,

        /**
         * Cache for the proxy/browser's default time, It will be held in the server cache an amount of time specified
         * in AbstractSxAction.
         */
        DEFAULT,

        /** Do not cache at all in server cache or in proxy/browser. */
        FORBID
    }

    /**
     * @return the last date at which the extension source has been modified.
     */
    long getLastModifiedDate();

    /**
     * @return the content of the extension source. For example, a javascript script for a javascript extension.
     */
    String getContent();

    /**
     * @return the cache policy associated with this extension source.
     */
    CachePolicy getCachePolicy();
}
