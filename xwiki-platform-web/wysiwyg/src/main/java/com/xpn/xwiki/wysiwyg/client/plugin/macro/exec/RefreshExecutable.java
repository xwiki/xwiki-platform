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
package com.xpn.xwiki.wysiwyg.client.plugin.macro.exec;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.util.Console;
import com.xpn.xwiki.wysiwyg.client.widget.LoadingPanel;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandListener;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandManager;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.AbstractExecutable;

/**
 * Refreshes all the macros present on the edited document.
 * 
 * @version $Id$
 */
public class RefreshExecutable extends AbstractExecutable
{
    /**
     * The command used to notify all the plug-ins that the content of the rich text area is about to be submitted.
     */
    private static final Command SUBMIT = new Command("submit");

    /**
     * The syntax used for storing the edited document.
     */
    private final String syntax;

    /**
     * Used to prevent typing in the rich text area while waiting for the updated content from the server.
     */
    private final LoadingPanel waiting = new LoadingPanel();

    /**
     * Creates a new refresh executable.
     * 
     * @param syntax the syntax used for storing the edited document
     */
    public RefreshExecutable(String syntax)
    {
        this.syntax = syntax;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#execute(RichTextArea, String)
     */
    public boolean execute(final RichTextArea rta, String param)
    {
        // Check if there is a refresh in progress.
        if (waiting.isLoading()) {
            return false;
        }

        // Prevent typing while waiting for the updated content.
        waiting.startLoading(rta);
        waiting.setFocus(true);

        // Allow other plug-ins to adjust the content before the refresh by executing a submit command.
        CommandListener submitListener = new CommandListener()
        {
            public boolean onBeforeCommand(CommandManager sender, Command command, String param)
            {
                // The refresh is executed just before the submit command is executed.
                if (SUBMIT.equals(command)) {
                    refresh(rta);
                }
                return false;
            }

            public void onCommand(CommandManager sender, Command command, String param)
            {
                // ignore
            }
        };
        // We add the listener now to be sure it is the last one called, after all the other plug-ins did their job.
        rta.getCommandManager().addCommandListener(submitListener);
        if (!rta.getCommandManager().execute(SUBMIT)) {
            // Send the refresh request even if the submit command failed.
            refresh(rta);
        }
        // We no longer need to lister to the submit command.
        rta.getCommandManager().removeCommandListener(submitListener);

        return true;
    }

    /**
     * Sends a request to the server to parse and re-render the current content of the given rich text area.
     * 
     * @param rta the rich text area whose content will be refreshed
     */
    private void refresh(final RichTextArea rta)
    {
        WysiwygService.Singleton.getInstance().parseAndRender(rta.getHTML(), syntax, new AsyncCallback<String>()
        {
            public void onFailure(Throwable caught)
            {
                rta.setFocus(true);
                waiting.stopLoading();
                Console.getInstance().error(caught.getMessage());
            }

            public void onSuccess(String result)
            {
                // Reset the content of the rich text area.
                rta.getCommandManager().execute(new Command("reset"), result);
                // Store the initial value of the rich text area in case it is submitted without gaining focus.
                rta.getCommandManager().execute(SUBMIT, true);
                // Try to focus the rich text area.
                rta.setFocus(true);
                waiting.stopLoading();
            }
        });
    }
}
