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
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentCopiedEvent;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.internal.LinkRefactoring;
import org.xwiki.refactoring.internal.event.AbstractEntityCopyOrRenameEvent;
import org.xwiki.refactoring.job.AbstractCopyOrMoveRequest;

/**
 * Updates the relative links from the content of a new document that is the result of a rename or copy operation.
 * 
 * @version $Id$
 * @since 11.1RC1
 */
@Component
@Named(RelativeLinkUpdaterListener.NAME)
@Singleton
public class RelativeLinkUpdaterListener extends AbstractEventListener
{
    /**
     * The name of this event listener.
     */
    public static final String NAME = "refactoring.relativeLinksUpdater";

    @Inject
    private Logger logger;

    @Inject
    private LinkRefactoring linkRefactoring;

    /**
     * Default constructor.
     */
    public RelativeLinkUpdaterListener()
    {
        super(NAME, new DocumentCopiedEvent(), new DocumentRenamedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        boolean updateRelativeLinks =
            data instanceof AbstractCopyOrMoveRequest ? ((AbstractCopyOrMoveRequest) data).isUpdateLinks() : true;
        if (updateRelativeLinks && (event instanceof DocumentCopiedEvent || event instanceof DocumentRenamedEvent)) {
            @SuppressWarnings("unchecked")
            AbstractEntityCopyOrRenameEvent<DocumentReference> copyOrRenameEvent =
                (AbstractEntityCopyOrRenameEvent<DocumentReference>) event;
            this.logger.info("Updating the relative links from [{}].", copyOrRenameEvent.getTargetReference());
            this.linkRefactoring.updateRelativeLinks(copyOrRenameEvent.getSourceReference(),
                copyOrRenameEvent.getTargetReference());
        }
    }
}
