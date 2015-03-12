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

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import org.codehaus.groovy.control.CompilationFailedException;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * The task that will get executed by the Scheduler when the Job is triggered. This task in turn calls a Groovy script
 * to perform the execution.
 * <p/>
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

            // The XWiki context was saved in the Job execution data map. Get it as we'll retrieve
            // the script to execute from it.
            XWikiContext context = (XWikiContext) data.get("context");

            // Get the job XObject to be executed
            BaseObject object = (BaseObject) data.get("xjob");

            // Force context document
            XWikiDocument jobDocument = context.getWiki().getDocument(object.getName(), context);
            context.setDoc(jobDocument);
            context.put("sdoc", jobDocument);

            if (context.getWiki().getRightService().hasProgrammingRights(context)) {

                // Make the Job execution data available to the Groovy script
                Binding binding = new Binding(data.getWrappedMap());

                // Execute the Groovy script
                GroovyShell shell = new GroovyShell(Thread.currentThread().getContextClassLoader(), binding);
                shell.evaluate(object.getLargeStringValue("script"));
            } else {
                throw new JobExecutionException("The user [" + context.getUser() + "] didn't have "
                    + "programming rights when the job [" + jobContext.getJobDetail().getName() + "] was scheduled.");
            }
        } catch (CompilationFailedException e) {
            throw new JobExecutionException("Failed to execute script for job [" + jobContext.getJobDetail().getName()
                + "]", e, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
