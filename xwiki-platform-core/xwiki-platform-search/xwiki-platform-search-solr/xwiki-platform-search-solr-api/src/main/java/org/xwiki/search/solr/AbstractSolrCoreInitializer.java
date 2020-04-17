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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.schema.BinaryField;
import org.apache.solr.schema.BoolField;
import org.apache.solr.schema.DatePointField;
import org.apache.solr.schema.DoublePointField;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.FloatPointField;
import org.apache.solr.schema.IntPointField;
import org.apache.solr.schema.LongPointField;
import org.apache.solr.schema.StrField;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.search.solr.internal.DefaultSolrUtils;
import org.xwiki.stability.Unstable;

/**
 * Base helper class to implement {@link SolrCoreInitializer}.
 * 
 * @version $Id$
 * @since 12.3RC1
 */
@Unstable
public abstract class AbstractSolrCoreInitializer implements SolrCoreInitializer
{
    /**
     * The name of the field containing the identifier of the document.
     */
    public static final String SOLR_FIELD_ID = "id";

    /**
     * The base schema version for XWiki 12.3.
     */
    public static final long SCHEMA_VERSION_12_3 = 120300000;

    /**
     * The base schema version.
     */
    public static final long SCHEMA_BASE_VERSION = SCHEMA_VERSION_12_3;

    /**
     * The name of the attribute containing the name of the Solr field.
     */
    protected static final String SOLR_FIELD_NAME = "name";

    /**
     * The name of the attribute indicating if the Solr field should be indexed.
     */
    protected static final String SOLR_FIELD_INDEXED = "indexed";

    /**
     * The name of the attribute indicating if the Solr field should be stored.
     */
    protected static final String SOLR_FIELD_STORED = "stored";

    /**
     * The name of the attribute Control the placement of documents when a sort field is not present.
     */
    protected static final String SOLR_FIELD_SORTMISSINGLAST = "sortMissingLast";

    /**
     * The name of the attribute indicating that a single document might contain multiple values for this Solr field
     * type.
     */
    protected static final String SOLR_FIELD_MULTIVALUED = "multiValued";

    /**
     * The name of the attribute indicating if the value of the field will be put in a column-oriented DocValues
     * structure.
     */
    protected static final String SOLR_FIELD_DOCVALUES = "docValues";

    private static final String SOLR_TYPENAME_XVERSION = "__xversion";

    private static final String SOLR_TYPENAME_CVERSION = "__cversion";

    private static final String SOLR_VERSIONFIELDTYPE_VALUE = "defVal";

    @Inject
    protected ComponentDescriptor<SolrCoreInitializer> descriptor;

    protected SolrClient client;

    protected Map<String, FieldTypeRepresentation> types;

    @Override
    public void initialize(SolrClient client) throws SolrException
    {
        this.client = client;

        // Make sure the base schema (mostly default types) in in sync with this version of XWiki
        initializeBaseSchema();

        // Make sure this core schema is created/migrated if needed
        initializeCoreSchema();

        // Commit changes (if any)
        commit();
    }

    protected Map<String, FieldTypeRepresentation> getFieldTypes(boolean force) throws SolrException
    {
        if (this.types == null || force) {
            SchemaResponse.FieldTypesResponse response;
            try {
                response = new SchemaRequest.FieldTypes().process(this.client);
            } catch (Exception e) {
                throw new SolrException("Failed to get the list of field types", e);
            }

            Map<String, FieldTypeRepresentation> map = new HashMap<>(response.getFieldTypes().size());

            response.getFieldTypes().forEach(t -> map.put((String) t.getAttributes().get(FieldType.TYPE_NAME), t));

            this.types = map;
        }

        return this.types;
    }

    protected void initializeBaseSchema() throws SolrException
    {
        Long xversion = getCurrentXWikiVersion();

        if (xversion == null) {
            // No xversion means it's the old version which only contained the id field and string field type
            // Inspired from _default Solr configset
            // http://github.com/apache/lucene-solr/blob/master/solr/server/solr/configsets/_default/conf/managed-schema

            //////////
            // TYPES
            //////////

            addFieldType(DefaultSolrUtils.SOLR_TYPE_STRINGS, StrField.class.getName(), SOLR_FIELD_SORTMISSINGLAST, true,
                SOLR_FIELD_MULTIVALUED, true, SOLR_FIELD_DOCVALUES, true);

            addFieldType(DefaultSolrUtils.SOLR_TYPE_BOOLEAN, BoolField.class.getName(), SOLR_FIELD_SORTMISSINGLAST,
                true);
            addFieldType(DefaultSolrUtils.SOLR_TYPE_BOOLEANS, BoolField.class.getName(), SOLR_FIELD_SORTMISSINGLAST,
                true, SOLR_FIELD_MULTIVALUED, true);

            // Numeric field types that index values using KD-trees.
            // Point fields don't support FieldCache, so they must have docValues="true" if needed for sorting,
            // faceting, functions, etc.
            addFieldType(DefaultSolrUtils.SOLR_TYPE_PINT, IntPointField.class.getName(), SOLR_FIELD_DOCVALUES, true);
            addFieldType(DefaultSolrUtils.SOLR_TYPE_PFLOAT, FloatPointField.class.getName(), SOLR_FIELD_DOCVALUES,
                true);
            addFieldType(DefaultSolrUtils.SOLR_TYPE_PLONG, LongPointField.class.getName(), SOLR_FIELD_DOCVALUES, true);
            addFieldType(DefaultSolrUtils.SOLR_TYPE_PDOUBLE, DoublePointField.class.getName(), SOLR_FIELD_DOCVALUES,
                true);

            addFieldType(DefaultSolrUtils.SOLR_TYPE_PINTS, IntPointField.class.getName(), SOLR_FIELD_DOCVALUES, true,
                SOLR_FIELD_MULTIVALUED, true);
            addFieldType(DefaultSolrUtils.SOLR_TYPE_PFLOATS, FloatPointField.class.getName(), SOLR_FIELD_DOCVALUES,
                true, SOLR_FIELD_MULTIVALUED, true);
            addFieldType(DefaultSolrUtils.SOLR_TYPE_PLONGS, LongPointField.class.getName(), SOLR_FIELD_DOCVALUES, true,
                SOLR_FIELD_MULTIVALUED, true);
            addFieldType(DefaultSolrUtils.SOLR_TYPE_PDOUBLES, DoublePointField.class.getName(), SOLR_FIELD_DOCVALUES,
                true, SOLR_FIELD_MULTIVALUED, true);

            // Since fields of this type are by default not stored or indexed, any data added to them will be ignored
            // outright
            addFieldType("ignored", "solr.StrField", SOLR_FIELD_STORED, false, SOLR_FIELD_INDEXED, false,
                SOLR_FIELD_MULTIVALUED, true);

            addFieldType(DefaultSolrUtils.SOLR_TYPE_PDATE, DatePointField.class.getName(), SOLR_FIELD_DOCVALUES, true);
            addFieldType(DefaultSolrUtils.SOLR_TYPE_PDATES, DatePointField.class.getName(), SOLR_FIELD_DOCVALUES, true,
                SOLR_FIELD_MULTIVALUED, true);

            // Binary data type. The data should be sent/retrieved in as Base64 encoded Strings
            addFieldType(DefaultSolrUtils.SOLR_TYPE_BINARY, BinaryField.class.getName());

            //////////
            // FIELDS
            //////////

            // Used for partial update
            // docValues are enabled by default for long type so we don't need to index the version field
            addField("_version_", DefaultSolrUtils.SOLR_TYPE_PLONG, false, SOLR_FIELD_INDEXED, false, SOLR_FIELD_STORED,
                false);

            // Save scheme version
            setCurrentXWikiVersion(true);
        } else {
            // Update version
            setCurrentXWikiVersion(false);
        }
    }

    protected void initializeCoreSchema() throws SolrException
    {
        Long cversion = getCurrentCoreVersion();

        if (cversion == null) {
            // Create the core schema from scratch
            createSchema();

            // Update version
            setCurrentCoreVersion(true);
        } else if (cversion.longValue() != getVersion()) {
            // Migrate the existing core schema
            migrateSchema(cversion);

            // Update version
            setCurrentCoreVersion(false);
        }
    }

    /**
     * Create the schema from scratch.
     * 
     * @throws SolrException when failing to create the schema
     */
    protected abstract void createSchema() throws SolrException;

    /**
     * Migrate the existing schema.
     * 
     * @param cversion the version of the schema currently stored
     * @throws SolrException when failing to migrate the schema
     */
    protected abstract void migrateSchema(long cversion) throws SolrException;

    @Override
    public String getCoreName()
    {
        return this.descriptor.getRoleHint();
    }

    /**
     * @return the version of this core's schema according to current specifications
     */
    protected abstract long getVersion();

    /**
     * @return the version of the base schema currently stored in Solr
     * @throws SolrException when failing to get the type
     */
    protected Long getCurrentXWikiVersion() throws SolrException
    {
        return getVersion(SOLR_TYPENAME_XVERSION);
    }

    protected void setCurrentXWikiVersion(boolean add) throws SolrException
    {
        setVersion(SOLR_TYPENAME_XVERSION, SCHEMA_BASE_VERSION, add);
    }

    /**
     * @return the version of the schema as defined by this initializer currently stored in Solr
     * @throws SolrException when failing to get the type
     */
    protected Long getCurrentCoreVersion() throws SolrException
    {
        return getVersion(SOLR_TYPENAME_CVERSION);
    }

    protected void setCurrentCoreVersion(boolean add) throws SolrException
    {
        setVersion(SOLR_TYPENAME_CVERSION, getVersion(), add);
    }

    private Long getVersion(String name) throws SolrException
    {
        FieldTypeRepresentation fieldType = getFieldType(name);

        if (fieldType == null) {
            return null;
        }

        String value = (String) fieldType.getAttributes().get(SOLR_VERSIONFIELDTYPE_VALUE);

        return NumberUtils.createLong(value);
    }

    private FieldTypeRepresentation getFieldType(String name) throws SolrException
    {
        return getFieldTypes(false).get(name);
    }

    private void setVersion(String name, long version, boolean add) throws SolrException
    {
        setFieldType(name, "solr.ExternalFileField", add, SOLR_VERSIONFIELDTYPE_VALUE, String.valueOf(version));
    }

    /**
     * Add a field in the Solr schema.
     * <p>
     * String (UTF-8 encoded string or Unicode). Strings are intended for small fields and are not tokenized or analyzed
     * in any way. They have a hard limit of slightly less than 32K.
     * 
     * @param name the name of the field to add
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @throws SolrException when failing to add the field
     */
    protected void addStringField(String name, boolean multiValued, boolean dynamic) throws SolrException
    {
        addField(name, multiValued ? DefaultSolrUtils.SOLR_TYPE_STRINGS : DefaultSolrUtils.SOLR_TYPE_STRING, dynamic);
    }

    /**
     * Add a field in the Solr schema.
     * <p>
     * Contains either true or false. Values of "1", "t", or "T" in the first character are interpreted as true. Any
     * other values in the first character are interpreted as false.
     * 
     * @param name the name of the field to add
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @throws SolrException when failing to add the field
     */
    protected void addBooleanField(String name, boolean multiValued, boolean dynamic) throws SolrException
    {
        addField(name, multiValued ? DefaultSolrUtils.SOLR_TYPE_BOOLEANS : DefaultSolrUtils.SOLR_TYPE_BOOLEAN, dynamic);
    }

    /**
     * Add a field in the Solr schema.
     * <p>
     * Integer field (32-bit signed integer).
     * 
     * @param name the name of the field to add
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @throws SolrException when failing to add the field
     */
    protected void addPIntField(String name, boolean multiValued, boolean dynamic) throws SolrException
    {
        addField(name, multiValued ? DefaultSolrUtils.SOLR_TYPE_PINTS : DefaultSolrUtils.SOLR_TYPE_PINT, dynamic);
    }

    /**
     * Add a field in the Solr schema.
     * <p>
     * Floating point field (32-bit IEEE floating point).
     * 
     * @param name the name of the field to add
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @throws SolrException when failing to add the field
     */
    protected void addPFloatField(String name, boolean multiValued, boolean dynamic) throws SolrException
    {
        addField(name, multiValued ? DefaultSolrUtils.SOLR_TYPE_PFLOATS : DefaultSolrUtils.SOLR_TYPE_PFLOAT, dynamic);
    }

    /**
     * Add a field in the Solr schema.
     * <p>
     * Long field (64-bit signed integer).
     * 
     * @param name the name of the field to add
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @throws SolrException when failing to add the field
     */
    protected void addPLongField(String name, boolean multiValued, boolean dynamic) throws SolrException
    {
        addField(name, multiValued ? DefaultSolrUtils.SOLR_TYPE_PLONGS : DefaultSolrUtils.SOLR_TYPE_PLONG, dynamic);
    }

    /**
     * Add a field in the Solr schema.
     * <p>
     * Double field (64-bit IEEE floating point).
     * 
     * @param name the name of the field to add
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @throws SolrException when failing to add the field
     */
    protected void addPDoubleField(String name, boolean multiValued, boolean dynamic) throws SolrException
    {
        addField(name, multiValued ? DefaultSolrUtils.SOLR_TYPE_PDOUBLES : DefaultSolrUtils.SOLR_TYPE_PDOUBLE, dynamic);
    }

    /**
     * Add a field in the Solr schema.
     * <p>
     * Date field. Represents a point in time with millisecond precision.
     * 
     * @param name the name of the field to add
     * @param multiValued true if the field can contain several values
     * @param dynamic true to create a dynamic field
     * @throws SolrException when failing to add the field
     */
    protected void addPDateField(String name, boolean multiValued, boolean dynamic) throws SolrException
    {
        addField(name, multiValued ? DefaultSolrUtils.SOLR_TYPE_PDATES : DefaultSolrUtils.SOLR_TYPE_PDATE, dynamic);
    }

    /**
     * Binary data.
     * 
     * @param name the name of the field to add
     * @param dynamic true to create a dynamic field
     * @throws SolrException when failing to add the field
     */
    protected void addBinaryField(String name, boolean dynamic) throws SolrException
    {
        addField(name, DefaultSolrUtils.SOLR_TYPE_BINARY, dynamic);
    }

    /**
     * Binary data.
     * 
     * @param name the name of the field to add
     * @throws SolrException when failing to add the field
     */
    protected void addMapField(String name) throws SolrException
    {
        addStringField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_STRING), false, true);
        addStringField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_STRINGS), true, true);
        addBooleanField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_BOOLEAN), false, true);
        addBooleanField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_BOOLEANS), true, true);
        addPIntField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_PINT), false, true);
        addPIntField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_PINTS), true, true);
        addPFloatField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_PFLOAT), false, true);
        addPFloatField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_PFLOATS), true, true);
        addPLongField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_PLONG), false, true);
        addPLongField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_PLONGS), true, true);
        addPDoubleField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_PDOUBLE), false, true);
        addPDoubleField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_PDOUBLES), true, true);
        addPDateField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_PDATE), false, true);
        addPDateField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_PDATES), true, true);
        addBinaryField(DefaultSolrUtils.getMapDynamicFieldName(name, DefaultSolrUtils.SOLR_TYPE_BINARY), true);

        // TODO: storage of unsupported types
    }

    /**
     * Add a field in the Solr schema.
     * 
     * @param name the name of the field to add
     * @param type the tpe of the field to addd
     * @param dynamic true to create a dynamic field
     * @param attributes attributed to add to the field definition
     * @throws SolrException when failing to add the field
     */
    protected void addField(String name, String type, boolean dynamic, Object... attributes) throws SolrException
    {
        Map<String, Object> fieldAttributes = new HashMap<>();
        fieldAttributes.put(SOLR_FIELD_NAME, name);
        fieldAttributes.put(FieldType.TYPE, type);

        MapUtils.putAll(fieldAttributes, attributes);

        addField(fieldAttributes, dynamic);
    }

    /**
     * Add a field in the Solr schema.
     * 
     * @param fieldAttributes the attributes of the field to add
     * @param dynamic true to create a dynamic field
     * @throws SolrException when failing to add the field
     */
    protected void addField(Map<String, Object> fieldAttributes, boolean dynamic) throws SolrException
    {
        try {
            if (dynamic) {
                new SchemaRequest.AddDynamicField(fieldAttributes).process(this.client);
            } else {
                new SchemaRequest.AddField(fieldAttributes).process(this.client);
            }
        } catch (Exception e) {
            throw new SolrException("Failed to add a field in the Solr core", e);
        }
    }

    /**
     * Add a field type in the Solr schema.
     * 
     * @param name the name of the field type
     * @param solrClass the class of the field type
     * @param attributes the other attributes of the field type
     * @throws SolrException when failing to add the field
     */
    protected void addFieldType(String name, String solrClass, Object... attributes) throws SolrException
    {
        setFieldType(name, solrClass, true, attributes);
    }

    /**
     * Add a field type in the Solr schema.
     * 
     * @param name the name of the field type
     * @param solrClass the class of the field type
     * @param attributes the other attributes of the field type
     * @throws SolrException when failing to add the field
     */
    protected void replaceFieldType(String name, String solrClass, Object... attributes) throws SolrException
    {
        setFieldType(name, solrClass, false, attributes);
    }

    /**
     * Add a field type in the Solr schema.
     * 
     * @param name the name of the field type
     * @param solrClass the class of the field type
     * @param add true if the field type should be added, false for replace
     * @param attributes the other attributes of the field type
     * @throws SolrException when failing to add the field
     */
    protected void setFieldType(String name, String solrClass, boolean add, Object... attributes) throws SolrException
    {
        Map<String, Object> attributesMap = new HashMap<>(2 + (attributes.length > 0 ? attributes.length / 2 : 0));

        attributesMap.put(FieldType.TYPE_NAME, name);
        attributesMap.put(FieldType.CLASS_NAME, solrClass);

        MapUtils.putAll(attributesMap, attributes);

        setFieldType(attributesMap, add);
    }

    /**
     * Add a field type in the Solr schema.
     * 
     * @param attributes the attributes of the field to add
     * @param add true if the field type should be added, false for replace
     * @throws SolrException when failing to add the field
     */
    protected void setFieldType(Map<String, Object> attributes, boolean add) throws SolrException
    {
        try {
            FieldTypeDefinition definition = new FieldTypeDefinition();
            definition.setAttributes(attributes);

            if (add) {
                new SchemaRequest.AddFieldType(definition).process(this.client);
            } else {
                new SchemaRequest.ReplaceFieldType(definition).process(this.client);
            }
        } catch (Exception e) {
            throw new SolrException("Failed to add a field type in the Solr core", e);
        }

        // Reset the cache
        this.types = null;
    }

    /**
     * Performs an explicit commit, causing pending documents to be committed for indexing.
     * 
     * @throws SolrException when failing to commit
     */
    protected void commit() throws SolrException
    {
        try {
            this.client.commit();
        } catch (Exception e) {
            throw new SolrException("Failed to commit", e);
        }
    }
}
