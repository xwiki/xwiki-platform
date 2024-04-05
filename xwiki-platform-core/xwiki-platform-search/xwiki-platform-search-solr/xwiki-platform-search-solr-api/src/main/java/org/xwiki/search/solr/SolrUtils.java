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
package org.xwiki.search.solr;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Role;

/**
 * Various helpers around the Solr API.
 * 
 * @version $Id$
 * @since 12.3RC1
 */
@Role
public interface SolrUtils
{
    /**
     * Set or replace the field value(s) with the specified value(s), or remove the values if 'null' or empty list is
     * specified as the new value. May be specified as a single value, or as a list for multiValued fields.
     * 
     * @since 12.5RC1
     */
    String ATOMIC_UPDATE_MODIFIER_SET = "set";

    /**
     * Adds the specified values to a multiValued field. May be specified as a single value, or as a list.
     * 
     * @since 12.5RC1
     */
    String ATOMIC_UPDATE_MODIFIER_ADD = "add";

    /**
     * Adds the specified values to a multiValued field, only if not already present. May be specified as a single
     * value, or as a list.
     * 
     * @since 12.5RC1
     */
    String ATOMIC_UPDATE_MODIFIER_ADD_DISTINCT = "add-distinct";

    /**
     * Removes (all occurrences of) the specified values from a multiValued field. May be specified as a single value,
     * or as a list.
     * 
     * @since 12.5RC1
     */
    String ATOMIC_UPDATE_MODIFIER_REMOVE = "remove";

    /**
     * Removes all occurrences of the specified regex from a multiValued field. May be specified as a single value, or
     * as a list.
     * 
     * @since 12.5RC1
     */
    String ATOMIC_UPDATE_MODIFIER_REMOVEREGEX = "removeregex";

    /**
     * Increments a numeric value by a specific amount. Must be specified as a single numeric value.
     * 
     * @since 12.5RC1
     */
    String ATOMIC_UPDATE_MODIFIER_INC = "inc";

    /**
     * Extract a {@code Map<String, Object>} from the passed {@link SolrDocument}.
     * <p>
     * Solr {@link DocumentObjectBinder} has native support for maps, unfortunately it only support maps for which all
     * entries have the same type (and only String and primitive types). The point of this API is to provide binding for
     * a generic Map of objects.
     * <p>
     * This API imply that it exist in the schema dynamic fields of the form {@code <mapname>_*_string} (and
     * {@code <mapname>_*_boolean}, {@code <mapname>_*_pint}, etc.). Can be done using
     * {@link AbstractSolrCoreInitializer#addMapField(String)}.
     * 
     * @param <V> the type of the value in the map
     * @param fieldName the prefix of the fields containing the map data
     * @param document the Solr document
     * @return the Map extracted from the Solr document
     * @see #setMap(String, Map, SolrInputDocument)
     */
    <V> Map<String, V> getMap(String fieldName, SolrDocument document);

    /**
     * Set a {@code Map<String, Object>} in the passed {@link SolrDocument}.
     * 
     * @param fieldName the prefix of the fields containing the map data
     * @param fieldValue the map to store in the {@link SolrDocument}
     * @param document the Solr document
     * @see #getMap(String, SolrDocument)
     */
    void setMap(String fieldName, Map<String, ?> fieldValue, SolrInputDocument document);

    /**
     * @param key the key in the map
     * @param mapFieldName the name of the virtual map field
     * @param type the type of the value
     * @return the name of the Solr field
     * @since 13.9RC1
     */
    default String getMapFieldName(String key, String mapFieldName, Type type)
    {
        return mapFieldName;
    }

    /**
     * Extract the identifier of the document.
     * 
     * @param document the Solr document
     * @return the value associated with the passed field name in the document
     */
    String getId(SolrDocument document);

    /**
     * Extract from the document the value associated with the passed field name.
     * 
     * @param <T> the of the value to return
     * @param fieldName the name of the field in the document
     * @param document the Solr document
     * @return the value associated with the passed field name in the document
     */
    <T> T get(String fieldName, SolrDocument document);

    /**
     * Set the identifier of the document.
     * 
     * @param fieldValue the id value to store in the {@link SolrDocument}
     * @param document the Solr document
     */
    void setId(Object fieldValue, SolrInputDocument document);

    /**
     * Store in the document the value associated with the passed field name.
     * 
     * @param fieldName the name of the field in the document
     * @param fieldValue the value to store in the {@link SolrDocument}
     * @param document the Solr document
     */
    void set(String fieldName, Object fieldValue, SolrInputDocument document);

    /**
     * Store in the document the value associated with the passed field name.
     * 
     * @param modifier the atomic update modifier to apply (set, add, add-distinct, remove, removeregex, inc)
     * @param fieldName the name of the field in the document
     * @param fieldValue the value to store in the {@link SolrDocument}
     * @param document the Solr document
     * @since 12.5RC1
     */
    void setAtomic(String modifier, String fieldName, Object fieldValue, SolrInputDocument document);

    /**
     * Store in the document the value associated with the passed field name.
     *
     * @param modifier the atomic update modifier to apply (set, add, add-distinct, remove, removeregex, inc)
     * @param fieldName the name of the field in the document
     * @param fieldValue the value to store in the {@link SolrDocument}
     * @param valueType the type to use as reference to serialize the value
     * @param document the Solr document
     * @since 12.10
     */
    default void setAtomic(String modifier, String fieldName, Object fieldValue, Type valueType,
        SolrInputDocument document)
    {
        setAtomic(modifier, fieldName, fieldValue, document);
    }

    /**
     * Store in the document the values associated with the passed field name.
     * <p>
     * The field is expected to be multivalued.
     * 
     * @param fieldName the name of the field in the document
     * @param fieldValue the value to store in the {@link SolrDocument}
     * @param document the Solr document
     */
    void set(String fieldName, Collection<?> fieldValue, SolrInputDocument document);



    /**
     * Store in the document the value associated with the passed field name.
     * <p>
     * If the value is not of type {@link String} it's converted automatically.
     * 
     * @param fieldName the name of the field in the document
     * @param fieldValue the value to store in the {@link SolrDocument}
     * @param document the Solr document
     */
    void setString(String fieldName, Object fieldValue, SolrInputDocument document);

    /**
     * Store in the document the value associated with the passed field name.
     * <p>
     * If the value is not of type {@link String} it's converted automatically.
     * 
     * @param fieldName the name of the field in the document
     * @param fieldValue the value to store in the {@link SolrDocument}
     * @param valueType the type to use as reference to serialize the value
     * @param document the Solr document
     * @since 12.6
     */
    void setString(String fieldName, Object fieldValue, Type valueType, SolrInputDocument document);

    /**
     * @param fieldName the name of the field in the document
     * @param fieldValue the value to store in the {@link SolrDocument}
     * @param valueType the type to use as reference to serialize the values
     * @param document the Solr document
     * @since 12.9RC1
     */
    default void setString(String fieldName, Collection<?> fieldValue, Type valueType, SolrInputDocument document)
    {
        setString(fieldName, fieldValue, document);
    }

    /**
     * Serialize the value into a value usable in a Solr filter query.
     * 
     * @param fieldValue the value of a field
     * @return the Solr query version of the passed value
     */
    String toFilterQueryString(Object fieldValue);

    /**
     * Serialize the value into a value usable in a Solr filter query.
     * 
     * @param fieldValue the value of a field
     * @param valueType the type to use as reference to serialize the value
     * @return the Solr query version of the passed value
     * @since 12.9RC1
     */
    default String toFilterQueryString(Object fieldValue, Type valueType)
    {
        return toFilterQueryString(fieldValue);
    }

    /**
     * Serialize the value into a string that is a complete query string, i.e., cannot be combined with other strings.
     * <p>
     * For example, this serializes the empty string as "".
     *
     * @param fieldValue the value of a field
     * @return the Solr query version of the passed value
     * @since 13.10.7
     * @since 14.4.2
     * @since 14.5
     */
    default String toCompleteFilterQueryString(Object fieldValue)
    {
        return toFilterQueryString(fieldValue);
    }

    /**
     * Serialize the value into a string that is a complete query string, i.e., cannot be combined with other strings.
     * <p>
     * For example, this serializes the empty string as "".
     *
     * @param fieldValue the value of a field
     * @param valueType the type to use as reference to serialize the value
     * @return the Solr query version of the passed value
     * @since 13.10.7
     * @since 14.4.2
     * @since 14.5
     */
    default String toCompleteFilterQueryString(Object fieldValue, Type valueType)
    {
        return toFilterQueryString(fieldValue, valueType);
    }


    /**
     * Extract from the document the value associated with the passed field name.
     * 
     * @param <T> the of the value to return
     * @param fieldName the name of the field in the document
     * @param document the Solr document
     * @param targetType the type of the value to return
     * @return the value associated with the passed field name in the document
     */
    <T> T get(String fieldName, SolrDocument document, Type targetType);

    /**
     * Extract from the document the value associated with the passed field name.
     * 
     * @param <T> the of the value to return
     * @param fieldName the name of the field in the document
     * @param document the Solr document
     * @param def the default value if none is available
     * @return the value associated with the passed field name in the document
     * @since 12.6
     */
    <T> T get(String fieldName, SolrDocument document, T def);

    /**
     * Extract from the document the values associated with the passed field name.
     * 
     * @param <T> the of the value to return
     * @param fieldName the name of the field in the document
     * @param document the Solr document
     * @param targetType the type of the value to return
     * @return the value associated with the passed field name in the document
     * @since 12.9RC1
     */
    default <T> Collection<T> getCollection(String fieldName, SolrDocument document, Type targetType)
    {
        return getCollection(fieldName, document);
    }

    /**
     * Extract from the document the values associated with the passed field name.
     * 
     * @param <T> the of the value to return
     * @param fieldName the name of the field in the document
     * @param document the Solr document
     * @return the value associated with the passed field name in the document
     */
    <T> Collection<T> getCollection(String fieldName, SolrDocument document);

    /**
     * Extract from the document the values associated with the passed field name.
     * 
     * @param <T> the of the value to return
     * @param fieldName the name of the field in the document
     * @param document the Solr document
     * @return the value associated with the passed field name in the document
     */
    <T> Set<T> getSet(String fieldName, SolrDocument document);

    /**
     * Extract from the document the values associated with the passed field name.
     * 
     * @param <T> the of the value to return
     * @param fieldName the name of the field in the document
     * @param document the Solr document
     * @return the value associated with the passed field name in the document
     */
    <T> List<T> getList(String fieldName, SolrDocument document);
}
