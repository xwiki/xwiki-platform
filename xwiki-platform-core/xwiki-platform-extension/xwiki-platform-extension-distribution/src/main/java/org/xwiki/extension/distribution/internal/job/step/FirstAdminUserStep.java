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
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.rightsmanager.RightsManager;

/**
 * Register a new owner if no user exist.
 * 
 * @version $Id$
 * @since 8.0RC1
 */
@Component
@Named(FirstAdminUserStep.ID)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class FirstAdminUserStep extends AbstractDistributionStep
{
    /**
     * The identifier of the step.
     */
    public static final String ID = "firstadminuser";

    @Inject
    private transient Logger logger;

    @Inject
    private transient Provider<XWikiContext> xcontextProvider;

    /**
     * Default constructor.
     */
    public FirstAdminUserStep()
    {
        super(ID);
    }

    @Override
    public void prepare()
    {
        if (getState() == null) {
            setState(State.COMPLETED);

            if (isMainWiki()) {
                try {
                    if (RightsManager.getInstance().countAllGlobalUsersOrGroups(true, null,
                        this.xcontextProvider.get()) == 0) {
                        // If there is no user register one
                        setState(null);
                    }
                } catch (XWikiException e) {
                    this.logger.error("Failed to count global users", e);
                }
            }
        }
    }
}
