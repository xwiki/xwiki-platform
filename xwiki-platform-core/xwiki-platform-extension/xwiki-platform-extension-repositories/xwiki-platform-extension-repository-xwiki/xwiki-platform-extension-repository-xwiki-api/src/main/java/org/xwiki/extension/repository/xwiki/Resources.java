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
package org.xwiki.extension.repository.xwiki;

/**
 * Resources to use to access Extension Manager REST service.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class Resources
{
    // Entry point

    /**
     * Entry point of XWiki Repository protocol.
     */
    public final static String ENTRYPOINT = "/repository";

    // Path parameters

    /**
     * Name of the parameter indication the id of the extension.
     */
    public final static String PPARAM_EXTENSIONID = "extensionId";

    /**
     * Name of the parameter indication the version of the extension.
     */
    public final static String PPARAM_EXTENSIONVERSION = "extensionVersion";

    // Extensions

    /**
     * Get extensions.
     */
    public final static String EXTENSIONS = ENTRYPOINT + "/extensions";

    /**
     * Get extension informations.
     */
    public final static String EXTENSION = EXTENSIONS + "/{" + PPARAM_EXTENSIONID + "}";

    /**
     * Get extension versions
     */
    public final static String EXTENSION_VERSIONS = EXTENSION + "/versions";

    /**
     * Get extension version informations
     */
    public final static String EXTENSION_VERSION = EXTENSION_VERSIONS + "/{" + PPARAM_EXTENSIONVERSION + "}";

    /**
     * Download extension file.
     */
    public final static String EXTENSION_VERSION_FILE = EXTENSION_VERSIONS + "/file";

    // Search

    /**
     * Execute search query among extensions.
     */
    public final static String SEARCH = ENTRYPOINT + "/search";

    // Query parameters

    /**
     * Language of the result to produce.
     */
    public final static String QPARAM_LANGUAGE = "language";

    /**
     * The search query.
     */
    public final static String QPARAM_SEARCH_QUERY = "q";

    /**
     * Offset from where the search start to return results.
     */
    public final static String QPARAM_LIST_START = "start";

    /**
     * Maximum number of results.
     */
    public final static String QPARAM_LIST_NUMBER = "number";
}
