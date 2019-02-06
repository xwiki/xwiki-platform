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
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.job.MoveRequest;

/**
 * Creates an automatic redirect from the old location to the new location after a document has been renamed.
 * 
 * @version $Id$
 * @since 11.1RC1
 */
@Component
@Named(AutomaticRedirectCreatorListener.NAME)
@Singleton
public class AutomaticRedirectCreatorListener extends AbstractEventListener
{
    /**
     * The name of this event listener.
     */
    public static final String NAME = "refactoring.automaticRedirectCreator";

    @Inject
    private Logger logger;

    @Inject
    private ModelBridge modelBridge;

    /**
     * Default constructor.
     */
    public AutomaticRedirectCreatorListener()
    {
        super(NAME, new DocumentRenamedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof DocumentRenamedEvent) {
            boolean autoRedirect = true;
            if (data instanceof MoveRequest) {
                autoRedirect = ((MoveRequest) data).isAutoRedirect();
            }
            if (autoRedirect) {
                DocumentRenamedEvent documentRenamedEvent = (DocumentRenamedEvent) event;
                this.logger.info("Creating automatic redirect from [{}] to [{}].",
                    documentRenamedEvent.getOldReference(), documentRenamedEvent.getNewReference());
                this.modelBridge.createRedirect(documentRenamedEvent.getOldReference(),
                    documentRenamedEvent.getNewReference());
            }
        }
    }
}
