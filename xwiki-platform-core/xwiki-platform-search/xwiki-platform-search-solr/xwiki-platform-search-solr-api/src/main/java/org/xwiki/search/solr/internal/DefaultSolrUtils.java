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
package org.xwiki.search.solr.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.properties.ConverterManager;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.SolrUtils;

/**
 * Default implementation of {@link SolrUtils}.
 * 
 * @version $Id$
 * @since 12.3RC1
 */
@Component
@Singleton
public class DefaultSolrUtils implements SolrUtils
{
    /**
     * The name of the type string.
     */
    public static final String SOLR_TYPE_STRING = "string";

    /**
     * The name of the type strings.
     */
    public static final String SOLR_TYPE_STRINGS = "strings";

    /**
     * The name of the type boolean.
     */
    public static final String SOLR_TYPE_BOOLEAN = "boolean";

    /**
     * The name of the type booleans.
     */
    public static final String SOLR_TYPE_BOOLEANS = "booleans";

    /**
     * The name of the type pint.
     */
    public static final String SOLR_TYPE_PINT = "pint";

    /**
     * The name of the type pints.
     */
    public static final String SOLR_TYPE_PINTS = "pints";

    /**
     * The name of the type pfloat.
     */
    public static final String SOLR_TYPE_PFLOAT = "pfloat";

    /**
     * The name of the type pfloats.
     */
    public static final String SOLR_TYPE_PFLOATS = "pfloats";

    /**
     * The name of the type plong.
     */
    public static final String SOLR_TYPE_PLONG = "plong";

    /**
     * The name of the type plong.
     */
    public static final String SOLR_TYPE_PLONGS = "plongs";

    /**
     * The name of the type pdouble.
     */
    public static final String SOLR_TYPE_PDOUBLE = "pdouble";

    /**
     * The name of the type pdoubles.
     */
    public static final String SOLR_TYPE_PDOUBLES = "pdoubles";

    /**
     * The name of the type pdate.
     */
    public static final String SOLR_TYPE_PDATE = "pdate";

    /**
     * The name of the type pdates.
     */
    public static final String SOLR_TYPE_PDATES = "pdates";

    /**
     * The name of the type binary.
     */
    public static final String SOLR_TYPE_BINARY = "binary";

    private static final String PATTERN_GROUP = "(.+)";

    private static final Map<Class<?>, String> CLASS_SUFFIX_MAPPING = new HashMap<>();

    static {
        CLASS_SUFFIX_MAPPING.put(String.class, SOLR_TYPE_STRING);
        CLASS_SUFFIX_MAPPING.put(Double.class, SOLR_TYPE_PDOUBLE);
        CLASS_SUFFIX_MAPPING.put(double.class, SOLR_TYPE_PDOUBLE);
        CLASS_SUFFIX_MAPPING.put(Float.class, SOLR_TYPE_PFLOAT);
        CLASS_SUFFIX_MAPPING.put(float.class, SOLR_TYPE_PFLOAT);
        CLASS_SUFFIX_MAPPING.put(Long.class, SOLR_TYPE_PLONG);
        CLASS_SUFFIX_MAPPING.put(long.class, SOLR_TYPE_PLONG);
        CLASS_SUFFIX_MAPPING.put(Integer.class, SOLR_TYPE_PINT);
        CLASS_SUFFIX_MAPPING.put(int.class, SOLR_TYPE_PINT);
        CLASS_SUFFIX_MAPPING.put(Boolean.class, SOLR_TYPE_BOOLEAN);
        CLASS_SUFFIX_MAPPING.put(boolean.class, SOLR_TYPE_BOOLEAN);
        CLASS_SUFFIX_MAPPING.put(Date.class, SOLR_TYPE_PDATE);
        CLASS_SUFFIX_MAPPING.put(byte[].class, SOLR_TYPE_BINARY);
    }

    @Inject
    private ConverterManager converter;

    /**
     * @param value the value for which to find the storage type
     * @return the type to use to store the value in Solr
     */
    public static String getTypeName(Object value)
    {
        String typeName;

        if (value == null) {
            // TODO: add support for null values ?
            return null;
        }

        typeName = CLASS_SUFFIX_MAPPING.get(value.getClass());

        if (typeName != null) {
            return typeName;
        }

        if (value instanceof Collection) {
            typeName = getTypeName((Collection) value);
        } else if (value.getClass().isArray()) {
            typeName = getTypeName(value.getClass().getComponentType());
            if (typeName != null) {
                // It's an array so use the multivalued version of the type
                typeName += 's';
            }
        } else {
            typeName = getTypeName(value.getClass());
        }

        return typeName;
    }

    private static String getTypeName(Iterable<?> collection)
    {
        String typeName = null;

        for (Object element : collection) {
            if (element != null) {
                String elementType = getTypeName(element);

                if (elementType == null || (typeName != null && typeName != elementType)) {
                    return null;
                }

                typeName = elementType;
            }
        }

        return typeName + 's';
    }

    private static String getTypeName(Class<?> valueClass)
    {
        return CLASS_SUFFIX_MAPPING.get(valueClass);
    }

    /**
     * @param mapFieldName the name of the virtual map field
     * @param type the type of the value
     * @return the prefix to used for the names of the dynamic Solr fields used for this map
     */
    public static String getMapDynamicFieldName(String mapFieldName, String type)
    {
        return getMapFieldName("*", mapFieldName, type);
    }

    /**
     * @param mapFieldName the name of the virtual map field
     * @return the {@link Pattern} to use to match a dynamic field used for this map
     */
    public static Pattern getMapFieldPattern(String mapFieldName)
    {
        return Pattern.compile(getMapFieldName(PATTERN_GROUP, mapFieldName, PATTERN_GROUP));
    }

    /**
     * @param key the key in the map
     * @param mapFieldName the name of the virtual map field
     * @param type the type of the value
     * @return the name of the Solr field
     */
    public static String getMapFieldName(String key, String mapFieldName, String type)
    {
        return key + "__" + mapFieldName + '_' + type;
    }

    @Override
    public Map<String, Object> getMap(String mapFieldName, SolrDocument document)
    {
        Map<String, Object> map = new HashMap<>();

        Pattern pattern = getMapFieldPattern(mapFieldName);

        for (String fieldName : document.getFieldNames()) {
            Matcher matcher = pattern.matcher(fieldName);

            if (matcher.matches()) {
                map.put(matcher.group(1), document.getFieldValue(fieldName));
            }
        }

        return map;
    }

    @Override
    public void setMap(String mapFieldName, Map<String, ?> fieldValue, SolrInputDocument document)
    {
        fieldValue.forEach((key, value) -> {
            // TODO: add support for null value ?
            if (value != null) {
                String typeName = CLASS_SUFFIX_MAPPING.get(value.getClass());

                if (typeName != null) {
                    document.setField(getMapFieldName(key, mapFieldName, typeName), value);
                } else if (value instanceof Iterable) {
                    typeName = getTypeName((Iterable) value);

                    if (typeName != null) {
                        for (Object element : (Iterable) value) {
                            document.addField(getMapFieldName(key, mapFieldName, typeName), element);
                        }
                    }
                } else if (value.getClass().isArray()) {
                    typeName = getTypeName(value.getClass().getComponentType());

                    if (typeName != null) {
                        // It's an array so use the multivalued version of the type
                        typeName += 's';

                        for (int i = 0; i < Array.getLength(value); ++i) {
                            document.addField(getMapFieldName(key, mapFieldName, typeName), Array.get(value, i));
                        }
                    }
                    if (typeName != null) {
                        // It's an array so use the multivalued version of the type
                        typeName += 's';
                    }
                }
            }
        });
    }

    @Override
    public String getId(SolrDocument document)
    {
        return get(AbstractSolrCoreInitializer.SOLR_FIELD_ID, document);
    }

    @Override
    public <T> T get(String fieldName, SolrDocument document)
    {
        return (T) document.getFieldValue(fieldName);
    }

    @Override
    public void setId(Object fieldValue, SolrInputDocument document)
    {
        set(AbstractSolrCoreInitializer.SOLR_FIELD_ID, fieldValue, document);
    }

    @Override
    public void set(String fieldName, Object fieldValue, SolrInputDocument document)
    {
        document.setField(fieldName, fieldValue);
    }

    @Override
    public void set(String fieldName, Collection<?> fieldValue, SolrInputDocument document)
    {
        // Make sure to cleanup any existing value
        document.removeField(fieldName);

        // Add each value of the collection
        fieldValue.forEach(e -> document.addField(fieldName, e));
    }

    @Override
    public void setString(String fieldName, Object fieldValue, SolrInputDocument document)
    {
        String value;
        if (fieldValue instanceof String) {
            value = (String) fieldValue;
        } else {
            value = this.converter.convert(String.class, fieldValue);
        }

        document.setField(fieldName, value);
    }

    @Override
    public <T> T get(String fieldName, SolrDocument document, Type targetType)
    {
        return this.converter.convert(targetType, get(fieldName, document));
    }

    @Override
    public <T> Collection<T> getCollection(String fieldName, SolrDocument document)
    {
        return (Collection) document.getFieldValues(fieldName);
    }

    @Override
    public <T> Set<T> getSet(String fieldName, SolrDocument document)
    {
        Collection<T> collection = getCollection(fieldName, document);

        if (collection == null) {
            return null;
        }

        return collection instanceof Set ? (Set<T>) collection : new HashSet<>(collection);
    }

    @Override
    public <T> List<T> getList(String fieldName, SolrDocument document)
    {
        Collection<T> collection = getCollection(fieldName, document);

        if (collection == null) {
            return null;
        }

        return collection instanceof Set ? (List<T>) collection : new ArrayList<>(collection);
    }
}
