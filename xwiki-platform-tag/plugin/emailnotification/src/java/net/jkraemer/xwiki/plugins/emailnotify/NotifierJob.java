/*
 * Copyright 2005 Jens Krämer
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
  * 
 * Created on 11.04.2005
 * Version: $Id$
 */
package net.jkraemer.xwiki.plugins.emailnotify;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author <a href="mailto:jk@jkraemer.net">Jens Kraemer </a>
 */
public class NotifierJob implements Job
{
    static final String NOTIFIER_KEY = "notifier";
    
    /**
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    public void execute (JobExecutionContext arg0) throws JobExecutionException
    {
        NotificationSender notifier = (NotificationSender) arg0.getJobDetail().getJobDataMap().get(NOTIFIER_KEY);
        try
        {
            notifier.run ();
        } catch (Exception e)
        {
            throw new JobExecutionException (e);
        }
    }

}
