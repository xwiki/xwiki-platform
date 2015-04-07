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
package org.xwiki.wiki.internal.provisioning;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.status.PopLevelProgressEvent;
import org.xwiki.job.event.status.PushLevelProgressEvent;
import org.xwiki.job.event.status.StepProgressEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.provisioning.WikiCopier;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Default implementation for {@link WikiCopier}.
 * @version $Id$
 * @since 7.0M2
 */
@Component
@Singleton
public class DefaultWikiCopier implements WikiCopier
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private ObservationManager observationManager;
    
    @Inject
    private Logger logger;

    @Override
    public void copyDocuments(String fromWikiId, String toWikiId, boolean withHistory) throws WikiManagerException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        try {
            Query query = queryManager.createQuery("select distinct doc.fullName from Document as doc", Query.XWQL);
            query.setWiki(fromWikiId);
            List<String> documentFullnames = query.execute();

            observationManager.notify(new PushLevelProgressEvent(documentFullnames.size()), this);

            WikiReference fromWikiReference = new WikiReference(fromWikiId);
            for (String documentFullName : documentFullnames) {
                DocumentReference origDocReference = documentReferenceResolver.resolve(documentFullName,
                        fromWikiReference);
                DocumentReference newDocReference = new DocumentReference(toWikiId,
                        origDocReference.getLastSpaceReference().getName(), origDocReference.getName());
                
                logger.info("Copying document [{}] to [{}].", origDocReference, newDocReference);
                xwiki.copyDocument(origDocReference, newDocReference, null, !withHistory, true, context);
                logger.info("Done copying document [{}] to [{}].", origDocReference, newDocReference);
                
                observationManager.notify(new StepProgressEvent(), this);
            }

            observationManager.notify(new PopLevelProgressEvent(), this);
        } catch (QueryException e) {
            WikiManagerException thrownException = 
                new WikiManagerException("Unable to get the list of wiki documents to copy.", e);
            logger.error(thrownException.getMessage(), thrownException);
            throw thrownException;
        } catch (XWikiException e) {
            WikiManagerException thrownException = new WikiManagerException("Failed to copy documents.", e);
            logger.error(thrownException.getMessage(), thrownException);
            throw thrownException;
        }

    }

    @Override
    public void copyDeletedDocuments(String fromWikiId, String toWikiId) throws WikiManagerException
    {
        throw new WikiManagerException("This method is not implemented yet");
    }
}
