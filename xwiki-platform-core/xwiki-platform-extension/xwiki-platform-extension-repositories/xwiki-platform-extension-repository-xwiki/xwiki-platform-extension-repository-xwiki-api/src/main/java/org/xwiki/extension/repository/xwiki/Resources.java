package org.xwiki.extension.repository.xwiki;

/**
 * Resources to use to access Extension Manager REST service.
 * <p>
 * Resources producing list of result support the following arguments:
 * <ul>
 * <li>start: offset where the search start to return results</li>
 * <li>number: maximum number of results</li>
 * </ul>
 * <p>
 * Resources producing extensions descriptor support the following arguments:
 * <ul>
 * <li>language: language used to resolve the descriptor values</li>
 * </ul>
 * 
 * @version $Id$
 */
public class Resources
{
    // Entry point

    public final static String ENTRYPOINT = "/repository";

    // Extensions

    /**
     * ?start={offset}&number={number}
     */
    public final static String EXTENSIONS = ENTRYPOINT + "/extensions";

    public final static String EXTENSIONS_ID = EXTENSIONS + "/{extensionId}";

    public final static String EXTENSIONSVERSIONS = EXTENSIONS_ID + "/versions";

    public final static String EXTENSIONSVERSIONS_VERSION = EXTENSIONSVERSIONS + "/{extensionVersion}";

    public final static String EXTENSIONFILE = EXTENSIONSVERSIONS + "/file";

    // Search

    /**
     * ?q={keywords}
     */
    public final static String SEARCH = ENTRYPOINT + "/search";

    /**
     * ?q={keywords}
     */
    public final static String SEARCH_FROM_ID = EXTENSIONS_ID + "/search";
}
