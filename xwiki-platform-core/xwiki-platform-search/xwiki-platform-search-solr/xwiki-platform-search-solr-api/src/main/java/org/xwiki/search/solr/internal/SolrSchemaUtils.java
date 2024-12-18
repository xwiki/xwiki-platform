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
     * @param add true if the field type should be added, false for replace
     * @throws SolrException when failing to add the field
     * @since 16.2.0RC1
     */
    public void setFieldType(XWikiSolrCore core, FieldTypeDefinition definition, boolean add) throws SolrException
    {
        SolrCoreSchema schema = getSchema(core);

        try {
            if (add) {
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

        getFieldTypes(core, false).put((String) definition.getAttributes().get(FieldType.TYPE_NAME), representation);
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
}
