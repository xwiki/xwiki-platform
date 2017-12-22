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
package org.xwiki.ratings.internal;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.ratings.RatingsConfiguration;
import org.xwiki.ratings.RatingsManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Provides Ratings configuration.
 *
 * @see RatingsConfiguration
 * @version $Id$
 * @since 8.2.1
 */
@Component
@Singleton
public class DefaultRatingsConfiguration implements RatingsConfiguration
{
    @Inject
    private Logger logger;
    
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Get document.
     *
     * @param reference the reference for which to return the document
     * @return the document
     */
    public XWikiDocument getDocument(EntityReference reference)
    {
        XWikiContext context = xcontextProvider.get();
        try
        {
            return context.getWiki().getDocument(reference, context);
        } catch (XWikiException e) {
            logger.error("Failed to retrieve the document for the reference [{}].", reference, e);
            return null;
        }
    }

    /**
     * Get configuration document.
     *
     * @param documentReference the documentReference for which to return the configuration document
     * @return the configuration document
     */
    public XWikiDocument getConfigurationDocument(DocumentReference documentReference)
    {
        SpaceReference lastSpaceReference = documentReference.getLastSpaceReference();
        while (lastSpaceReference.getType() == EntityType.SPACE) {
            DocumentReference configurationDocumentReference =
                new DocumentReference(RatingsManager.RATINGS_CONFIG_SPACE_PAGE, lastSpaceReference);
            XWikiDocument spaceConfigurationDocument = getDocument((EntityReference) configurationDocumentReference);
            if (spaceConfigurationDocument != null
                && spaceConfigurationDocument.getXObject(RatingsManager.RATINGS_CONFIG_CLASSREFERENCE) != null) {
                return spaceConfigurationDocument;
            }
            if (lastSpaceReference.getParent().getType() == EntityType.SPACE) {
                lastSpaceReference = new SpaceReference(lastSpaceReference.getParent());
            } else {
                break;
            }
        }
        XWikiDocument globalConfigurationDocument = getDocument(RatingsManager.RATINGS_CONFIG_GLOBAL_REFERENCE);
        return globalConfigurationDocument;
    }
    
    /**
     * Retrieves configuration parameter from the current space's WebPreferences and fallback to XWiki.RatingsConfig if
     * it does not exist.
     * 
     * @param documentReference the document being rated or for which the existing ratings are fetched
     * @param parameterName the parameter for which to retrieve the value
     * @param defaultValue the default value for the parameter
     * @return the value of the given parameter name from the current configuration context
     */
    public String getConfigurationParameter(DocumentReference documentReference, String parameterName, 
        String defaultValue)
    {
        XWikiDocument configurationDocument = getConfigurationDocument(documentReference);
        if (configurationDocument != null && !configurationDocument.isNew() 
            && configurationDocument.getXObject(RatingsManager.RATINGS_CONFIG_CLASSREFERENCE) != null) {
            try
            {
                BaseProperty prop = (BaseProperty) configurationDocument.
                    getXObject(RatingsManager.RATINGS_CONFIG_CLASSREFERENCE).get(parameterName);
                String propValue = (prop == null) ? defaultValue : prop.getValue().toString();
                return (propValue.equals("") ? defaultValue : propValue);  
            } catch (XWikiException e) {
                logger.error("Failed to retrieve the property for the configurationDocument [{}].", 
                    configurationDocument, e);
                return null;
            } 
        }
        return defaultValue;
    }
}
