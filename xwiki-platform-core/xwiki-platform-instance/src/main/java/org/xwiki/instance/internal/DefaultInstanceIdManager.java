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
package org.xwiki.instance.internal;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.instance.InstanceId;
import org.xwiki.instance.InstanceIdManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * Allow initializing and retrieving the unique instance id.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
public class DefaultInstanceIdManager implements InstanceIdManager, Initializable
{
    /**
     * Used to store the new instance id if none exists already.
     *
     * Note that we use a Provider so that this component can be injected into a Listener (Listeners are initialized
     * very early and before the store is ready).
     */
    @Inject
    @Named("hibernate")
    private Provider<XWikiStoreInterface> hibernateStoreProvider;

    /**
     * Used to get the XWiki Context.
     */
    @Inject
    private Execution execution;

    /**
     * Log in case of problem.
     */
    @Inject
    private Logger logger;

    /**
     * The unique instance id, cached for performances so that we don't get it from the database every time some client
     * code needs it.
     */
    private InstanceId instanceId;

    @Override
    public InstanceId getInstanceId()
    {
        return this.instanceId;
    }

    @Override
    public void initialize()
    {
        // Load it from the database
        XWikiContext context = getXWikiContext();
        XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStoreProvider.get();

        // Try retrieving the UUID from the database

        // First ensure that we're on the main wiki since we store the unique id only on the main wiki
        String originalDatabase = context.getWikiId();
        context.setWikiId(context.getMainXWiki());

        try {
            InstanceId id = store.failSafeExecuteRead(context, session -> {
                // Retrieve the id from the database
                CriteriaBuilder builder = session.getCriteriaBuilder();
                CriteriaQuery<InstanceId> query = builder.createQuery(InstanceId.class);
                Root<InstanceId> root = query.from(InstanceId.class);
                query.select(root);
                return session.createQuery(query).getSingleResult();
            });

            // If the database doesn't hold the UUID then compute one and save it
            if (id == null) {
                // Compute UUID
                final InstanceId newId = new InstanceId(UUID.randomUUID().toString());
                // Store it. Note that this can fail in which case no UUID is saved in the DB and the operation
                // will be retried again next time the wiki is restarted.
                try {
                    store.executeWrite(context, session -> {
                        session.createQuery("delete from " + InstanceId.class.getName()).executeUpdate();
                        session.save(newId);
                        return null;
                    });
                } catch (XWikiException e) {
                    this.logger.warn("Failed to save Instance id to database. Reason: [{}]",
                        ExceptionUtils.getRootCauseMessage(e));
                }
                id = newId;
            }

            this.instanceId = id;
        } finally {
            // Restore original database
            context.setWikiId(originalDatabase);
        }
    }

    /**
     * @return XWikiContext
     */
    private XWikiContext getXWikiContext()
    {
        ExecutionContext context = this.execution.getContext();
        return (XWikiContext) context.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
