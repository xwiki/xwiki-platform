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
package org.xwiki.ratings.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.ratings.RatingsException;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.RatingsManagerFactory;
import org.xwiki.script.service.ScriptService;

/**
 * Script service to manipulate ratings for different ratings hints.
 * This service might be used in two ways: by default it performs calls on the default Ratings Application hint
 * (see {@link RatingsManagerFactory#DEFAULT_APP_HINT}), but it can be used with the hint of a custom application e.g.
 * {@code $services.ratings.foo} will call the methods for a {@link RatingsManager} identified by {@code foo}.
 * For more information, see the documentation of {@link RatingsManagerFactory}.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Singleton
@Named("ratings")
public class RatingsScriptService extends AbstractScriptRatingsManager implements Initializable, ScriptService
{
    static final String EXECUTION_CONTEXT_PREFIX = "ratings.script.";

    @Inject
    private RatingsManagerFactory ratingsManagerFactory;

    @Inject
    private Execution execution;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            setRatingsManager(this.ratingsManagerFactory.getRatingsManager(RatingsManagerFactory.DEFAULT_APP_HINT));
        } catch (RatingsException e) {
            throw new InitializationException("Error when trying to retrieve the instance of RatingsManager", e);
        }
    }

    /**
     * Retrieve a specific {@link DefaultScriptRatingsManager} for the given hint.
     * This method will in fact instantiate the right {@link RatingsManager} for the given hint, and create a
     * {@link DefaultScriptRatingsManager} around it to allow performing all script service calls on it.
     * Note that the method also caches the instantiated {@link DefaultScriptRatingsManager} in the
     * {@link ExecutionContext} so that it's kept during the execution of a script.
     *
     * @param managerHint the hint of a {@link RatingsManager} to use. See {@link RatingsManagerFactory} for more
     *                    information.
     * @return a wrapper around the {@link RatingsManager} to perform the script service calls.
     */
    public DefaultScriptRatingsManager get(String managerHint)
    {
        ExecutionContext executionContext = this.execution.getContext();
        String executionContextCacheKey = EXECUTION_CONTEXT_PREFIX + managerHint;

        DefaultScriptRatingsManager scriptRatingsManager = null;
        if (executionContext.hasProperty(executionContextCacheKey)) {
            scriptRatingsManager = (DefaultScriptRatingsManager) executionContext.getProperty(executionContextCacheKey);
        } else {
            try {
                RatingsManager ratingsManager = this.ratingsManagerFactory.getRatingsManager(managerHint);
                scriptRatingsManager = this.componentManager.getInstance(DefaultScriptRatingsManager.class);
                scriptRatingsManager.setRatingsManager(ratingsManager);
                executionContext.setProperty(executionContextCacheKey, scriptRatingsManager);
            } catch (RatingsException e) {
                this.logger.error(
                    String.format("Error when trying to retrieve RatingsManager instance with hint [%s]", managerHint),
                    e);
            } catch (ComponentLookupException e) {
                this.logger.error(
                    String.format("Error when trying to retrieve DefaultScriptRatingsManager instance with hint [%s]",
                        managerHint),
                    e);
            }
        }

        return scriptRatingsManager;
    }
}
