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
import org.xwiki.extension.rating.RatingExtension;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

public class XWikiRepositoryModel
{
    // References

    public static final String EXTENSION_CLASSNAME = "ExtensionCode.ExtensionClass";

    public static final String AVERAGERATING_CLASSNAME = "XWiki.AverageRatingsClass";

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

    public static final String PROP_RATING_TOTALVOTES = "nbvotes";

    public static final String PROP_RATING_AVERAGEVOTE = "averagevote";

    // Consolidation

    public static final String PROP_EXTENSION_LASTVERSION = "lastVersion";

    public static final String PROP_EXTENSION_VALIDEXTENSION = "validExtension";

    // Solr

    public static final String SOLRPROP_EXTENSION_VALIDEXTENSION = toExtensionClassSolrPropertyName(
        PROP_EXTENSION_VALIDEXTENSION, "boolean");

    public static final String SOLR_STRING = "string";

    public static final String SOLR_INTEGER = "int";

    public static final Map<String, SolrField> SOLR_FIELDS = new HashMap<>();

    public static class ExtensionSolrField extends SolrField
    {
        public ExtensionSolrField(String name, Float boostValue)
        {
            this(name, SOLR_STRING, boostValue);
        }

        public ExtensionSolrField(String name, String type, Float boostValue)
        {
            super(toExtensionClassSolrPropertyName(name, type), type != null ? toExtensionClassSolrPropertyName(name)
                : null, boostValue);
        }
    }

    public static class RatingSolrField extends SolrField
    {
        public RatingSolrField(String name, Float boostValue)
        {
            this(name, SOLR_STRING, boostValue);
        }

        public RatingSolrField(String name, String type, Float boostValue)
        {
            super(toAverageRatingClassSolrPropertyName(name, type), type != null
                ? toAverageRatingClassSolrPropertyName(name) : null, boostValue);
        }
    }

    public static class SolrField
    {
        public final String name;

        public final String boostName;

        public final Float boostValue;

        public SolrField(String name, String boostName, Float boostValue)
        {
            this.name = name;
            this.boostName = boostName;
            this.boostValue = boostValue;
        }
    }

    static {
        SOLR_FIELDS.put(Extension.FIELD_ID, new ExtensionSolrField(PROP_EXTENSION_ID, 10.0f));
        SOLR_FIELDS.put(Extension.FIELD_FEATURE, new ExtensionSolrField(PROP_EXTENSION_FEATURES, 9.0f));
        SOLR_FIELDS.put(Extension.FIELD_FEATURES, SOLR_FIELDS.get(Extension.FIELD_FEATURE));
        SOLR_FIELDS.put(Extension.FIELD_NAME, new ExtensionSolrField(PROP_EXTENSION_NAME, 8.0f));
        SOLR_FIELDS.put(Extension.FIELD_TYPE, new ExtensionSolrField(PROP_EXTENSION_TYPE, 8.0f));
        SOLR_FIELDS.put(Extension.FIELD_CATEGORY, new ExtensionSolrField(PROP_EXTENSION_CATEGORY, 7.0f));
        SOLR_FIELDS.put(Extension.FIELD_SUMMARY, new ExtensionSolrField(PROP_EXTENSION_SUMMARY, 6.0f));

        // We only search in the description but we don't retrieve it (because it's not stored in a stable field)
        SOLR_FIELDS.put(Extension.FIELD_DESCRIPTION, new SolrField(PROP_EXTENSION_DESCRIPTION, null, 5.0f));

        // Not very interesting for fulltext search
        SOLR_FIELDS.put(Extension.FIELD_AUTHOR, new ExtensionSolrField(PROP_EXTENSION_AUTHORS, null));
        SOLR_FIELDS.put(Extension.FIELD_AUTHORS, SOLR_FIELDS.get(Extension.FIELD_AUTHOR));
        SOLR_FIELDS.put(Extension.FIELD_VERSION, new ExtensionSolrField(PROP_EXTENSION_LASTVERSION, null));
        SOLR_FIELDS.put(Extension.FIELD_LICENSE, new ExtensionSolrField(PROP_EXTENSION_LICENSENAME, null));
        SOLR_FIELDS.put(Extension.FIELD_LICENSES, SOLR_FIELDS.get(Extension.FIELD_LICENSE));
        SOLR_FIELDS.put(Extension.FIELD_SCM, new ExtensionSolrField(PROP_EXTENSION_SCMURL, null));
        SOLR_FIELDS.put(Extension.FIELD_SCM, new ExtensionSolrField(PROP_EXTENSION_SCMURL, null));
        SOLR_FIELDS.put(PROP_EXTENSION_SCMCONNECTION, new ExtensionSolrField(PROP_EXTENSION_SCMCONNECTION, null));
        SOLR_FIELDS.put(PROP_EXTENSION_SCMDEVCONNECTION, new ExtensionSolrField(PROP_EXTENSION_SCMDEVCONNECTION, null));
        SOLR_FIELDS.put(Extension.FIELD_WEBSITE, new ExtensionSolrField(PROP_EXTENSION_WEBSITE, null));

        // Rating
        SOLR_FIELDS.put(PROP_RATING_TOTALVOTES, new RatingSolrField(PROP_RATING_TOTALVOTES, "int", null));
        SOLR_FIELDS.put("votes", SOLR_FIELDS.get(PROP_RATING_TOTALVOTES));
        SOLR_FIELDS.put(RatingExtension.FIELD_AVERAGE_VOTE, new RatingSolrField(PROP_RATING_AVERAGEVOTE, "float", null));
        SOLR_FIELDS.put("vote", SOLR_FIELDS.get(PROP_RATING_AVERAGEVOTE));

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

    public static String toAverageRatingClassSolrPropertyName(String propertyName)
    {
        return "property." + AVERAGERATING_CLASSNAME + '.' + propertyName;
    }

    public static String toAverageRatingClassSolrPropertyName(String propertyName, String type)
    {
        return toAverageRatingClassSolrPropertyName(propertyName) + '_' + type;
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
