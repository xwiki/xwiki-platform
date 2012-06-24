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

import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.managers.ThreadedListenerManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.ircbot.IRCBot;

import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * Make PircBotX a Component.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class PircBotXIRCBot extends ExtendedPircBotX implements IRCBot, Initializable
{
    /**
     * Used to construct a clean Execution Context for the PircBotX input threads.
     */
    @Inject
    private Execution execution;

    /**
     * Used to construct a clean Execution Context for the PircBotX input threads.
     */
    @Inject
    private ExecutionContextManager executionContextManager;

    /**
     * Used to construct a clean Execution Context for the PircBotX input threads.
     */
    @Inject
    private XWikiStubContextProvider stubContextProvider;

    @Override
    public void initialize() throws InitializationException
    {
        // We use a Fixed Thread Pool instead of the default Cached Thread Pool that PircBotX uses by default because
        // we need to initialize each thread with an Execution Context (it's a ThreadLocal) and since we haven't found
        // a way to clean up the Execution Context ThreadLocal when the thread dies, using a lot of short-lived threads
        // (as it's done by the Cached Thread Pool) will slowly use up all memory...
        setListenerManager(new ThreadedListenerManager<PircBotX>(
            Executors.newFixedThreadPool(3, new XWikiContextualizedThreadFactory(this.execution,
                this.executionContextManager, this.stubContextProvider))));
    }
}
