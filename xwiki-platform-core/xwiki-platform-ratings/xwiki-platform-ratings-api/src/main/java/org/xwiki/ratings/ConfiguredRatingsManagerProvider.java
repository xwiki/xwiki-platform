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
package org.xwiki.ratings;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Retrieve the appropriate RatingsManager by looking at the current configuration settings.
 * 
 * @version $Id$
 * @since 6.4M3
 */
@Component
@Singleton
// TODO: replace this system by a default component dynamically taking into account the configuration behind the scene
public class ConfiguredRatingsManagerProvider implements ConfiguredProvider<RatingsManager>
{
    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private RatingsConfiguration ratingsConfiguration;
    
    /**
     * Retrieve the XWiki context from the current execution context.
     * 
     * @return the XWiki context
     * @throws RuntimeException if there was an error retrieving the context
     */
    protected XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    /**
     * Retrieve the XWiki private API object.
     * 
     * @return the XWiki private API object
     */
    protected XWiki getXWiki()
    {
        return getXWikiContext().getWiki();
    }

    /**
     * Retrieve an instance of the desired RatingsManager (default/separate). - default: save the rating information in
     * the same page - separate: save the rating information in a specified space
     *
     * @param documentRef the document to which the ratings are associated to
     * @return the ratings manager selected by looking at the current configuration settings
     */
    @Override
    public RatingsManager get(DocumentReference documentRef)
    {
        String defaultHint = "default";
        String ratingsHint =
            getXWiki().Param(
                RatingsManager.RATINGS_CONFIG_PARAM_PREFIX + RatingsManager.RATINGS_CONFIG_FIELDNAME_MANAGER_HINT,
                defaultHint);

        try {
            XWikiDocument configurationDocument = ratingsConfiguration.getConfigurationDocument(documentRef);
            if (!configurationDocument.isNew()
                && configurationDocument.getXObject(RatingsManager.RATINGS_CONFIG_CLASSREFERENCE) != null) {
                BaseProperty prop =
                    (BaseProperty) configurationDocument.getXObject(RatingsManager.RATINGS_CONFIG_CLASSREFERENCE).get(
                        RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_MANAGER_HINT);
                String hint = (prop == null) ? null : (String) prop.getValue();
                ratingsHint = (hint == null) ? ratingsHint : hint;
            }
        } catch (Exception e) {
            logger.error("Cannot read ratings config", e);
        }

        try {
            return componentManager.getInstance(RatingsManager.class, ratingsHint);
        } catch (ComponentLookupException e) {
            // TODO Auto-generated catch block
            logger.error("Error loading ratings manager component for hint " + ratingsHint, e);
            try {
                return componentManager.getInstance(RatingsManager.class, defaultHint);
            } catch (ComponentLookupException e1) {
                return null;
            }
        }
    }
}
