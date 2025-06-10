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
package org.xwiki.rest.internal.resources;

import java.net.URI;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.SearchResult;

/**
 * A component role that provides search results for a keyword query with the given options.
 *
 * @version $Id$
 * @since 17.5.0RC1
 */
@Role
public interface KeywordSearchSource
{
    /**
     * Search for results based on the given keywords and options.
     *
     * @param keywords the keywords to search for
     * @param options the options to customize the search, such as scope and filters
     * @param baseURI the base URI for links
     * @return a list of search results matching the keywords and options
     * @throws XWikiRestException if an error occurs during the search operation
     */
    List<SearchResult> search(String keywords, KeywordSearchOptions options, URI baseURI) throws XWikiRestException;
}
