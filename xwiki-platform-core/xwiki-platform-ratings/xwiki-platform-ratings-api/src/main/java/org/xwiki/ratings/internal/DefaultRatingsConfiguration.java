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
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.ratings.RatingsConfiguration;
import org.xwiki.ratings.RatingsManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Provides Ratings configuration.
 *
 * @see RatingsConfiguration
 * @version $Id$
 * @since 8.1M1
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
     * Retrieves the XWiki private API object.
     *
     * @return The XWiki private API object
     */
    protected XWiki getXWiki()
    {
        return getXWikiContext().getWiki();
    }

    /**
     * Retrieve the XWiki context from the current execution context.
     * 
     * @return the XWiki context
     * @throws RuntimeException If there was an error retrieving the context
     */
    private XWikiContext getXWikiContext()
    {
        return this.xcontextProvider.get();
    }

    /**
     * Store a caught exception in the context.
     * 
     * @param e the exception to store, can be null to clear the previously stored exception
     */
    private void setError(Throwable e)
    {
        getXWikiContext().put("exception", e);
    }

    /**
     * Get configuration document.
     *
     * @param documentReference the documentReference for which to return the configuration document
     * @return the configuration document
     */
    public XWikiDocument getConfigurationDocument(DocumentReference documentReference)
    {
        setError(null);

        try {
            SpaceReference lastSpaceReference = documentReference.getLastSpaceReference();
            while (lastSpaceReference.getType() == EntityType.SPACE) {
                DocumentReference spaceConfigDocReference =
                    new DocumentReference(RatingsManager.RATINGS_CONFIG_SPACE_PAGE, lastSpaceReference);
                XWikiDocument spaceConfigDoc = getXWiki().getDocument(spaceConfigDocReference, getXWikiContext());
                if (spaceConfigDoc != null
                    && spaceConfigDoc.getXObject(RatingsManager.RATINGS_CONFIG_CLASSREFERENCE) != null) {
                    return spaceConfigDoc;
                }
                if (lastSpaceReference.getParent().getType() == EntityType.SPACE) {
                    lastSpaceReference = new SpaceReference(lastSpaceReference.getParent());
                } else {
                    break;
                }
            }
            XWikiDocument globalConfigDoc =
                getXWiki().getDocument(RatingsManager.RATINGS_CONFIG_GLOBAL_REFERENCE, getXWikiContext());
            return globalConfigDoc;
        } catch (Throwable e) {
            setError(e);
            return null;
        }
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
        try {
            XWikiDocument configDoc = getConfigurationDocument(documentReference);
            if (configDoc != null && !configDoc.isNew() 
                && configDoc.getXObject(RatingsManager.RATINGS_CONFIG_CLASSREFERENCE) != null) {
                BaseProperty prop = (BaseProperty) configDoc.getXObject(RatingsManager.RATINGS_CONFIG_CLASSREFERENCE)
                    .get(parameterName);
                String propValue = (prop == null) ? defaultValue : prop.getValue().toString();
                return (propValue.equals("") ? defaultValue : propValue);
            }
        } catch (Exception e) {
            logger.error("Cannot read ratings config", e);
        }
        return defaultValue;
    }
}
