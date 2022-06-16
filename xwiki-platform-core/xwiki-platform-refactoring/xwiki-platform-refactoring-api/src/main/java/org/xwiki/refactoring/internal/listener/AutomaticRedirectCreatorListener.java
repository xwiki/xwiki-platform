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
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.JobContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.internal.job.DeleteJob;
import org.xwiki.refactoring.job.DeleteRequest;
import org.xwiki.refactoring.job.MoveRequest;

/**
 * Creates an automatic redirect from the old location to the new location after a document has been renamed or deleted.
 * 
 * @version $Id$
 * @since 11.1RC1
 */
@Component
@Named(AutomaticRedirectCreatorListener.NAME)
@Singleton
public class AutomaticRedirectCreatorListener extends AbstractLocalEventListener
{
    /**
     * The name of this event listener.
     */
    public static final String NAME = "refactoring.automaticRedirectCreator";

    private static final String CREATING_AUTOMATIC_REDIRECT_FROM_TO = "Creating automatic redirect from [{}] to [{}].";

    @Inject
    private Logger logger;

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private JobContext jobContext;

    /**
     * Default constructor.
     */
    public AutomaticRedirectCreatorListener()
    {
        super(NAME, new DocumentRenamedEvent(), new DocumentDeletedEvent());
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        if (event instanceof DocumentRenamedEvent) {
            boolean autoRedirect = true;
            if (data instanceof MoveRequest) {
                autoRedirect = ((MoveRequest) data).isAutoRedirect();
            }
            if (autoRedirect) {
                DocumentRenamedEvent documentRenamedEvent = (DocumentRenamedEvent) event;
                this.logger.info(CREATING_AUTOMATIC_REDIRECT_FROM_TO, documentRenamedEvent.getSourceReference(),
                    documentRenamedEvent.getTargetReference());
                this.modelBridge.createRedirect(documentRenamedEvent.getSourceReference(),
                    documentRenamedEvent.getTargetReference());
            }
        } else if (event instanceof DocumentDeletedEvent && this.jobContext.getCurrentJob() instanceof DeleteJob) {
            DeleteJob job = (DeleteJob) this.jobContext.getCurrentJob();
            DeleteRequest request = (DeleteRequest) job.getRequest();
            DocumentDeletedEvent documentDeletedEvent = (DocumentDeletedEvent) event;
            DocumentReference newTarget =
                request.getNewBacklinkTargets().get(documentDeletedEvent.getDocumentReference());

            if (request.isAutoRedirect() && newTarget != null) {
                this.logger.info(CREATING_AUTOMATIC_REDIRECT_FROM_TO, documentDeletedEvent.getDocumentReference(),
                    newTarget);
                this.modelBridge.createRedirect(documentDeletedEvent.getDocumentReference(), newTarget);
            }
        }
    }
}
