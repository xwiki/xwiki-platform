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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.MapUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.schema.FieldType;
import org.xwiki.component.annotation.Component;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;

@Component(roles = SolrSchemaUtils.class)
@Singleton
public class SolrSchemaUtils
{
    /**
     * The name of the attribute containing the name of the Solr field.
     */
    private static final String SOLR_FIELD_NAME = "name";

    private class SolrCoreSchema
    {
        private final String coreName;
        SolrClient solrClient;
        Map<String, Map<String, Object>> fields;

        public SolrCoreSchema(String name)
        {
            this.coreName = name;
        }
    }

    private final Map<String, SolrCoreSchema> coreSchemaMap = new HashMap<>();

    @Inject
    private Solr solr;

    private SolrCoreSchema getSchema(String coreName)
    {
        return this.coreSchemaMap.computeIfAbsent(coreName, SolrCoreSchema::new);
    }

    private SolrClient getClient(String coreName) throws SolrException
    {
        SolrCoreSchema schema = getSchema(coreName);
        if (schema.solrClient == null) {
            schema.solrClient = this.solr.getClient(coreName);
        }
        return schema.solrClient;
    }

    public void setClient(String coreName, SolrClient solrClient)
    {
        getSchema(coreName).solrClient = solrClient;
    }

    public Map<String, Map<String, Object>> getFields(String coreName, boolean force) throws SolrException
    {
        SolrCoreSchema schema = getSchema(coreName);
        if (schema.fields == null || force) {
            SchemaResponse.FieldsResponse response;
            try {
                response = new SchemaRequest.Fields().process(getClient(coreName));
            } catch (Exception e) {
                throw new SolrException("Failed to get the list of fields", e);
            }

            Map<String, Map<String, Object>> map = new HashMap<>(response.getFields().size());
            response.getFields().forEach(e -> map.put((String) e.get(SOLR_FIELD_NAME), e));
            schema.fields = map;
        }

        // Return a copy to avoid concurrency issues, in particular in case of commits
        return new HashMap<>(schema.fields);
    }

    /**
     * Add or replace a field in the Solr schema.
     *
     * @param fieldAttributes the attributes of the field to add
     * @param dynamic true to create a dynamic field
     * @throws SolrException when failing to add the field
     * @since 12.5RC1
     */
    public void setField(String coreName, Map<String, Object> fieldAttributes, boolean dynamic)
        throws SolrException
    {
        String name = (String) fieldAttributes.get(SOLR_FIELD_NAME);
        try {
            SolrClient solrClient = getClient(coreName);
            if (getFields(coreName, false).containsKey(name)) {
                if (dynamic) {
                    new SchemaRequest.ReplaceDynamicField(fieldAttributes).process(solrClient);
                } else {
                    new SchemaRequest.ReplaceField(fieldAttributes).process(solrClient);
                }
            } else {
                if (dynamic) {
                    new SchemaRequest.AddDynamicField(fieldAttributes).process(solrClient);
                } else {
                    new SchemaRequest.AddField(fieldAttributes).process(solrClient);
                }
            }
            // Add it to the cache
            getSchema(coreName).fields.put(name, fieldAttributes);
        } catch (Exception e) {
            throw new SolrException("Failed to set a field in the Solr core", e);
        }
    }

    /**
     * Add or replace a field in the Solr schema.
     *
     * @param name the name of the field to set
     * @param type the type of the field to set
     * @param dynamic true to create a dynamic field
     * @param attributes attributed to add to the field definition
     * @throws SolrException when failing to add the field
     * @since 12.5RC1
     */
    public void setField(String coreName, String name, String type, boolean dynamic, Object... attributes)
        throws SolrException
    {
        Map<String, Object> fieldAttributes = new HashMap<>();
        fieldAttributes.put(SOLR_FIELD_NAME, name);
        fieldAttributes.put(FieldType.TYPE, type);

        MapUtils.putAll(fieldAttributes, attributes);

        setField(coreName, fieldAttributes, dynamic);
    }

    public void commit(String coreName) throws SolrException
    {
        try {
            getClient(coreName).commit();
        } catch (Exception e) {
            throw new SolrException("Failed to commit", e);
        }
        // Reset the cache
        getSchema(coreName).fields = null;
    }
}
