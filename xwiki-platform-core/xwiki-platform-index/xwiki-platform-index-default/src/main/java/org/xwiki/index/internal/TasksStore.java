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
package org.xwiki.index.internal;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.doc.tasks.XWikiDocumentIndexingTask;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;

/**
 * Provide the operations to interact with the tasks store.
 *
 * @version $Id$
 * @since 14.1RC1
 */
@Component(roles = TasksStore.class)
@Singleton
public class TasksStore extends XWikiHibernateBaseStore
{
    @Inject
    private ExecutionContextManager contextManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Retrieve the list of all the tasks queued for a given wiki for the current instance.
     *
     * @param wikiId the wiki in which to execute the query
     * @param instanceId the identifier of the cluster instance in which to execute the task. Each cluster member is
     *     in charge of consuming its own tasks
     * @return the list of all the task
     * @throws XWikiException in case of error when creating or executing the query
     */
    public List<XWikiDocumentIndexingTask> getAllTasks(String wikiId, String instanceId) throws XWikiException
    {
        return initWikiContext(xWikiContext -> executeRead(xWikiContext,
            session -> session.createQuery("SELECT t FROM XWikiDocumentIndexingTask t "
                    + "WHERE t.instanceId = :instanceId")
                .setParameter("instanceId", instanceId)
                .getResultList()), wikiId);
    }

    /**
     * Persist a task to the queue.
     *
     * @param wikiId the wiki in which to execute the query
     * @param task the task to persist
     * @throws XWikiException in case of error when saving the task
     */
    public void addTask(String wikiId, XWikiDocumentIndexingTask task) throws XWikiException
    {
        initWikiContext(xWikiContext -> {
            executeWrite(xWikiContext, session -> {
                innerAddTask(task, session);
                return null;
            });
            return null;
        }, wikiId);
    }

    /**
     * Remove a task from the queue.
     *
     * @param wikiId the wiki in which to execute the query
     * @param docId the docId to remove
     * @param version the version to remove
     * @param type the type of the task to remove
     * @throws XWikiException in case of error when removing the task
     */
    public void deleteTask(String wikiId, long docId, String version, String type) throws XWikiException
    {
        initWikiContext(xWikiContext -> {
            executeWrite(xWikiContext, session -> {
                String query = "delete from XWikiDocumentIndexingTask t where t.docId = :docId ";
                if (StringUtils.isEmpty(version)) {
                    // The is null part is required for Oracle.
                    query += "and (t.version = :version or t.version is null)";
                } else {
                    query += "and t.version = :version ";
                }
                query = query + "and t.type = :type";
                session.createQuery(query)
                    .setParameter("docId", docId)
                    .setParameter("version", version)
                    .setParameter("type", type)
                    .executeUpdate();
                return null;
            });
            return null;
        }, wikiId);
    }

    /**
     * Remove all tasks of the same type and document, regardless of tehe version, then add the new task to the queue.
     *
     * @param wikiId the wiki in which to execute the query
     * @param task the task replacing the previously queued tasks for the same document and the same type
     * @throws XWikiException in case of error when removing or adding the tasks
     */
    public void replaceTask(String wikiId, XWikiDocumentIndexingTask task) throws XWikiException
    {
        initWikiContext(xWikiContext -> {
            executeWrite(xWikiContext, session -> {
                session.createQuery("delete from XWikiDocumentIndexingTask t where t.docId = :docId "
                        + "and t.type = :type")
                    .setParameter("docId", task.getDocId())
                    .setParameter("type", task.getType())
                    .executeUpdate();
                innerAddTask(task, session);
                return null;
            });
            return null;
        }, wikiId);
    }

    /**
     * Return  an {@link XWikiDocument} by its id.
     *
     * @param wikiId the wiki in which to execute the query
     * @param docId the id of the document to retrive
     * @return the document
     * @throws XWikiException in case of error when executing the query
     */
    public XWikiDocument getDocument(String wikiId, long docId) throws XWikiException
    {
        return initWikiContext(context -> executeRead(context, session -> (XWikiDocument)
            session.createQuery("select doc from XWikiDocument doc where doc.id = :docId")
                .setParameter("docId", docId)
                .getSingleResult()
        ), wikiId);
    }

    private <T> T initWikiContext(Lambda<T> r, String wikiId) throws XWikiException
    {
        ExecutionContext executionContext = new ExecutionContext();
        try {
            this.contextManager.pushContext(executionContext, false);
            this.contextManager.initialize(executionContext);

            XWikiContext xWikiContext = this.xcontextProvider.get();
            xWikiContext.setWikiId(wikiId);
            return r.call(xWikiContext);
        } catch (Exception e) {
            throw new XWikiException("Failed to executed the task in the context", e);
        } finally {
            this.contextManager.popContext();
        }
    }

    private void innerAddTask(XWikiDocumentIndexingTask task, Session session)
    {
        // In case of inconsistent data. But the timestamp is expected to be initialized by the caller.
        if (task.getTimestamp() == null) {
            task.setTimestamp(new Date());
        }

        // Update allowed in case the same document is queued again with the same version and the same task on 
        // restart.
        session.saveOrUpdate(task);
    }

    /**
     * Internal functional interface with no return value and the possibility to throw exceptions.
     */
    @FunctionalInterface
    private interface Lambda<T>
    {
        T call(XWikiContext context) throws Exception;
    }
}
