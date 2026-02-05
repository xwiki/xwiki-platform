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
package org.xwiki.job.store.internal;

import javax.inject.Named;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * Executes Hibernate operations in the main wiki.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@Component(roles = MainWikiHibernateExecutor.class)
@Singleton
public class MainWikiHibernateExecutor
{
    @Inject
    @Named("hibernate")
    private XWikiStoreInterface hibernateStore;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Execute a read-only Hibernate action.
     *
     * @param action the Hibernate action to execute
     * @param <T> the return type of the action
     * @return the result of the action
     * @throws JobStatusStoreException if an error occurs during the action execution
     */
    public <T> T executeRead(XWikiHibernateBaseStore.HibernateCallback<T> action) throws JobStatusStoreException
    {
        return execute(action, false);
    }

    /**
     * Execute a write Hibernate action.
     *
     * @param action the Hibernate action to execute
     * @throws JobStatusStoreException if an error occurs during the action execution
     */
    public void executeWrite(XWikiHibernateBaseStore.HibernateCallback<Void> action) throws JobStatusStoreException
    {
        execute(action, true);
    }

    private <T> T execute(XWikiHibernateBaseStore.HibernateCallback<T> action, boolean write)
        throws JobStatusStoreException
    {
        // Execute all database queries in the main wiki as jobs are global.
        XWikiContext context = this.contextProvider.get();
        String currentWiki = context.getWikiId();
        context.setWikiId(context.getMainXWiki());

        try {
            XWikiHibernateBaseStore store = (XWikiHibernateBaseStore) this.hibernateStore;
            if (write) {
                return store.executeWrite(context, action);
            } else {
                return store.executeRead(context, action);
            }
        } catch (Exception e) {
            throw new JobStatusStoreException("Failed to execute Hibernate operation.", e);
        } finally {
            context.setWikiId(currentWiki);
        }
    }

}
