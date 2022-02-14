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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.index.IndexException;
import org.xwiki.index.TaskConsumer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * This component is in charge of executing the tasks.
 *
 * @version $Id$
 * @since 14.1RC1
 */
@Component(roles = TaskExecutor.class)
@Singleton
public class TaskExecutor
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Provider<TasksStore> tasksStore;

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Inject
    private Provider<ComponentManager> componentManager;

    /**
     * Execute a task by initializing its context, and resolving the task consumer according to the task type.
     *
     * @param task the task to execute
     * @throws IndexException in case of error when executing the task
     */
    public void execute(TaskData task) throws IndexException
    {
        XWikiContext context = this.contextProvider.get();
        String oldWikId = context.getWikiId();
        try {
            context.setWikiId(task.getWikiId());
            XWikiDocument document = this.tasksStore.get().getDocument(task.getWikiId(), task.getDocId());
            XWikiDocument doc = this.documentRevisionProvider.getRevision(document, task.getVersion());
            this.componentManager.get().<TaskConsumer>getInstance(TaskConsumer.class, task.getType())
                .consume(doc.getDocumentReference(), doc.getVersion());
            task.getFuture().complete(task);
            this.tasksStore.get().deleteTask(task.getWikiId(), task.getDocId(), task.getVersion(), task.getType());
        } catch (ComponentLookupException e) {
            throw new IndexException(String.format("Failed to find a task consumer for task [%s]", task), e);
        } catch (XWikiException e) {
            throw new IndexException(String.format("Error during the execution of task [%s]", task), e);
        } finally {
            context.setWikiId(oldWikId);
        }
    }
}
