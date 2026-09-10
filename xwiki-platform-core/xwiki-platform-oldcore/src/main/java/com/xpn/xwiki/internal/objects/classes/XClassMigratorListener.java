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
package com.xpn.xwiki.internal.objects.classes;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.internal.event.XClassUpdatedEvent;

/**
 * Listen to classes modifications and automatically update objects accordingly when needed.
 *
 * @version $Id$
 * @since 7.1RC1
 */
@Component
@Singleton
@Named("XClassMigratorListener")
@SuppressWarnings("ClassFanOutComplexity")
public class XClassMigratorListener extends AbstractEventListener
{
    @Inject
    private Logger logger;

    @Inject
    private JobExecutor jobExecutor;

    /**
     * Set up the listener.
     */
    public XClassMigratorListener()
    {
        super(XClassMigratorListener.class.getName(), new XClassUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof XClassUpdatedEvent ev && data instanceof Collection<?> updatedProperties) {
            XClassMigratorRequest request = new XClassMigratorRequest();
            request.setClassToMigrate(ev.getReference());
            request.setUpdatedProperties(updatedProperties);
            request.setId("classMigration",
                String.format("%d-%d", System.currentTimeMillis(), ThreadLocalRandom.current().nextInt(100, 1000)));
            try {
                this.jobExecutor.execute(XClassMigratorJob.JOB_TYPE, request);
            } catch (JobException e) {
                logger.error("Migration of class [{}] failed", ev.getReference(), e);
            }
        } else {
            logger.error("Unexpected event of type [{}] or unexpected data of type [{}], this should not happen",
                    event == null ? null : event.getClass(), data == null ? null : data.getClass());
        }
    }
}
