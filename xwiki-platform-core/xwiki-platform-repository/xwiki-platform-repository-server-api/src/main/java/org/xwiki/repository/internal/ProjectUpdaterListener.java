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
package org.xwiki.repository.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.BeginEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Keep the project page up to date.
 * 
 * @version $Id$
 * @since 18.7.0RC1
 * @since 18.4.4
 */
@Component
@Named(ProjectUpdaterListener.NAME)
@Singleton
public class ProjectUpdaterListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "ProjectUpdaterListener";

    private static final BeginEvent IMPORT_PROCESS = new ProjectImportStartingEvent();

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Inject
    private Provider<RepositoryManager> repositoryManagerProvider;

    @Inject
    private ObservationContext observationContext;

    /**
     * The default constructor.
     */
    public ProjectUpdaterListener()
    {
        super(NAME, new DocumentCreatingEvent(), new DocumentUpdatingEvent(), new DocumentCreatedEvent(),
            new DocumentUpdatedEvent(), new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        if (!this.observationContext.isIn(IMPORT_PROCESS)) {
            if (event instanceof DocumentCreatingEvent || event instanceof DocumentUpdatingEvent) {
                if (isProjectPage(document)) {
                    // Make sure the last version of the project is up to date when creating/updating a project page
                    try {
                        this.repositoryManagerProvider.get().updateLastProjectVersion(document);
                    } catch (Exception e) {
                        this.logger.error("Failed to update last version for project [{}]",
                            document.getDocumentReference(), e);
                    }
                }
            } else if (isProjectVersionPage(document)) {
                // Update the last version of the project after modifying a version page, but only if not in the
                // middle of a project import (in which case it will be done at the end of the import)
                try {
                    // Get project page reference from the project version page
                    DocumentReference projectReference = getProjectReferenceFromVersionPage(document);

                    if (projectReference != null) {
                        // Update the last version of the project
                        this.repositoryManagerProvider.get().updateLastProjectVersion(projectReference);
                    }
                } catch (Exception e) {
                    this.logger.error("Failed to update project for version page [{}]", document.getDocumentReference(),
                        e);
                }
            }
        }
    }

    private DocumentReference getProjectReferenceFromVersionPage(XWikiDocument document)
    {
        EntityReference versionParent = document.getDocumentReference().getParent().getParent().getParent();
        if (versionParent != null) {
            return new DocumentReference("WebHome", (SpaceReference) versionParent);
        }
        return null;
    }

    private boolean isProjectPage(XWikiDocument document)
    {
        return document.getXObject(XWikiRepositoryModel.PROJECT_CLASSREFERENCE) != null;
    }

    private boolean isProjectVersionPage(XWikiDocument document)
    {
        return (document.getXObject(XWikiRepositoryModel.PROJECT_CLASSREFERENCE) == null
            && document.getXObject(XWikiRepositoryModel.PROJECTVERSION_CLASSREFERENCE) != null)
            || (document.getOriginalDocument().getXObject(XWikiRepositoryModel.PROJECT_CLASSREFERENCE) == null
                && document.getOriginalDocument()
                    .getXObject(XWikiRepositoryModel.PROJECTVERSION_CLASSREFERENCE) != null);
    }
}
