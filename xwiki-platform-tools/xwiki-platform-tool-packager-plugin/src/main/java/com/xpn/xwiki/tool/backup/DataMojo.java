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
package com.xpn.xwiki.tool.backup;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.internal.validator.AbstractExtensionValidator;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.script.ScriptExtensionRewriter;
import org.xwiki.index.TaskManager;
import org.xwiki.index.internal.DefaultTasksManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.tool.extension.util.ExtensionArtifact;
import org.xwiki.tool.utils.AbstractOldCoreMojo;

import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Maven 2 plugin to generate XWiki data folder (database and extensions).
 *
 * @version $Id$
 * @since 9.0RC1
 * @since 8.4.2
 */
@Mojo(name = "data", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresProject = true)
public class DataMojo extends AbstractOldCoreMojo
{
    /**
     * The list of artifacts (and their dependencies) to install.
     */
    @Parameter
    private List<ExtensionArtifact> includes;

    @Override
    public void executeInternal() throws MojoExecutionException
    {
        InstallRequest installRequest = new InstallRequest();

        // Allow modifying root namespace
        installRequest.setRootModificationsAllowed(true);

        // Make sure jars are installed on root
        // TODO: use a less script oriented class
        ScriptExtensionRewriter rewriter = new ScriptExtensionRewriter();
        rewriter.installExtensionTypeOnRootNamespace("jar");
        rewriter.installExtensionTypeOnRootNamespace("webjar");
        installRequest.setRewriter(rewriter);

        // Use superadmin as pages author
        installRequest.setProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE,
            new DocumentReference("xwiki", "XWiki", XWikiRightService.SUPERADMIN_USER));

        this.extensionHelper.install(this.includes, installRequest, "wiki:xwiki", null);
    }

    @Override
    protected void after() throws MojoExecutionException
    {
        consumeIndexingQueue();
        super.after();
    }

    /**
     * Waits for the document indexing tasks to be completed before continuing.
     *
     * @throws MojoExecutionException in case of error when resolving the components or when stopping the thread
     */
    private void consumeIndexingQueue() throws MojoExecutionException
    {
        ComponentManager componentManager =
            (ComponentManager) this.oldCoreHelper.getXWikiContext().get(ComponentManager.class.getName());
        try {
            // Manually starts the consumer thread as it is not automatically started in the packaging setup.
            // This is also convenient as we can be sure that the tasks are all queued before being consumed.
            TaskManager taskManager = componentManager.getInstance(TaskManager.class);
            if (taskManager instanceof DefaultTasksManager) {
                ((DefaultTasksManager) taskManager).startThread();
            }

            getLog().info(
                String.format("Waiting for the document indexing queue of size %d to be empty...",
                    taskManager.getQueueSize()));
            while (taskManager.getQueueSize() > 0) {
                Thread.sleep(100);
            }
            getLog().info("Document indexing queue empty.");
        } catch (ComponentLookupException e) {
            throw new MojoExecutionException("Failed to get task manager.", e);
        } catch (InterruptedException e) {
            throw new MojoExecutionException("Error while waiting for the task manager to finish.", e);
        }
    }
}
