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
package com.xpn.xwiki.internal.cache.rendering;

import java.util.List;
import java.util.regex.Pattern;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Default implementation of {@link RenderingCacheConfiguration}.
 * 
 * @version $Id$
 * @since 2.4M1
 */
@Component
public class DefaultRenderingCacheConfiguration implements RenderingCacheConfiguration
{
    /**
     * Configuration key prefix.
     */
    private static final String PREFIX = "core.renderingcache.";

    /**
     * Name of the property indication if the cache is enabled or not.
     */
    private static final String PROPNAME_ENABLED = PREFIX + "enabled";

    /**
     * Name of the property listing the references of the documents to cache.
     */
    private static final String PROPNAME_DOCUMENTS = PREFIX + "documents";

    /**
     * Name of the property indication the time to live of the elements in the cache.
     */
    private static final String PROPNAME_DURATION = PREFIX + "duration";

    /**
     * The default time to live of the elements in the cache.
     */
    private static final int PROPVALUE_DURATION = 300;

    /**
     * Name of the property indication the size of the cache.
     */
    private static final String PROPNAME_SIZE = PREFIX + "size";

    /**
     * The default size of the cache.
     */
    private static final int PROPVALUE_SIZE = 100;

    /**
     * xwiki.properties file configurations.
     */
    @Requirement("xwikiproperties")
    private ConfigurationSource farmConfiguration;

    /**
     * Wiki configuration.
     */
    @Requirement("wiki")
    private ConfigurationSource wikiConfiguration;

    /**
     * Used to serialize a document reference into a String.
     */
    @Requirement
    private EntityReferenceSerializer<String> serializer;

    /**
     * Used to serialize a document reference into a String.
     */
    @Requirement("compactwiki")
    private EntityReferenceSerializer<String> wikiSerializer;

    /**
     * USed to get the current wiki.
     */
    @Requirement
    private ModelContext modelContext;

    /**
     * The cached pattern coming from xwiki.properties file.
     */
    private Pattern farmPattern;

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.internal.cache.rendering.RenderingCacheConfiguration#isEnabled()
     */
    public boolean isEnabled()
    {
        return isFarmEnabled();
    }

    /**
     * @return true if the rendering cache system is enabled in general
     */
    public boolean isFarmEnabled()
    {
        return this.farmConfiguration.getProperty(PROPNAME_ENABLED, false);
    }

    /**
     * @return true if the rendering cache system is enabled in general
     */
    public boolean isWikiEnabled()
    {
        return this.wikiConfiguration.getProperty(PROPNAME_ENABLED, true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.internal.cache.rendering.RenderingCacheConfiguration#getDuration()
     */
    public int getDuration()
    {
        return this.farmConfiguration.getProperty(PROPNAME_DURATION, PROPVALUE_DURATION);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.internal.cache.rendering.RenderingCacheConfiguration#getSize()
     */
    public int getSize()
    {
        return this.farmConfiguration.getProperty(PROPNAME_SIZE, PROPVALUE_SIZE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.internal.cache.rendering.RenderingCacheConfiguration#isCached(org.xwiki.model.reference.DocumentReference)
     */
    public boolean isCached(DocumentReference documentReference)
    {
        if (documentReference != null && isFarmEnabled()) {
            if (isCachedInFarm(documentReference)) {
                return true;
            }

            return isCachedInWiki(documentReference);
        }

        return false;
    }

    /**
     * Indicate if the provided document's rendering result should be cached according to farm configuration.
     * 
     * @param documentReference the reference of the document
     * @return true if the document should be cached, false otherwise
     */
    private boolean isCachedInFarm(DocumentReference documentReference)
    {
        Pattern pattern = getFarmPattern();

        if (pattern != null) {
            String documentReferenceString = this.serializer.serialize(documentReference);

            return pattern.matcher(documentReferenceString).matches();
        }

        return false;
    }

    /**
     * Indicate if the provided document's rendering result should be cached according to wiki configuration.
     * 
     * @param documentReference the reference of the document
     * @return true if the document should be cached, false otherwise
     */
    public boolean isCachedInWiki(DocumentReference documentReference)
    {
        if (isWikiEnabled()
            && this.modelContext.getCurrentEntityReference() != null
            && documentReference.getWikiReference().getName().equals(
                this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI).getName())) {
            Pattern pattern = getWikiPattern();

            if (pattern != null) {
                return pattern.matcher(this.serializer.serialize(documentReference)).matches()
                    || pattern.matcher(this.wikiSerializer.serialize(documentReference)).matches();
            }
        }

        return false;
    }

    /**
     * @return the pattern to match documents to cache according to farm configuration.
     */
    private Pattern getFarmPattern()
    {
        if (this.farmPattern == null) {
            this.farmPattern = getPattern(this.farmConfiguration.getProperty(PROPNAME_DOCUMENTS, List.class));
        }

        return this.farmPattern;
    }

    /**
     * @return the pattern to match documents to cache according to wiki configuration.
     */
    private Pattern getWikiPattern()
    {
        return getPattern(this.wikiConfiguration.getProperty(PROPNAME_DOCUMENTS, List.class));
    }

    /**
     * Convert a list of String patterns into one {@link Pattern} object.
     * 
     * @param configuration the {@link String} to convert to one {@link Pattern}
     * @return {@link Pattern} version of the provided list of {@link String}.
     */
    private Pattern getPattern(List<String> configuration)
    {
        Pattern pattern = null;

        if (configuration != null && !configuration.isEmpty()) {
            StringBuffer patternBuffer = new StringBuffer();

            for (String patternString : configuration) {
                if (patternBuffer.length() > 0) {
                    patternBuffer.append('|');
                }
                patternBuffer.append('(');
                patternBuffer.append(patternString);
                patternBuffer.append(')');
            }

            pattern = Pattern.compile(patternBuffer.toString());
        }

        return pattern;
    }
}
