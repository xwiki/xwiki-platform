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
package org.xwiki.search.solr.internal.api;

import org.xwiki.component.annotation.Role;

/**
 * Encodes and decodes Solr field names so that they can be used in various Solr components. This is needed because not
 * all characters are allowed inside a field name. The Solr schema specifies this:
 * <p>
 * <blockquote>field names should consist of alphanumeric or underscore characters only and not start with a digit. This
 * is not currently strictly enforced, but other field names will not have first class support from all components and
 * back compatibility is not guaranteed. Names with both leading and trailing underscores (e.g. _version_) are
 * reserved.</blockquote>
 * <p>
 * Note that the Solr query syntax supports escaping special characters using backslash, but only in the field value,
 * not in the field name, so we still need to encode the names of dynamic fields we add (e.g. for XClass properties).
 * 
 * @version $Id$
 * @since 5.3RC1
 */
@Role
public interface SolrFieldNameEncoder
{
    /**
     * Encodes the given string so that it can be used as a field name.
     * 
     * @param fieldName the raw field name that needs to be encoded
     * @return the encoded field name
     */
    String encode(String fieldName);

    /**
     * Decodes the raw field name from the given string.
     * 
     * @param fieldName the encoded field name
     * @return the raw field name
     */
    String decode(String fieldName);
}
