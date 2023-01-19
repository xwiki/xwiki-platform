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
package org.xwiki.tag.internal;

import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Role;

/**
 * Provides the operations to select tags. Either to list all the tags, count the number of tags in a given space, or
 * list all the documents containing a given tag.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.4.8
 * @since 14.10.4
 */
@Role
public interface TagsSelector
{
    /**
     * @return all the tags of the current wiki
     * @throws TagException in case of issue when retrieving the tags
     */
    List<String> getAllTags() throws TagException;

    /**
     * Return a map of tags and their respective count according to the provided query parts.
     *
     * @param fromHql the {@code from} part of the query used to retrieve the tags
     * @param whereHql the {@code where} part of the query used to retrieve the tags
     * @param parameterValues the list of indexed parameters used in the query
     * @return the maps of tags and their respective count
     * @throws TagException in case of issue where retrieving the tags
     */
    Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, List<?> parameterValues)
        throws TagException;

    /**
     * Return a map of tags and their respective count according to the provided query parts.
     *
     * @param fromHql the {@code from} part of the query used to retrieve the tags
     * @param whereHql the {@code where} part of the query used to retrieve the tags
     * @param parameters the maps of named parameters used in the query
     * @return the maps of tags and their respective count
     * @throws TagException in case of issue where retrieving the tags
     */
    Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, Map<String, ?> parameters)
        throws TagException;

    /**
     * Return a list of serialized documents reference of documents containing a given tag.
     *
     * @param tag the tag to list documents for
     * @param includeHiddenDocuments when {@code true} hidden document are returned as well
     * @return the list of serialized document reference of document containing a given tag
     * @throws TagException in cas of issue where retrieving the documents
     */
    List<String> getDocumentsWithTag(String tag, boolean includeHiddenDocuments) throws TagException;
}
