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
package org.xwiki.refactoring.internal.listener;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.job.Job;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.ReplaceUserRequest;
import org.xwiki.refactoring.script.RequestFactory;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.rightsmanager.RightsManager;

/**
 * Update the rights and groups after a user or a group has been renamed.
 * It also update the authors reference if it's a user rename,
 * see {@link org.xwiki.refactoring.internal.job.ReplaceUserJob} for more details.
 *
 * @version $Id$
 * @since 11.9RC1
 */
@Component
@Named(UpdateRightsOnDocumentRenameListener.NAME)
@Singleton
public class UpdateRightsOnDocumentRenameListener extends AbstractLocalEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.refactoring.internal.listener.UpdateRightsOnDocumentRenameListener";

    private static final String GROUPS_CLASSNAME = "XWiki.XWikiGroups";
    private static final String USERS_CLASSNAME = "XWiki.XWikiUsers";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("default")
    private EntityReferenceResolver<String> stringEntityReferenceResolver;

    @Inject
    private RequestFactory requestFactory;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    private EntityReference groupClassReference;
    private EntityReference userClassReference;

    /**
     * Default constructor.
     */
    public UpdateRightsOnDocumentRenameListener()
    {
        super(NAME, new DocumentRenamedEvent());
    }

    /**
     * @return the reference for XWikiGroups class
     */
    private EntityReference getGroupClassReference()
    {
        if (this.groupClassReference == null) {
            this.groupClassReference =
                this.stringEntityReferenceResolver.resolve(GROUPS_CLASSNAME, EntityType.DOCUMENT);
        }
        return this.groupClassReference;
    }

    /**
     * @return the reference for XWikUsers class
     */
    private EntityReference getUserClassReference()
    {
        if (this.userClassReference == null) {
            this.userClassReference =
                this.stringEntityReferenceResolver.resolve(USERS_CLASSNAME, EntityType.DOCUMENT);
        }
        return userClassReference;
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        DocumentRenamedEvent documentRenamedEvent = (DocumentRenamedEvent) event;
        DocumentReference sourceReference = documentRenamedEvent.getSourceReference();
        DocumentReference targetReference = documentRenamedEvent.getTargetReference();

        this.processDocumentRenamed(sourceReference, targetReference);
    }

    private void processDocumentRenamed(DocumentReference sourceReference, DocumentReference targetReference)
    {
        XWikiContext context = contextProvider.get();
        try {
            // first we check if the document being renamed contains a group or a user object to see if there's
            // actually a refactoring to do.
            XWikiDocument document = context.getWiki().getDocument(targetReference, context);
            boolean definesGroup = document.getXObject(getGroupClassReference()) != null;
            boolean definesUser = document.getXObject(getUserClassReference()) != null;

            // If the document contains a group object, then we need to rename the rights and groups
            // with the new group reference.
            if (definesGroup) {
                this.refactorRightsAndGroupsInAllWikis(sourceReference, targetReference, false, context);
            }

            // If the document contains a user object, then we need to rename the rights with the new user reference,
            // and we also needs to update the author reference where it's needed.
            if (definesUser) {
                this.refactorRightsAndGroupsInAllWikis(sourceReference, targetReference, true, context);
                this.updateAuthors(sourceReference, targetReference);
            }
        } catch (XWikiException e) {
            this.logger.error("Error while updating rights objects after a rename of user or group [{}].",
                sourceReference, e);
        } catch (ComponentLookupException | InterruptedException e) {
            this.logger.error("Error while updating authors after a rename of user or group [{}].",
                sourceReference, e);
        } catch (WikiManagerException e) {
            this.logger.error("Error while looping over all wikis to update rights objects after "
                    + "a rename of user or group [{}].", sourceReference, e);
        }
    }

    private void refactorRightsAndGroupsInAllWikis(DocumentReference sourceReference, DocumentReference targetReference,
        boolean isUser, XWikiContext context)
        throws WikiManagerException, XWikiException
    {
        String currentWiki = context.getWikiId();
        try {
            for (String wikiId : wikiDescriptorManager.getAllIds()) {
                context.setWikiId(wikiId);
                RightsManager.getInstance().replaceUserOrGroupFromAllRights(sourceReference, targetReference,
                    isUser, context);
                context.getWiki().getGroupService(context)
                    .replaceMemberInAllGroups(sourceReference, targetReference, context);
            }
        } finally {
            context.setWikiId(currentWiki);
        }
    }

    private void updateAuthors(DocumentReference sourceReference, DocumentReference targetReference)
        throws ComponentLookupException, InterruptedException
    {
        ReplaceUserRequest replaceUserRequest =
            this.requestFactory.createReplaceUserRequest(sourceReference, targetReference);
        replaceUserRequest.setReplaceDocumentAuthor(true);
        replaceUserRequest.setReplaceDocumentContentAuthor(true);
        replaceUserRequest.setReplaceDocumentCreator(true);

        // The author of the script shouldn't be tested it since we're in a listener.
        replaceUserRequest.setCheckAuthorRights(false);

        Job replaceUserJob = componentManager.getInstance(Job.class, RefactoringJobs.REPLACE_USER);
        replaceUserJob.initialize(replaceUserRequest);
        replaceUserJob.run();
        replaceUserJob.join();
    }
}
