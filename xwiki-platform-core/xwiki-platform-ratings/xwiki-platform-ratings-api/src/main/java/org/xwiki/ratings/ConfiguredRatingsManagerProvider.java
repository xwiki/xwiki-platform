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

@Component
@Singleton
public class ConfiguredRatingsManagerProvider implements ConfiguredProvider<RatingsManager>
{
    @Inject
    Logger logger;

    @Inject
    Execution execution;

    @Inject
    ComponentManager componentManager;

    /**
     * <p>
     * Retrieve the XWiki context from the current execution context
     * </p>
     * 
     * @return The XWiki context.
     * @throws RuntimeException If there was an error retrieving the context.
     */
    protected XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    /**
     * <p>
     * Retrieve the XWiki private API object
     * </p>
     * 
     * @return The XWiki private API object.
     */
    protected XWiki getXWiki()
    {
        return getXWikiContext().getWiki();
    }

    /**
     * Retrieve an instance of the desired RatingsManager (default/separate) - default: save the rating information in
     * the same page - separate: save the rating information in a specified space
     *
     * @return the ratings manager selected by looking at the current configuration settings
     */
    @Override
    public RatingsManager get(DocumentReference documentRef)
    {
        // TODO implement
        String ratingsHint =
            getXWiki().Param(
                RatingsManager.RATINGS_CONFIG_PARAM_PREFIX + RatingsManager.RATINGS_CONFIG_FIELDNAME_MANAGER_HINT,
                "default");

        try {
            XWikiDocument ratingDocument = getXWiki().getDocument(documentRef, getXWikiContext());
            XWikiDocument spaceConfigDoc =
                getXWiki().getDocument(ratingDocument.getSpace(), RatingsManager.RATINGS_CONFIG_SPACE_PAGE,
                    getXWikiContext());
            XWikiDocument globalConfigDoc =
                getXWiki().getDocument(RatingsManager.RATINGS_CONFIG_GLOBAL_PAGE, getXWikiContext());
            XWikiDocument configDoc =
                (spaceConfigDoc.getObject(RatingsManager.RATINGS_CONFIG_CLASSNAME) == null) ? globalConfigDoc
                    : spaceConfigDoc;

            if (!configDoc.isNew() && configDoc.getObject(RatingsManager.RATINGS_CONFIG_CLASSNAME) != null) {
                BaseProperty prop =
                    (BaseProperty) configDoc.getObject(RatingsManager.RATINGS_CONFIG_CLASSNAME).get(
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
                return componentManager.getInstance(RatingsManager.class, "default");
            } catch (ComponentLookupException e1) {
                return null;
            }
        }
    }
}
