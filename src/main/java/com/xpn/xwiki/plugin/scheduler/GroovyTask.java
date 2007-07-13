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

public class GroovyTask implements Job
{
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        try {
            JobDataMap data = context.getJobDetail().getJobDataMap();
            Context xcontext = (Context) data.get("context");
            int task = data.getInt("task");

            Binding binding = new Binding(data.getWrappedMap());
            GroovyShell shell = new GroovyShell(binding);
            BaseObject object = xcontext.getDoc().getObject(SchedulerPlugin.TASK_CLASS, task);
            Api api = new Api(xcontext.getContext());
            if (api.hasProgrammingRights()) {
                shell.evaluate(object.getLargeStringValue("script"));
            }
        } catch (CompilationFailedException e) {
            throw new JobExecutionException(e);
        }
    }
}
