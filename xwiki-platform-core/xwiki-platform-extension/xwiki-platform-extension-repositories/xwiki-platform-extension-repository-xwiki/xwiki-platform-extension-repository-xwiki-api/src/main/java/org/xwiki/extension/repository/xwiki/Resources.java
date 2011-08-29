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

    public final static String ENTRYPOINT = "/extensions";

    // Extensions

    /**
     * ?start={offset}&number={number}
     */
    public final static String EXTENSION_ID = ENTRYPOINT + "/ids/{extensionId}";

    public final static String EXTENSION = EXTENSION_ID + "/versions/{extensionVersion}";

    public final static String EXTENSIONFILE = EXTENSION + "/file";

    // Search

    /**
     * ?q={keywords}
     */
    public final static String SEARCH = ENTRYPOINT + "/search";

    /**
     * ?q={keywords}
     */
    public final static String SEARCH_FROM_ID = EXTENSION_ID + "/search";

    // Index

    public final static String EXTENSION_INDEX = ENTRYPOINT + "/index";
}
