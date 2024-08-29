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
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.job.MoveRequest;

/**
 * [legacy] Preserve existing parent-child relationships by updating the parent field of documents having the renamed
 * document as parent.
 * 
 * @version $Id$
 * @since 11.1RC1
 */
@Component
@Named(LegacyParentFieldUpdaterListener.NAME)
@Singleton
public class LegacyParentFieldUpdaterListener extends AbstractLocalEventListener
{
    /**
     * The name of this event listener.
     */
    public static final String NAME = "refactoring.legacyParentFieldUpdater";

    @Inject
    private Logger logger;

    @Inject
    private ModelBridge modelBridge;

    /**
     * Default constructor.
     */
    public LegacyParentFieldUpdaterListener()
    {
        super(NAME, new DocumentRenamedEvent());
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        boolean updateParentField = true;
        if (data instanceof MoveRequest) {
            updateParentField = ((MoveRequest) data).isUpdateParentField();
        }
        if (updateParentField) {
            DocumentRenamedEvent documentRenamedEvent = (DocumentRenamedEvent) event;
            this.logger.info("Updating the document parent fields from [{}] to [{}].",
                documentRenamedEvent.getSourceReference(), documentRenamedEvent.getTargetReference());
            this.modelBridge.updateParentField(documentRenamedEvent.getSourceReference(),
                documentRenamedEvent.getTargetReference());
        }
    }
}
