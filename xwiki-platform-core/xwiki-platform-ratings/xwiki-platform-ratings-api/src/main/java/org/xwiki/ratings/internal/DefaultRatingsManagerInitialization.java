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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.ratings.RatingsManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * The default ratings manager initialization.
 * 
 * @version $Id$
 */
@Component
@Named("ratingswikiinit")
@Singleton
// TODO: refactor to MandatoryDocumentInitializer
public class DefaultRatingsManagerInitialization implements EventListener, Initializable
{
    private static final String BASE_TYPE_INTEGER = "integer";

    private static final String BASE_TYPE_FLOAT = "float";

    private static final String XWIKI_ADMIN = "xwiki:XWiki.Admin";

    private static final LocalDocumentReference XWIKICLASSES_REFERENCE = new LocalDocumentReference("XWiki",
        "XWikiClasses");

    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(new WikiReadyEvent());
    }

    /**
     * Gets the component name.
     * 
     * @return the component name
     */
    @Override
    public String getName()
    {
        return "ratingswikiinit";
    }

    /**
     * Retrieve the XWiki context from the current execution context.
     * 
     * @return the XWiki context.
     * @throws RuntimeException if there was an error retrieving the context.
     */
    protected XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    /**
     * Retrieve the XWiki private API object.
     * 
     * @return the XWiki private API object.
     */
    protected XWiki getXWiki()
    {
        return getXWikiContext().getWiki();
    }

    @Override
    public void initialize() throws InitializationException
    {
    }

    @Override
    public void onEvent(Event wikiReadyEvent, Object arg1, Object arg2)
    {
        // making sure the classes exist
        initRatingsClass();
        initAverageRatingsClass();
    }

    /**
     * Initialize the AverageRatingsClass.
     */
    private void initAverageRatingsClass()
    {
        try {
            XWikiDocument doc;
            XWiki xwiki = getXWiki();
            boolean needsUpdate = false;

            doc = xwiki.getDocument(RatingsManager.AVERAGE_RATINGS_CLASSREFERENCE, getXWikiContext());
            BaseClass bclass = doc.getXClass();

            needsUpdate |=
                bclass.addNumberField(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_NBVOTES, "Number of Votes", 5,
                    BASE_TYPE_INTEGER);
            needsUpdate |=
                bclass.addNumberField(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE, "Average Vote", 5,
                    BASE_TYPE_FLOAT);
            needsUpdate |=
                bclass.addTextField(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD,
                    "Average Vote method", 10);

            if (StringUtils.isBlank(doc.getAuthor())) {
                needsUpdate = true;
                doc.setAuthor(XWIKI_ADMIN);
            }
            if (StringUtils.isBlank(doc.getCreator())) {
                needsUpdate = true;
                doc.setCreator(XWIKI_ADMIN);
            }
            if (StringUtils.isBlank(doc.getParent())) {
                needsUpdate = true;
                doc.setParentReference(XWIKICLASSES_REFERENCE);
            }

            String title = doc.getTitle();
            if ((title == null) || (title.equals(""))) {
                needsUpdate = true;
                doc.setTitle("XWiki Average Ratings Class");
            }

            if (needsUpdate) {
                xwiki.saveDocument(doc, getXWikiContext());
            }
        } catch (Exception e) {
            logger.error("Error while initializing average ratings class", e);
        }
    }

    /**
     * Initialize the RatingsClass.
     */
    private void initRatingsClass()
    {
        try {
            XWikiDocument doc;
            XWiki xwiki = getXWiki();
            boolean needsUpdate = false;

            doc = xwiki.getDocument(RatingsManager.RATINGS_CLASSREFERENCE, getXWikiContext());
            BaseClass bclass = doc.getXClass();

            needsUpdate |= bclass.addTextField(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR, "Author", 30);
            needsUpdate |=
                bclass.addNumberField(RatingsManager.RATING_CLASS_FIELDNAME_VOTE, "Vote", 5, BASE_TYPE_INTEGER);
            needsUpdate |= bclass.addDateField(RatingsManager.RATING_CLASS_FIELDNAME_DATE, "Date");
            needsUpdate |= bclass.addTextField(RatingsManager.RATING_CLASS_FIELDNAME_PARENT, "Parent", 30);

            if (StringUtils.isBlank(doc.getAuthor())) {
                needsUpdate = true;
                doc.setAuthor(XWIKI_ADMIN);
            }
            if (StringUtils.isBlank(doc.getCreator())) {
                needsUpdate = true;
                doc.setCreator(XWIKI_ADMIN);
            }
            if (StringUtils.isBlank(doc.getParent())) {
                needsUpdate = true;
                doc.setParentReference(XWIKICLASSES_REFERENCE);
            }

            String title = doc.getTitle();
            if ((title == null) || (title.equals(""))) {
                needsUpdate = true;
                doc.setTitle("XWiki Ratings Class");
            }

            if (needsUpdate) {
                xwiki.saveDocument(doc, getXWikiContext());
            }
        } catch (Exception e) {
            logger.error("Error while initializing ratings class", e);
        }
    }
}
