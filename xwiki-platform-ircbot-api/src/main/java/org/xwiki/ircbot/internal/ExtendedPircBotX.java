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
package org.xwiki.ircbot.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

import org.pircbotx.InputThread;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;

import com.xpn.xwiki.XWikiContext;

public class ExtendedPircBotX extends PircBotX
{
    private boolean shouldStop;

    public boolean shouldStop()
    {
        return this.shouldStop;
    }

    @Override
    public synchronized void disconnect()
    {
        this.shouldStop = true;
        super.disconnect();
    }

    @Override
    public synchronized void connect(String hostname) throws IOException, IrcException
    {
        super.connect(hostname);
        this.shouldStop = false;
    }
}
