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
package org.xwiki.search.solr.internal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.api.SolrConfiguration;

/**
 * Gives access to a Solr core which can be resolved the first time it's needed instead of when the component using it
 * is initialized.
 * <p>
 * With a remote Solr server, a server which cannot be reached while XWiki starts must not make the components using one
 * of its cores (and the components depending on them, like event listeners) unavailable until XWiki is restarted. After
 * a failed attempt (including the one made during the initialization of the component), the core is requested again
 * when needed, but not more than once every 10 seconds; in between, and while another thread is already requesting
 * the core, the operations fail immediately with the last failure instead of each waiting for the Solr server. A
 * warning is logged when the core becomes unavailable and an information message when it is available again.
 * <p>
 * Each instance gives access to one core: a component injects its own instance and calls {@link #initialize(String)}
 * with the name of the core during its own initialization, then {@link #get()} each time it needs the core.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component(roles = LazySolrCore.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class LazySolrCore
{
    /**
     * The minimum time between two attempts to get the core after a failure.
     */
    private static final long RETRY_DELAY = TimeUnit.SECONDS.toNanos(10);

    /**
     * Get the {@link Solr} component only when the core is requested: a Solr component which cannot be initialized yet
     * (because the Solr server cannot be reached for example) is then requested again later.
     */
    @Inject
    private Provider<Solr> solrProvider;

    @Inject
    private SolrConfiguration configuration;

    @Inject
    private Logger logger;

    private final Lock lock = new ReentrantLock();

    /**
     * The name of the core, {@code null} until {@link #initialize(String)} is called.
     */
    private volatile String name;

    /**
     * The minimum time in nanoseconds between two attempts to get the core after a failure.
     */
    private long retryDelay = RETRY_DELAY;

    private volatile XWikiSolrCore core;

    /**
     * The last failure to get the core, {@code null} if the core was never requested or if the last attempt succeeded.
     */
    private volatile SolrException failure;

    /**
     * The time ({@link System#nanoTime()}) before which the core is not requested again after a failure.
     */
    private volatile long retryTime;

    /**
     * Sets the core to give access to and gets it now, during the initialization of the component using it, so that it
     * is created, migrated and initialized during the startup when the Solr server is available. The failure handling
     * depends on the type of Solr server:
     * <ul>
     * <li>with a remote Solr server, an unavailable core can be a server which cannot be reached yet: the failure is
     * logged and handled like any other failed attempt, the core is requested again when needed (see {@link #get()})
     * </li>
     * <li>with any other type of Solr server (the embedded Solr), a core which cannot be loaded is a configuration
     * error, not a temporary condition: the failure is thrown to the caller, without being logged</li>
     * </ul>
     * Must be called once, before {@link #get()}.
     *
     * @param coreName the name of the core, for example {@code search}
     * @return the core, or {@code null} if a remote Solr server is used and the core is not available
     * @throws SolrException when a Solr server other than a remote one is used and the core cannot be loaded
     * @throws IllegalStateException when this instance was already initialized
     */
    public XWikiSolrCore initialize(String coreName) throws SolrException
    {
        if (this.name != null) {
            throw new IllegalStateException(
                String.format("This instance was already initialized with the Solr core [%s]", this.name));
        }

        this.name = coreName;

        if (RemoteSolr.TYPE.equals(this.configuration.getServerType())) {
            try {
                return get();
            } catch (SolrException e) {
                // Already logged
                return null;
            }
        }

        this.lock.lock();
        try {
            if (this.core == null) {
                this.core = getCore();
            }

            return this.core;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Gets the core, requesting it from Solr if it was not resolved yet. After a failure, the core is not requested
     * again before the retry delay, and while another thread is requesting it: the last failure is reported instead.
     *
     * @return the core, never {@code null}
     * @throws SolrException when the core cannot be found or the Solr server cannot be reached
     * @throws IllegalStateException when {@link #initialize(String)} was not called
     */
    public XWikiSolrCore get() throws SolrException
    {
        XWikiSolrCore solrCore = this.core;
        if (solrCore != null) {
            return solrCore;
        }

        if (this.name == null) {
            throw new IllegalStateException("The Solr core is requested before the initialization of this instance");
        }

        if (!this.lock.tryLock()) {
            // Another thread is requesting the core: don't wait for it (it can take as long as the connection timeout)
            // if a failure is already known. Once this instance is initialized, no failure is known only when that
            // thread has just resolved the core (the failure is cleared before the core is set): waiting for the lock
            // is then short, and resolve() returns that core.
            SolrException lastFailure = this.failure;
            if (lastFailure != null) {
                throw new SolrException(lastFailure.getMessage(), lastFailure);
            }

            this.lock.lock();
        }

        try {
            return resolve();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * @param retryDelay the minimum time in nanoseconds between two attempts to get the core after a failure
     */
    void setRetryDelay(long retryDelay)
    {
        this.retryDelay = retryDelay;
    }

    private XWikiSolrCore resolve() throws SolrException
    {
        if (this.core == null) {
            SolrException lastFailure = this.failure;
            if (lastFailure != null && System.nanoTime() - this.retryTime < 0) {
                throw new SolrException(lastFailure.getMessage(), lastFailure);
            }

            XWikiSolrCore solrCore;
            try {
                solrCore = getCore();
            } catch (SolrException e) {
                onFailure(e);

                throw e;
            }

            if (lastFailure != null) {
                this.logger.info("The Solr core [{}] is available again", this.name);

                this.failure = null;
            }

            this.core = solrCore;
        }

        return this.core;
    }

    private XWikiSolrCore getCore() throws SolrException
    {
        Solr solr;
        try {
            solr = this.solrProvider.get();
        } catch (RuntimeException e) {
            // The component Provider reports a component which fails to initialize as a RuntimeException.
            throw new SolrException("Failed to get the Solr component", e);
        }

        XWikiSolrCore solrCore = solr.getCore(this.name);
        if (solrCore == null) {
            throw new SolrException(String.format("The Solr core [%s] could not be found: with a remote Solr server it"
                + " must be created beforehand, with the name [<corePrefix>_%s_<Solr major version>]", this.name,
                this.name));
        }

        return solrCore;
    }

    private void onFailure(SolrException e)
    {
        if (this.failure == null) {
            this.logger.warn("The Solr core [{}] is not available: [{}]. Check that the Solr server can be reached and"
                + " that the core exists on it. The core will be requested again when needed.", this.name,
                ExceptionUtils.getRootCauseMessage(e));
        } else {
            this.logger.debug("The Solr core [{}] is still not available: [{}]", this.name,
                ExceptionUtils.getRootCauseMessage(e));
        }

        // Set the retry time first: the failure is read without the lock.
        this.retryTime = System.nanoTime() + this.retryDelay;
        this.failure = e;
    }
}
