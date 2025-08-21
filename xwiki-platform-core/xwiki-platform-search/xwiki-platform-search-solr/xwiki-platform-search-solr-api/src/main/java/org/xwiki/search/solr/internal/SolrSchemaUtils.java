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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.schema.FieldType;
import org.xwiki.component.annotation.Component;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;

/**
 * Helper for performing operations in a Solr schema.
 *
 * @version $Id$
 * @since 15.0
 * @since 14.10.4
 */
@Component(roles = SolrSchemaUtils.class)
@Singleton
public class SolrSchemaUtils
{
    /**
     * The name of the type used to store the version of the core schema.
     */
    public static final String SOLR_TYPENAME_CVERSION = "__cversion";

    /**
     * The name of the attribute holding the version value.
     */
    public static final String SOLR_VERSIONFIELDTYPE_VALUE = "defVal";

    /**
     * Contains data of a solr core schema.
     *
     * @version $Id$
     */
    private static final class SolrCoreSchema
    {
        private Map<String, FieldTypeRepresentation> types;

        private Map<String, Map<String, Object>> fields;

        private Map<String, Map<String, Object>> dynamicFields;

        private Map<String, Set<String>> copyFields;

        void reset()
        {
            this.types = null;
            this.fields = null;
            this.dynamicFields = null;
            this.copyFields = null;
        }
    }

    /**
     * The name of the attribute containing the name of the Solr field.
     */
    private static final String SOLR_FIELD_NAME = "name";

    private final Map<String, SolrCoreSchema> coreSchemaMap = new HashMap<>();

    private SolrCoreSchema getSchema(XWikiSolrCore core)
    {
        return this.coreSchemaMap.computeIfAbsent(core.getSolrName(), c -> new SolrCoreSchema());
    }

    /**
     * Retrieve all information about existing fields of the core.
     *
     * @param core the core to search in
     * @param force if {@code true} reloads all information, else gets the information from the cache
     * @return the map of all fields
     * @throws SolrException in case of problem to request fields information
     * @since 16.2.0RC1
     */
    public Map<String, FieldTypeRepresentation> getFieldTypes(XWikiSolrCore core, boolean force) throws SolrException
    {
        SolrCoreSchema schema = getSchema(core);
        if (schema.types == null || force) {
            SchemaResponse.FieldTypesResponse response;
            try {
                response = new SchemaRequest.FieldTypes().process(core.getClient());
            } catch (Exception e) {
                throw new SolrException("Failed to get the list of field types", e);
            }

            Map<String, FieldTypeRepresentation> map = new HashMap<>(response.getFieldTypes().size());
            response.getFieldTypes().forEach(t -> map.put((String) t.getAttributes().get(FieldType.TYPE_NAME), t));
            schema.types = map;
        }

        return schema.types;
    }

    /**
     * Retrieve all information about existing fields of the core.
     *
     * @param core the core to search in
     * @param force if {@code true} reloads all information, else gets the information from the cache
     * @return the map of all fields
     * @throws SolrException in case of problem to request fields information
     * @since 16.2.0RC1
     */
    public Map<String, Map<String, Object>> getFields(XWikiSolrCore core, boolean force) throws SolrException
    {
        SolrCoreSchema schema = getSchema(core);
        if (schema.fields == null || force) {
            SchemaResponse.FieldsResponse response;
            try {
                response = new SchemaRequest.Fields().process(core.getClient());
            } catch (Exception e) {
                throw new SolrException("Failed to get the list of fields", e);
            }

            Map<String, Map<String, Object>> map = new ConcurrentHashMap<>(response.getFields().size());
            response.getFields().forEach(e -> map.put((String) e.get(SOLR_FIELD_NAME), e));
            schema.fields = map;
        }

        return schema.fields;
    }

    /**
     * Retrieve all information about existing dynamic fields of the core.
     *
     * @param core the core to search in
     * @param force if {@code true} reloads all information, else gets the information from the cache
     * @return the map of all dynamic fields
     * @throws SolrException in case of problem to request fields information
     */
    public Map<String, Map<String, Object>> getDynamicFields(XWikiSolrCore core, boolean force) throws SolrException
    {
        SolrCoreSchema schema = getSchema(core);

        if (schema.dynamicFields == null || force) {
            SchemaResponse.DynamicFieldsResponse response;
            try {
                response = new SchemaRequest.DynamicFields().process(core.getClient());
            } catch (Exception e) {
                throw new SolrException("Failed to get the list of dynamic fields", e);
            }

            Map<String, Map<String, Object>> map = new ConcurrentHashMap<>(response.getDynamicFields().size());
            response.getDynamicFields().forEach(e -> map.put((String) e.get(SOLR_FIELD_NAME), e));
            schema.dynamicFields = map;
        }

        return schema.dynamicFields;
    }

    /**
     * Retrieve all information about existing dynamic fields of the core.
     *
     * @param core the core to search in
     * @param force if {@code true} reloads all information, else gets the information from the cache
     * @return the map of all dynamic fields
     * @throws SolrException in case of problem to request fields information
     */
    public Map<String, Set<String>> getCopyFields(XWikiSolrCore core, boolean force) throws SolrException
    {
        SolrCoreSchema schema = getSchema(core);

        if (schema.copyFields == null || force) {
            SchemaResponse.CopyFieldsResponse response;
            try {
                response = new SchemaRequest.CopyFields().process(core.getClient());
            } catch (Exception e) {
                throw new SolrException("Failed to get the list of copy fields", e);
            }

            Map<String, Set<String>> map = new ConcurrentHashMap<>(response.getCopyFields().size());
            for (Map<String, Object> fields : response.getCopyFields()) {
                Set<String> destinations =
                    map.computeIfAbsent((String) fields.get("source"), k -> ConcurrentHashMap.newKeySet());
                destinations.add((String) fields.get("dest"));
            }
            schema.copyFields = map;
        }

        return schema.copyFields;
    }

    /**
     * Add a field type in the Solr schema.
     * 
     * @param core the core to update
     * @param definition the definition of the field to add
     * @throws SolrException when failing to add the field
     * @since 16.2.0RC1
     */
    public void setFieldType(XWikiSolrCore core, FieldTypeDefinition definition) throws SolrException
    {
        setFieldType(core, definition, null);
    }

    /**
     * Add a field type in the Solr schema.
     * 
     * @param core the core to update
     * @param definition the definition of the field to add
     * @param add true or false to explicitly indicate if it's a new field, null to find automatically
     * @throws SolrException when failing to add the field
     * @since 17.8.0RC1
     */
    public void setFieldType(XWikiSolrCore core, FieldTypeDefinition definition, Boolean add) throws SolrException
    {
        String filedName = (String) definition.getAttributes().get(FieldType.TYPE_NAME);

        Map<String, FieldTypeRepresentation> filedTypes = getFieldTypes(core, false);

        try {
            if ((add == null && filedTypes.get(filedName) == null) || (add != null && add.equals(Boolean.TRUE))) {
                new SchemaRequest.AddFieldType(definition).process(core.getClient());
            } else {
                new SchemaRequest.ReplaceFieldType(definition).process(core.getClient());
            }
        } catch (Exception e) {
            throw new SolrException("Failed to add a field type in the Solr core", e);
        }

        // Add it to the cache
        FieldTypeRepresentation representation = new FieldTypeRepresentation();
        representation.setAttributes(definition.getAttributes());
        representation.setAnalyzer(definition.getAnalyzer());
        representation.setIndexAnalyzer(definition.getIndexAnalyzer());
        representation.setMultiTermAnalyzer(definition.getMultiTermAnalyzer());
        representation.setQueryAnalyzer(definition.getQueryAnalyzer());
        representation.setSimilarity(definition.getSimilarity());

        filedTypes.put(filedName, representation);
    }

    /**
     * Add or replace a field in the Solr schema.
     *
     * @param core the core to update
     * @param fieldAttributes the attributes of the field to add
     * @param dynamic true to create a dynamic field
     * @throws SolrException when failing to add the field
     */
    public void setField(XWikiSolrCore core, Map<String, Object> fieldAttributes, boolean dynamic) throws SolrException
    {
        String name = (String) fieldAttributes.get(SOLR_FIELD_NAME);
        if (dynamic) {
            setField(core, fieldAttributes, dynamic, !getDynamicFields(core, false).containsKey(name));
        } else {
            setField(core, fieldAttributes, dynamic, !getFields(core, false).containsKey(name));
        }
    }

    /**
     * Add or replace a field in the Solr schema.
     *
     * @param core the core to update
     * @param fieldAttributes the attributes of the field to add
     * @param dynamic true to create a dynamic field
     * @param add true if the field type should be added, false for replace
     * @throws SolrException when failing to add the field
     * @since 16.2.0RC1
     */
    public void setField(XWikiSolrCore core, Map<String, Object> fieldAttributes, boolean dynamic, boolean add)
        throws SolrException
    {
        String name = (String) fieldAttributes.get(SOLR_FIELD_NAME);

        try {
            if (dynamic) {
                if (add) {
                    new SchemaRequest.AddDynamicField(fieldAttributes).process(core.getClient());
                } else {
                    new SchemaRequest.ReplaceDynamicField(fieldAttributes).process(core.getClient());
                }

                // Add it to the cache
                getDynamicFields(core, false).put(name, fieldAttributes);
            } else {
                if (add) {
                    new SchemaRequest.AddField(fieldAttributes).process(core.getClient());
                } else {
                    new SchemaRequest.ReplaceField(fieldAttributes).process(core.getClient());
                }

                // Add it to the cache
                getFields(core, false).put(name, fieldAttributes);
            }
        } catch (Exception e) {
            throw new SolrException(
                String.format("Failed to set the field [%s] in the Solr core (dynamic: [%s])", name, dynamic), e);
        }
    }

    /**
     * Add or replace a field in the Solr schema.
     *
     * @param core the core to update
     * @param name the name of the field to set
     * @param type the type of the field to set
     * @param dynamic true to create a dynamic field
     * @param attributes attributed to add to the field definition
     * @throws SolrException when failing to add the field
     */
    public void setField(XWikiSolrCore core, String name, String type, boolean dynamic, Object... attributes)
        throws SolrException
    {
        Map<String, Object> fieldAttributes = new HashMap<>();
        fieldAttributes.put(SOLR_FIELD_NAME, name);
        fieldAttributes.put(FieldType.TYPE, type);

        MapUtils.putAll(fieldAttributes, attributes);

        setField(core, fieldAttributes, dynamic);
    }

    /**
     * Add or replace a field in the Solr schema.
     *
     * @param core the core to update
     * @param name the name of the field to set
     * @param type the type of the field to set
     * @param dynamic true to create a dynamic field
     * @param add true if the field type should be added, false for replace
     * @param attributes attributed to add to the field definition
     * @throws SolrException when failing to add the field
     */
    public void setField(XWikiSolrCore core, String name, String type, boolean dynamic, boolean add,
        Object... attributes) throws SolrException
    {
        Map<String, Object> fieldAttributes = new HashMap<>();
        fieldAttributes.put(SOLR_FIELD_NAME, name);
        fieldAttributes.put(FieldType.TYPE, type);

        MapUtils.putAll(fieldAttributes, attributes);

        setField(core, fieldAttributes, dynamic, add);
    }

    /**
     * Add or replace a field in the Solr schema.
     * <p>
     * String (UTF-8 encoded string or Unicode). Strings are intended for small fields and are not tokenized or analyzed
     * in any way. They have a hard limit of slightly less than 32K.
     * 
     * @param core the core to update
     * @param name the name of the field to set
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @param attributes attributed to add to the field definition
     * @throws SolrException when failing to set the field
     * @since 17.8.0RC1
     */
    public void setStringField(XWikiSolrCore core, String name, boolean multiValued, boolean dynamic,
        Object... attributes) throws SolrException
    {
        setField(core, name, multiValued ? DefaultSolrUtils.SOLR_TYPE_STRINGS : DefaultSolrUtils.SOLR_TYPE_STRING,
            dynamic, attributes);
    }

    /**
     * Add or replace a field in the Solr schema.
     * <p>
     * A general text field that has reasonable, generic cross-language defaults: it tokenizes with StandardTokenizer,
     * removes stop words from case-insensitive "stopwords.txt" (empty by default), and down cases. At query time only,
     * it also applies synonyms.
     * 
     * @param core the core to update
     * @param name the name of the field to set
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @param attributes attributed to add to the field definition
     * @throws SolrException when failing to set the field
     * @since 17.8.0RC1
     */
    public void setTextGeneralField(XWikiSolrCore core, String name, boolean multiValued, boolean dynamic,
        Object... attributes) throws SolrException
    {
        setField(core, name,
            multiValued ? DefaultSolrUtils.SOLR_TYPE_TEXT_GENERALS : DefaultSolrUtils.SOLR_TYPE_TEXT_GENERAL, dynamic,
            attributes);
    }

    /**
     * Add or replace a field in the Solr schema.
     * <p>
     * Contains either true or false. Values of "1", "t", or "T" in the first character are interpreted as true. Any
     * other values in the first character are interpreted as false.
     * 
     * @param core the core to update
     * @param name the name of the field to set
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @param attributes attributed to add to the field definition
     * @throws SolrException when failing to set the field
     * @since 17.8.0RC1
     */
    public void setBooleanField(XWikiSolrCore core, String name, boolean multiValued, boolean dynamic,
        Object... attributes) throws SolrException
    {
        setField(core, name, multiValued ? DefaultSolrUtils.SOLR_TYPE_BOOLEANS : DefaultSolrUtils.SOLR_TYPE_BOOLEAN,
            dynamic, attributes);
    }

    /**
     * Add or replace a field in the Solr schema.
     * <p>
     * Integer field (32-bit signed integer).
     * 
     * @param core the core to update
     * @param name the name of the field to set
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @param attributes attributed to add to the field definition
     * @throws SolrException when failing to set the field
     * @since 17.8.0RC1
     */
    public void setPIntField(XWikiSolrCore core, String name, boolean multiValued, boolean dynamic,
        Object... attributes) throws SolrException
    {
        setField(core, name, multiValued ? DefaultSolrUtils.SOLR_TYPE_PINTS : DefaultSolrUtils.SOLR_TYPE_PINT, dynamic,
            attributes);
    }

    /**
     * Add or replace a field in the Solr schema.
     * <p>
     * Floating point field (32-bit IEEE floating point).
     * 
     * @param core the core to update
     * @param name the name of the field to set
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @param attributes attributed to add to the field definition
     * @throws SolrException when failing to set the field
     * @since 17.8.0RC1
     */
    public void setPFloatField(XWikiSolrCore core, String name, boolean multiValued, boolean dynamic,
        Object... attributes) throws SolrException
    {
        setField(core, name, multiValued ? DefaultSolrUtils.SOLR_TYPE_PFLOATS : DefaultSolrUtils.SOLR_TYPE_PFLOAT,
            dynamic, attributes);
    }

    /**
     * Add or replace a field in the Solr schema.
     * <p>
     * Long field (64-bit signed integer).
     * 
     * @param core the core to update
     * @param name the name of the field to set
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @param attributes attributed to add to the field definition
     * @throws SolrException when failing to set the field
     * @since 17.8.0RC1
     */
    public void setPLongField(XWikiSolrCore core, String name, boolean multiValued, boolean dynamic,
        Object... attributes) throws SolrException
    {
        setField(core, name, multiValued ? DefaultSolrUtils.SOLR_TYPE_PLONGS : DefaultSolrUtils.SOLR_TYPE_PLONG,
            dynamic, attributes);
    }

    /**
     * Add or replace a field in the Solr schema.
     * <p>
     * Double field (64-bit IEEE floating point).
     * 
     * @param core the core to update
     * @param name the name of the field to set
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @param attributes attributed to add to the field definition
     * @throws SolrException when failing to set the field
     * @since 17.8.0RC1
     */
    public void setPDoubleField(XWikiSolrCore core, String name, boolean multiValued, boolean dynamic,
        Object... attributes) throws SolrException
    {
        setField(core, name, multiValued ? DefaultSolrUtils.SOLR_TYPE_PDOUBLES : DefaultSolrUtils.SOLR_TYPE_PDOUBLE,
            dynamic, attributes);
    }

    /**
     * Add or replace a field in the Solr schema.
     * <p>
     * Date field. Represents a point in time with millisecond precision.
     * 
     * @param core the core to update
     * @param name the name of the field to set
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @param attributes attributed to add to the field definition
     * @throws SolrException when failing to set the field
     * @since 17.8.0RC1
     */
    public void setPDateField(XWikiSolrCore core, String name, boolean multiValued, boolean dynamic,
        Object... attributes) throws SolrException
    {
        setField(core, name, multiValued ? DefaultSolrUtils.SOLR_TYPE_PDATES : DefaultSolrUtils.SOLR_TYPE_PDATE,
            dynamic, attributes);
    }

    /**
     * Add or replace a field in the Solr schema.
     * <p>
     * Binary data.
     * 
     * @param core the core to update
     * @param name the name of the field to set
     * @param dynamic true to create a dynamic field
     * @param attributes attributed to add to the field definition
     * @throws SolrException when failing to set the field
     * @since 17.8.0RC1
     */
    public void setBinaryField(XWikiSolrCore core, String name, boolean dynamic, Object... attributes)
        throws SolrException
    {
        setField(core, name, DefaultSolrUtils.SOLR_TYPE_BINARY, dynamic, attributes);
    }

    /**
     * @param core the core to update
     * @param name the name of the field to delete
     * @param dynamic true to delete a dynamic field
     * @throws SolrException when failing to delete the field
     * @since 16.2.0RC1
     */
    public void deleteField(XWikiSolrCore core, String name, boolean dynamic) throws SolrException
    {
        try {
            if (dynamic) {
                new SchemaRequest.DeleteDynamicField(name).process(core.getClient());
            } else {
                new SchemaRequest.DeleteField(name).process(core.getClient());
            }

            // Remove the field from the cache
            getFields(core, false).remove(name);
        } catch (Exception e) {
            throw new SolrException(String.format("Failed to delete the field [%s] (dynamic: [%s])", name, dynamic), e);
        }
    }

    /**
     * Add a copy field.
     * 
     * @param core the core to update
     * @param source the source field name
     * @param destinations the collection of the destination field names
     * @throws SolrException when failing to add the field
     * @since 16.2.0RC1
     */
    public void addCopyField(XWikiSolrCore core, String source, String... destinations) throws SolrException
    {
        addCopyField(core, source, List.of(destinations));
    }

    /**
     * Add a copy field.
     * 
     * @param core the core to update
     * @param source the source field name
     * @param destinations the collection of the destination field names
     * @throws SolrException when failing to add the field
     * @since 16.2.0RC1
     */
    public void addCopyField(XWikiSolrCore core, String source, List<String> destinations) throws SolrException
    {
        try {
            new SchemaRequest.AddCopyField(source, destinations).process(core.getClient());

            // Add it to the cache
            getCopyFields(core, false).computeIfAbsent(source, k -> ConcurrentHashMap.newKeySet()).addAll(destinations);
        } catch (Exception e) {
            throw new SolrException("Failed to add a copy field in the Solr core", e);
        }
    }

    /**
     * Performs an explicit commit, causing pending documents to be committed for indexing.
     *
     * @param core the core to commit
     * @throws SolrException when failing to commit
     */
    public void commit(XWikiSolrCore core) throws SolrException
    {
        try {
            core.getClient().commit();
        } catch (Exception e) {
            throw new SolrException("Failed to commit", e);
        }

        // Reset the cache
        getSchema(core).reset();
    }

    private FieldTypeRepresentation getFieldType(XWikiSolrCore core, String name) throws SolrException
    {
        return getFieldTypes(core, false).get(name);
    }

    /**
     * @param core the core
     * @param name the name of the version
     * @return the version, or null if no value could be found for the provided version name
     * @throws SolrException when failing to get the version
     * @since 17.8.0RC1
     */
    public Long getVersion(XWikiSolrCore core, String name) throws SolrException
    {
        FieldTypeRepresentation fieldType = getFieldType(core, name);

        if (fieldType == null) {
            return null;
        }

        String value = (String) fieldType.getAttributes().get(SolrSchemaUtils.SOLR_VERSIONFIELDTYPE_VALUE);

        return NumberUtils.createLong(value);
    }

    /**
     * Add a field type in the Solr schema.
     * 
     * @param core the core
     * @param attributes the attributes of the field to add
     * @throws SolrException when failing to add the field
     * @since 17.8.0RC1
     */
    public void setFieldType(XWikiSolrCore core, Map<String, Object> attributes) throws SolrException
    {
        setFieldType(core, attributes, null);
    }

    /**
     * Add a field type in the Solr schema.
     * 
     * @param core the core
     * @param attributes the attributes of the field to add
     * @param add true or false to explicitly indicate if it's a new field, null to find automatically
     * @throws SolrException when failing to add the field
     * @since 17.8.0RC1
     */
    public void setFieldType(XWikiSolrCore core, Map<String, Object> attributes, Boolean add) throws SolrException
    {
        FieldTypeDefinition definition = new FieldTypeDefinition();
        definition.setAttributes(attributes);

        setFieldType(core, definition, add);
    }

    /**
     * Add a field type in the Solr schema.
     * 
     * @param core the core
     * @param name the name of the field type
     * @param solrClass the class of the field type
     * @param attributes the other attributes of the field type
     * @throws SolrException when failing to add the field
     * @since 17.8.0RC1
     */
    public void setFieldType(XWikiSolrCore core, String name, String solrClass, Object... attributes)
        throws SolrException
    {
        setFieldType(core, name, solrClass, null, attributes);
    }

    /**
     * Add a field type in the Solr schema.
     * 
     * @param core the core
     * @param name the name of the field type
     * @param solrClass the class of the field type
     * @param add true or false to explicitly indicate if it's a new field, null to find automatically
     * @param attributes the other attributes of the field type
     * @throws SolrException when failing to add the field
     * @since 17.8.0RC1
     */
    public void setFieldType(XWikiSolrCore core, String name, String solrClass, Boolean add, Object... attributes)
        throws SolrException
    {
        Map<String, Object> attributesMap = new HashMap<>(2 + (attributes.length > 0 ? attributes.length / 2 : 0));

        attributesMap.put(FieldType.TYPE_NAME, name);
        attributesMap.put(FieldType.CLASS_NAME, solrClass);

        MapUtils.putAll(attributesMap, attributes);

        setFieldType(core, attributesMap);
    }

    /**
     * @param core the core
     * @param name the name of the version
     * @param version the version value
     * @throws SolrException when failing to set the version
     * @since 17.8.0RC1
     */
    public void setVersion(XWikiSolrCore core, String name, long version) throws SolrException
    {
        setFieldType(core, name, "solr.ExternalFileField", SolrSchemaUtils.SOLR_VERSIONFIELDTYPE_VALUE,
            String.valueOf(version));
    }

    /**
     * @param core the core
     * @param name the name of the version
     * @param version the version value
     * @param add true or false to explicitly indicate if it's a new field, null to find automatically
     * @throws SolrException when failing to set the version
     * @since 17.8.0RC1
     */
    public void setVersion(XWikiSolrCore core, String name, long version, Boolean add) throws SolrException
    {
        setFieldType(core, name, "solr.ExternalFileField", SolrSchemaUtils.SOLR_VERSIONFIELDTYPE_VALUE,
            String.valueOf(version), add);
    }
}
