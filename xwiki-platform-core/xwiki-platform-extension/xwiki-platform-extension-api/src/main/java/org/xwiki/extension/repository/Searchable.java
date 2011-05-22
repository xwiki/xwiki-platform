package org.xwiki.extension.repository;

import java.util.List;

import org.xwiki.extension.Extension;

/**
 * A repository can implements it to provide search capabilities.
 * 
 * @version $Id$
 */
// TODO: add more complete query support
public interface Searchable
{
    /**
     * Search extension based of the provided pattern.
     * <p>
     * The pattern is a simple character chain.
     * 
     * @param pattern the pattern to search
     * @param offset the offset from where to start returning search results
     * @param nb the maximum number of search results to return
     * @return the found extensions descriptors, empty list if nothing could be found
     * @throws SearchException error when trying to search provided pattern
     */
    List<Extension> search(String pattern, int offset, int nb) throws SearchException;
}
