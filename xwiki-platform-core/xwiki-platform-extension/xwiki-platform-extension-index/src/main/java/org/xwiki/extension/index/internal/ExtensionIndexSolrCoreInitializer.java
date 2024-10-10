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
package org.xwiki.extension.index.internal;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServerException;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.DefaultExtensionComponent;
import org.xwiki.extension.Extension;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.RemoteExtension;
import org.xwiki.extension.rating.RatingExtension;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.SolrException;

/**
 * Initialize the Solr core dedicated to events storage.
 *
 * @version $Id$
 * @since 12.10
 */
@Component
@Named(ExtensionIndexSolrCoreInitializer.NAME)
@Singleton
public class ExtensionIndexSolrCoreInitializer extends AbstractSolrCoreInitializer
{
    /**
     * The name of the core.
     */
    public static final String NAME = "extension_index";

    /**
     * Suffix used for indexed version of standard extensions fields.
     */
    public static final String INDEX_SUFFIX = "_index";

    /**
     * Index the list of features in a full text search friendly manner.
     */
    public static final String SOLR_FIELD_EXTENSIONFEATURES_INDEX = Extension.FIELD_EXTENSIONFEATURES + INDEX_SUFFIX;

    /**
     * Index the list of authors in a full text search friendly manner.
     */
    public static final String SOLR_FIELD_AUTHORS_INDEX = Extension.FIELD_AUTHORS + INDEX_SUFFIX;

    /**
     * Index the list of components in a full text search friendly manner.
     *
     * @since 13.3RC1
     */
    public static final String SOLR_FIELD_COMPONENTS_INDEX = Extension.FIELD_COMPONENTS + INDEX_SUFFIX;

    /**
     * We can't use {@link Extension#FIELD_ID} because it collide with {@link #SOLR_FIELD_ID}.
     */
    public static final String SOLR_FIELD_EXTENSIONID = "s_extensionId";

    /**
     * The name of the field indicating in which namespaces an extension is compatible.
     */
    public static final String SOLR_FIELD_COMPATIBLE_NAMESPACES = "s_compatibleNamespaces";

    /**
     * The name of the field indicating in which namespaces an extension is incompatible.
     */
    public static final String SOLR_FIELD_INCOMPATIBLE_NAMESPACES = "s_incompatibleNamespaces";

    /**
     * The name of the field indicating if an entry is the last version of an extension.
     */
    public static final String SOLR_FIELD_LAST = "s_last";

    /**
     * The name of the field containing the date at which the extension was indexed.
     */
    public static final String SOLR_FIELD_INDEX_DATE = "s_indexDate";

    /**
     * The name of the field storing the score of the most critical CVE know for an extension.
     *
     * @see #SECURITY_CVE_CVSS for the list of individual CVSS values of the CVEs known for the extension
     * @since 15.5RC1
     */
    public static final String SECURITY_MAX_CVSS = "security_maxCVSS";

    /**
     * The name of the field storing the IDs of the known CVEs for an extension.
     *
     * @since 15.5RC1
     */
    public static final String SECURITY_CVE_ID = "security_cveID";

    /**
     * The link of the CVEs known for an extension.
     *
     * @since 15.5RC1
     */
    public static final String SECURITY_CVE_LINK = "security_cveLink";

    /**
     * The values of the individual CVEs known for an extension.
     *
     * @see #SECURITY_MAX_CVSS stores the maxium value of this collection
     */
    public static final String SECURITY_CVE_CVSS = "security_cveCVSS";

    /**
     * The number of CVEs know for an extension.
     */
    public static final String SECURITY_CVE_COUNT = "security_cveCount";

    /**
     * The computed minimal version where all the vulnerabilities are fixed.
     */
    public static final String SECURITY_FIX_VERSION = "security_fixVersion";

    /**
     * A textual explanation of what to do about this extension to fix the security vulnerabilities.
     */
    public static final String SECURITY_ADVICE = "security_advice";

    /**
     * When {@code true} the extension is core.
     */
    public static final String IS_CORE_EXTENSION = "is_core_extension";

    /**
     * When {@code true} the extension has been reviewed and is not is considered as safe, {@code false} otherwise.
     */
    public static final String IS_REVIEWED_SAFE = "security_is_reviewed_safe";

    /**
     * Contains the explanations regarding why a given vulnerability can be considered as safe. This field contains an
     * array of html contents.
     */
    public static final String IS_SAFE_EXPLANATIONS = "security_is_safe_explanations";

    private static final Pattern COMPONENT_SPECIAL_CHARS = Pattern.compile("[<>,]+");

    @Override
    protected long getVersion()
    {
        return SCHEMA_VERSION_16_7;
    }

    @Override
    public boolean isCache()
    {
        return true;
    }

    @Override
    protected void createSchema() throws SolrException
    {
        setStringField(SOLR_FIELD_EXTENSIONID, false, false);
        setStringField(Extension.FIELD_VERSION, false, false);

        setStringField(Extension.FIELD_TYPE, false, false);
        setStringField(Extension.FIELD_REPOSITORY, false, false);
        setStringField(Extension.FIELD_ALLOWEDNAMESPACES, true, false);
        setStringField(Extension.FIELD_CATEGORY, false, false);

        setTextGeneralField(Extension.FIELD_NAME, false, false);
        setTextGeneralField(Extension.FIELD_SUMMARY, false, false);
        setTextGeneralField(Extension.FIELD_WEBSITE, false, false);

        setBooleanField(RemoteExtension.FIELD_RECOMMENDED, false, false);

        setPIntField(RatingExtension.FIELD_TOTAL_VOTES, false, false);
        setPFloatField(RatingExtension.FIELD_AVERAGE_VOTE, false, false);

        setStringField(Extension.FIELD_AUTHORS, true, false);
        setTextGeneralField(SOLR_FIELD_AUTHORS_INDEX, true, false, SOLR_FIELD_STORED, false);

        setStringField(Extension.FIELD_EXTENSIONFEATURES, true, false);
        setTextGeneralField(SOLR_FIELD_EXTENSIONFEATURES_INDEX, true, false, SOLR_FIELD_STORED, false);

        // TODO: add dependencies
        // TODO: add managed dependencies

        setPDateField(SOLR_FIELD_INDEX_DATE, false, false);

        setBooleanField(SOLR_FIELD_LAST, false, false);

        setStringField(SOLR_FIELD_COMPATIBLE_NAMESPACES, true, false);
        setStringField(SOLR_FIELD_INCOMPATIBLE_NAMESPACES, true, false);

        migrateSchema(SCHEMA_VERSION_12_9);
    }

    @Override
    protected void migrateSchema(long cversion) throws SolrException
    {
        if (cversion < SCHEMA_VERSION_13_3) {
            // Store all components
            setStringField(Extension.FIELD_COMPONENTS, true, false);
            setTextGeneralField(SOLR_FIELD_COMPONENTS_INDEX, true, false, SOLR_FIELD_STORED, false);
            addCopyField(Extension.FIELD_COMPONENTS, SOLR_FIELD_COMPONENTS_INDEX);

            // Store hints for each component type
            setStringField(toComponentFieldName("*"), true, true);
        }

        if (cversion < SCHEMA_VERSION_14_0) {
            setStringField(InstalledExtension.FIELD_INSTALLED_NAMESPACES, true, false);
        }

        if (cversion < SCHEMA_VERSION_15_5) {
            setPDoubleField(SECURITY_MAX_CVSS, false, false);
            setStringField(SECURITY_CVE_ID, true, false);
            setStringField(SECURITY_CVE_LINK, true, false);
            setPDoubleField(SECURITY_CVE_CVSS, true, false);
            setPIntField(SECURITY_CVE_COUNT, false, false);
            setStringField(SECURITY_FIX_VERSION, false, false);
            setStringField(SECURITY_ADVICE, false, false);
        }

        if (cversion < SCHEMA_VERSION_15_6) {
            setBooleanField(IS_REVIEWED_SAFE, true, false);
            setStringField(IS_SAFE_EXPLANATIONS, true, false);
        }

        if (cversion < SCHEMA_VERSION_15_9) {
            setBooleanField(IS_CORE_EXTENSION, false, false);
            if (cversion >= SCHEMA_VERSION_15_6) {
                // Cleanup previously required field from the index.
                try {
                    this.core.getClient().deleteByQuery("is_installed:[* TO *] OR is_from_environment:[* TO *]");
                } catch (SolrServerException | IOException e) {
                    throw new SolrException("Failed to cleanup is_installed field", e);
                }
                deleteField("is_installed", false);
                deleteField("is_from_environment", false);
            }
        }

        if (cversion < SCHEMA_VERSION_16_7) {
            setStringField(RemoteExtension.FIELD_SUPPORT_PLANS, true, false);
        }
    }

    /**
     * @param componentType the type of the component
     * @return the name of the Solr field where to store this component hints
     */
    public static String toComponentFieldName(String componentType)
    {
        String cannonicalType = DefaultExtensionComponent.toCanonicalComponentType(componentType);

        // It's a pity but I cannot find any way to escape < > and , in a Solr query so replacing them
        // Conflict is theorically possible but highly unlikely, especially since it's only about component roles
        cannonicalType = COMPONENT_SPECIAL_CHARS.matcher(cannonicalType).replaceAll("_");

        return Extension.FIELD_COMPONENTS + "__" + cannonicalType;
    }
}
