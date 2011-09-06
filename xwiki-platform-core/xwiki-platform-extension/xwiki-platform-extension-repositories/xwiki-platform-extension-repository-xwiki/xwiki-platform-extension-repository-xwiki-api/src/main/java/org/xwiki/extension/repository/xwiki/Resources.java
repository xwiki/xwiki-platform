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

    /**
     * Execute search query among versions of an extension.
     */
    public final static String SEARCH_FROM_ID = EXTENSION + "/search";

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
    public final static String QPARAM_SEARCH_START = "start";

    /**
     * Maximum number of results.
     */
    public final static String QPARAM_SEARCH_NUMBER = "number";
}
