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
package org.xwiki.repository.internal;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.extension.Extension;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

public class XWikiRepositoryModel
{
    // References

    public static final String EXTENSION_CLASSNAME = "ExtensionCode.ExtensionClass";

    public static final String EXTENSIONVERSION_CLASSNAME = "ExtensionCode.ExtensionVersionClass";

    public static final String EXTENSIONDEPENDENCY_CLASSNAME = "ExtensionCode.ExtensionDependencyClass";

    public static final String EXTENSIONPROXY_CLASSNAME = "ExtensionCode.ExtensionProxyClass";

    public static final EntityReference EXTENSION_CLASSREFERENCE = new EntityReference("ExtensionClass",
        EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    public static final EntityReference EXTENSIONVERSION_CLASSREFERENCE = new EntityReference("ExtensionVersionClass",
        EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    public static final EntityReference EXTENSIONDEPENDENCY_CLASSREFERENCE = new EntityReference(
        "ExtensionDependencyClass", EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    public static final EntityReference EXTENSIONPROXY_CLASSREFERENCE = new EntityReference("ExtensionProxyClass",
        EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    public static final EntityReference EXTENSION_TEMPLATEREFERENCE = new EntityReference("ExtensionTemplate",
        EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    public static final String CONFIGURATION_CLASSNAME = "ExtensionCode.RepositoryConfigClass";

    public static final EntityReference CONFIGURATION_CLASSREFERENCE = new EntityReference("RepositoryConfigClass",
        EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    public static final EntityReference CONFIGURATION_REFERENCE = new EntityReference("RepositoryConfig",
        EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    // Properties

    public static final String PROP_EXTENSION_ID = "id";

    public static final String PROP_EXTENSION_TYPE = "type";

    public static final String PROP_EXTENSION_LICENSENAME = "licenseName";

    public static final String PROP_EXTENSION_SUMMARY = "summary";

    public static final String PROP_EXTENSION_DESCRIPTION = "description";

    public static final String PROP_EXTENSION_NAME = "name";

    public static final String PROP_EXTENSION_WEBSITE = "website";

    public static final String PROP_EXTENSION_AUTHORS = "authors";

    public static final String PROP_EXTENSION_FEATURES = "features";

    public static final String PROP_EXTENSION_CATEGORY = "category";

    public static final String PROP_EXTENSION_SCMURL = "source";

    public static final String PROP_EXTENSION_SCMCONNECTION = "scmconnection";

    public static final String PROP_EXTENSION_SCMDEVCONNECTION = "scmdevconnection";

    public static final String PROP_EXTENSION_PROPERTIES = "properties";

    public static final String PROP_VERSION_ID = "id";

    public static final String PROP_VERSION_VERSION = "version";

    public static final String PROP_VERSION_DOWNLOAD = "download";

    public static final String PROP_DEPENDENCY_EXTENSIONVERSION = "extensionVersion";

    public static final String PROP_DEPENDENCY_ID = "id";

    public static final String PROP_DEPENDENCY_CONSTRAINT = "constraint";

    public static final String PROP_PROXY_REPOSITORYID = "repositoryId";

    public static final String PROP_PROXY_REPOSITORYTYPE = "repositoryType";

    public static final String PROP_PROXY_REPOSITORYURI = "repositoryURI";

    public static final String PROP_PROXY_AUTOUPDATE = "autoUpdate";

    public static final String PROP_CONFIGURATION_DEFAULTIDPREFIX = "defaultIdPrefix";

    public static final String PROP_CONFIGURATION_VALIDTYPEs = "validTypes";

    // Consolidation

    public static final String PROP_EXTENSION_LASTVERSION = "lastVersion";

    public static final String PROP_EXTENSION_VALIDEXTENSION = "validExtension";

    // Solr

    public static final String SOLRPROP_EXTENSION_VALIDEXTENSION = toExtensionClassSolrPropertyName(
        PROP_EXTENSION_VALIDEXTENSION, "boolean");

    public static final String SOLR_STRING = "string";

    public static final Map<String, SolrField> SOLR_FIELDS = new HashMap<>();

    public static class SolrField
    {
        public final String name;

        public final String boostName;

        public final Float boostValue;

        public SolrField(String name, Float boostValue)
        {
            this(name, SOLR_STRING, boostValue);
        }

        public SolrField(String name, String type, Float boostValue)
        {
            this.name = toExtensionClassSolrPropertyName(name, type);
            this.boostName = type != null ? toExtensionClassSolrPropertyName(name) : null;
            this.boostValue = boostValue;
        }
    }

    static {
        SOLR_FIELDS.put(Extension.FIELD_ID, new SolrField(PROP_EXTENSION_ID, 10.0f));
        SOLR_FIELDS.put(Extension.FIELD_FEATURE, new SolrField(PROP_EXTENSION_FEATURES, 9.0f));
        SOLR_FIELDS.put(Extension.FIELD_FEATURES, SOLR_FIELDS.get(Extension.FIELD_FEATURE));
        SOLR_FIELDS.put(Extension.FIELD_NAME, new SolrField(PROP_EXTENSION_NAME, 8.0f));
        SOLR_FIELDS.put(Extension.FIELD_TYPE, new SolrField(PROP_EXTENSION_TYPE, 8.0f));
        SOLR_FIELDS.put(Extension.FIELD_CATEGORY, new SolrField(PROP_EXTENSION_CATEGORY, 7.0f));
        SOLR_FIELDS.put(Extension.FIELD_SUMMARY, new SolrField(PROP_EXTENSION_SUMMARY, 6.0f));

        // We only search in the description but we don't retrieve it (because it's not stored in a stable field) 
        SOLR_FIELDS.put(Extension.FIELD_DESCRIPTION, new SolrField(PROP_EXTENSION_DESCRIPTION, null, 5.0f));

        // Not very interesting for fulltext search
        SOLR_FIELDS.put(Extension.FIELD_AUTHOR, new SolrField(PROP_EXTENSION_AUTHORS, null));
        SOLR_FIELDS.put(Extension.FIELD_AUTHORS, SOLR_FIELDS.get(Extension.FIELD_AUTHOR));
        SOLR_FIELDS.put(Extension.FIELD_VERSION, new SolrField(PROP_EXTENSION_LASTVERSION, null));
        SOLR_FIELDS.put(Extension.FIELD_LICENSE, new SolrField(PROP_EXTENSION_LICENSENAME, null));
        SOLR_FIELDS.put(Extension.FIELD_LICENSES, SOLR_FIELDS.get(Extension.FIELD_LICENSE));
        SOLR_FIELDS.put(Extension.FIELD_SCM, new SolrField(PROP_EXTENSION_SCMURL, null));
        SOLR_FIELDS.put(Extension.FIELD_SCM, new SolrField(PROP_EXTENSION_SCMURL, null));
        SOLR_FIELDS.put(PROP_EXTENSION_SCMCONNECTION, new SolrField(PROP_EXTENSION_SCMCONNECTION, null));
        SOLR_FIELDS.put(PROP_EXTENSION_SCMDEVCONNECTION, new SolrField(PROP_EXTENSION_SCMDEVCONNECTION, null));
        SOLR_FIELDS.put(Extension.FIELD_WEBSITE, new SolrField(PROP_EXTENSION_WEBSITE, null));

        // Fields not stored
        // Extension.FIELD_REPOSITORY
    }

    public static String toExtensionClassSolrPropertyName(String propertyName)
    {
        return "property." + EXTENSION_CLASSNAME + '.' + propertyName;
    }

    public static String toExtensionClassSolrPropertyName(String propertyName, String type)
    {
        return toExtensionClassSolrPropertyName(propertyName) + '_' + type;
    }

    public static String toSolrField(String restField)
    {
        SolrField field = SOLR_FIELDS.get(restField);

        if (field == null) {
            return null;
        }

        return field.name;
    }
}
