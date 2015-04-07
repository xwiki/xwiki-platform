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
package org.xwiki.rest.internal.resources.search;

import java.util.List;

import javax.ws.rs.core.UriInfo;

import org.xwiki.component.annotation.Role;
import org.xwiki.rest.model.jaxb.SearchResult;

/**
 * Execute a search query.
 * 
 * @version $Id$
 * @since 6.1M2
 */
// TODO: this is wrong, each search type should provide it's own REST API
@Role
public interface SearchSource
{
    List<SearchResult> search(String query, String wikiName, String wikis, boolean hasProgrammingRights,
        String orderField, String order, boolean distinct, int number, int start, Boolean withPrettyNames,
        String className, UriInfo uriInfo) throws Exception;
}
