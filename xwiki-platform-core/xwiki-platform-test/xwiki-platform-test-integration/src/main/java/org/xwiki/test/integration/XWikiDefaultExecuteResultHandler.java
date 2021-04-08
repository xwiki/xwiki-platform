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
package org.xwiki.test.integration;

import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extend the default implementation class to log an error when the process fails before finishing properly.
 *
 * @version $Id$
 */
public class XWikiDefaultExecuteResultHandler extends DefaultExecuteResultHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiDefaultExecuteResultHandler.class);

    private String command;

    /**
     * @param command the command being executed (used when logging errors)
     */
    public XWikiDefaultExecuteResultHandler(String command)
    {
        this.command = command;
    }

    @Override
    public void onProcessFailed(ExecuteException e)
    {
        super.onProcessFailed(e);
        LOGGER.error("Command [{}] failed to execute successfully or has timed-out", this.command, e);
    }
}
