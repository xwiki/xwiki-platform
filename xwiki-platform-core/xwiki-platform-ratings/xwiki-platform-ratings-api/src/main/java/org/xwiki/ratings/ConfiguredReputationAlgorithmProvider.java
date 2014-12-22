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
 * Retrieve the appropriate ReputationAlgorithm by looking at the current configuration settings.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class ConfiguredReputationAlgorithmProvider implements ConfiguredProvider<ReputationAlgorithm>
{
    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private ConfiguredProvider<RatingsManager> ratingsManagerProvider;

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
     * Retrieve an instance of the desired ReputationAlorithm (default/simple/custom).
     *
     * @param documentRef documentRef the document to which the ratings are associated to
     * @return the reputation algorithm selected by looking at the current configuration settings
     */
    @Override
    public ReputationAlgorithm get(DocumentReference documentRef)
    {
        String defaultAlgorithmHint = "default";
        String reputationAlgorithmHint =
            getXWiki().Param(
                RatingsManager.RATINGS_CONFIG_PARAM_PREFIX
                    + RatingsManager.RATINGS_CONFIG_FIELDNAME_REPUTATIONALGORITHM_HINT, defaultAlgorithmHint);

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

            if (configDoc != null && !configDoc.isNew()
                && configDoc.getObject(RatingsManager.RATINGS_CONFIG_CLASSNAME) != null) {
                BaseProperty prop =
                    (BaseProperty) configDoc.getObject(RatingsManager.RATINGS_CONFIG_CLASSNAME).get(
                        RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION_ALGORITHM_HINT);
                String hint = (prop == null) ? null : (String) prop.getValue();
                if (hint == "custom") {
                    prop =
                        (BaseProperty) configDoc.getObject(RatingsManager.RATINGS_CONFIG_CLASSNAME).get(
                            RatingsManager.RATINGS_CONFIG_CLASS_FIELDNAME_REPUTATION_CUSTOM_ALGORITHM);
                    hint = (prop == null) ? null : (String) prop.getValue();
                }
                reputationAlgorithmHint = (hint == null) ? reputationAlgorithmHint : hint;
            }
        } catch (Exception e) {
            logger.error("Cannot read reputation algorithm config", e);
        }

        // if the reputation algorithm hint is a page let's try to get the instance from groovy
        if (reputationAlgorithmHint.contains(".")) {
            try {
                ReputationAlgorithmGroovy reputationInstance =
                    (ReputationAlgorithmGroovy) getXWiki().parseGroovyFromPage(reputationAlgorithmHint,
                        getXWikiContext());

                if (reputationInstance != null) {
                    reputationInstance.setComponentManager(componentManager);
                    reputationInstance.setExecution(execution);
                    reputationInstance.setXWikiContext(getXWikiContext());
                    reputationInstance.setRatingsManager(ratingsManagerProvider.get(documentRef));
                    return reputationInstance;
                }
            } catch (Throwable e) {
                logger.error("Cannot instanciate Reputation algorithm from page " + reputationAlgorithmHint, e);
            }
        }

        try {
            return componentManager.getInstance(ReputationAlgorithm.class, reputationAlgorithmHint);
        } catch (ComponentLookupException e) {
            // TODO Auto-generated catch block
            logger.error("Error loading ratings manager component for hint " + reputationAlgorithmHint, e);
            try {
                return componentManager.getInstance(ReputationAlgorithm.class, defaultAlgorithmHint);
            } catch (ComponentLookupException e1) {
                return null;
            }
        }
    }
}
