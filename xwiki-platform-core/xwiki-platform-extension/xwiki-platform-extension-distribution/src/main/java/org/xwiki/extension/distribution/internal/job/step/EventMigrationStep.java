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
package org.xwiki.extension.distribution.internal.job.step;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.query.SimpleEventQuery;

/**
 * Migrate event from legacy to new store.
 * 
 * @version $Id$
 * @since 12.6.1
 * @since 12.7RC1
 */
@Component
@Named(EventMigrationStep.ID)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class EventMigrationStep extends AbstractDistributionStep
{
    /**
     * The identifier of the step.
     */
    public static final String ID = "eventmigration";

    @Inject
    private transient Logger logger;

    @Inject
    private EventStore eventStore;

    @Inject
    private ComponentManager componentManager;

    /**
     * Default constructor.
     */
    public EventMigrationStep()
    {
        super(ID);
    }

    @Override
    public void prepare()
    {
        if (getState() == null) {
            setState(State.COMPLETED);

            if (this.componentManager.hasComponent(EventStream.class)) {
                try {
                    EventStream eventStream = this.componentManager.getInstance(EventStream.class);

                    long legacyCount = eventStream.countEvents();
                    long eventCount = this.eventStore.search(new SimpleEventQuery(0, 0)).getTotalHits();

                    if (legacyCount > eventCount) {
                        // There is more legacy events than new store events
                        setState(null);
                    }
                } catch (Exception e) {
                    this.logger.error("Failed to compare legacy and new store events count", e);
                }
            }
        }
    }
}
