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
package com.xpn.xwiki.plugin.scheduler;

import java.util.Objects;

import org.codehaus.groovy.control.CompilationFailedException;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

/**
 * The task that will get executed by the Scheduler when the Job is triggered. This task in turn calls a Groovy script
 * to perform the execution.
 * <p>
 * <b>Important:</b>: Note that the script will execute in the XWiki Context that was set at the time the Job was
 * scheduled for execution. For example calling <code>context.getDoc()</code> will return the current document that was
 * set at that time and not the current document that is set when the Groovy script executes...
 * 
 * @version $Id$
 */
public class GroovyJob extends AbstractJob
{
    /**
     * Executes the Groovy script passed in the <code>script</code> property of the
     * {@link com.xpn.xwiki.plugin.scheduler.SchedulerPlugin#XWIKI_JOB_CLASS} object extracted from the XWiki context
     * passed in the Quartz's Job execution context. The XWiki Task object is looked for in the current document that
     * was set in the context at the time the Job was scheduled.
     * 
     * @param jobContext the Quartz execution context containing the XWiki context from which the script to execute is
     *            retrieved
     * @throws JobExecutionException if the script fails to execute or if the user didn't have programming rights when
     *             the Job was scheduled
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    @Override
    protected void executeJob(JobExecutionContext jobContext) throws JobExecutionException
    {
        try {
            JobDataMap data = jobContext.getJobDetail().getJobDataMap();

            // Get the job XObject to be executed
            BaseObject object = (BaseObject) data.get("xjob");

            // Force context document
            XWikiDocument jobDocument = object.getOwnerDocument();
            if (jobDocument == null) {
                jobDocument = getXWikiContext().getWiki().getDocument(object.getDocumentReference(), getXWikiContext());
            }

            getXWikiContext().setDoc(jobDocument);
            XWikiDocument secureDocument;
            // Make sure that the secure document has the correct content author.
            if (!Objects.equals(jobDocument.getAuthors().getContentAuthor(),
                jobDocument.getAuthors().getEffectiveMetadataAuthor()))
            {
                secureDocument = jobDocument.clone();
                secureDocument.getAuthors().setContentAuthor(jobDocument.getAuthors().getEffectiveMetadataAuthor());
            } else {
                secureDocument = jobDocument;
            }
            getXWikiContext().put("sdoc", secureDocument);

            ContextualAuthorizationManager contextualAuthorizationManager =
                Utils.getComponent(ContextualAuthorizationManager.class);
            contextualAuthorizationManager.checkAccess(Right.PROGRAM);

            // Make the Job execution data available to the Groovy script
            Binding binding = new Binding(data.getWrappedMap());

            // Set the right instance of XWikiContext
            binding.setProperty("context", getXWikiContext());
            binding.setProperty("xcontext", getXWikiContext());
            data.put("xwiki", new com.xpn.xwiki.api.XWiki(getXWikiContext().getWiki(), getXWikiContext()));

            // Execute the Groovy script
            GroovyShell shell = new GroovyShell(Thread.currentThread().getContextClassLoader(), binding);
            shell.evaluate(object.getLargeStringValue("script"));
        } catch (CompilationFailedException e) {
            throw new JobExecutionException(
                "Failed to execute script for job [" + jobContext.getJobDetail().getKey() + "]", e);
        } catch (XWikiException e) {
            throw new JobExecutionException("Failed to load the document containing the job ["
                + jobContext.getJobDetail().getKey() + "]", e);
        } catch (AccessDeniedException e) {
            throw new JobExecutionException("Executing the job [" + jobContext.getJobDetail().getKey() + "] failed "
                + "due to insufficient rights.", e);
        }
    }
}
