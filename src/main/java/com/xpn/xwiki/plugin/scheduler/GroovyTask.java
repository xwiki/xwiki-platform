/*
 * Copyright 2005-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Context;

/**
 * The task that will get executed by the Scheduler when the Job is triggered. This task in
 * turn calls a Groovy script to perform the execution.
 *
 * <p><b>Important:</b>: Note that the script will execute in the XWiki Context that was set at
 * the time the Job was scheduled for execution. For example calling <code>context.getDoc()</code>
 * will return the current document that was set at that time and not the current document that is
 * set when the Groovy script executes...
 *
 * @version $Id: $
 */
public class GroovyTask implements Job
{
    /**
     * Executes the Groovy script passed in the <code>script</code> property of the
     * {@link com.xpn.xwiki.plugin.scheduler.SchedulerPlugin#TASK_CLASS} object extracted from the
     * XWiki context passed in the Quartz's Job execution context. The XWiki Task object is
     * looked for in the current document that was set in the context at the time the Job was
     * scheduled.
     *
     * @param context the Quartz execution context containing the XWiki context from which the
     *        script to execute is retrieved
     * @exception JobExecutionException if the script fails to execute or if the user didn't have
     *            programming rights when the Job was scheduled
     * @see Job#execute(org.quartz.JobExecutionContext)
     */
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        try {
            JobDataMap data = context.getJobDetail().getJobDataMap();

            // The XWiki context was saved in the Job execution data map. Get it as we'll retrieve
            // the script to execute from it.
            Context xcontext = (Context) data.get("context");

            Api api = new Api(xcontext.getContext());

            if (api.hasProgrammingRights()) {

                // The current task id. This is needed to find the correct XWiki Task object that
                // was stored in the current document as there can be several tasks stored in that
                // document.
                int task = data.getInt("task");

                // Get the correct task object from the current doc set when the Job was
                // scheduled.
                BaseObject object = xcontext.getDoc().getObject(SchedulerPlugin.TASK_CLASS, task);

                Binding binding = new Binding(data.getWrappedMap());
                GroovyShell shell = new GroovyShell(binding);

                // Execute the Groovy script
                shell.evaluate(object.getLargeStringValue("script"));
                
            } else {
                throw new JobExecutionException("The user [" + xcontext.getUser() + "] didn't have "
                    + "programming rights when the job [" + context.getJobDetail().getName()
                    + "] was scheduled.");
            }
        } catch (CompilationFailedException e) {
            throw new JobExecutionException("Failed to execute script for job ["
                + context.getJobDetail().getName() + "]", e, true);
        }
    }
}
