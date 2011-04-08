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
package org.xwiki.gwt.wysiwyg.client.plugin.macro.exec;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.Console;
import org.xwiki.gwt.user.client.ui.LoadingPanel;
import org.xwiki.gwt.user.client.ui.rta.Reloader;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.AbstractSelectionExecutable;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Refreshes all the macros present on the edited document.
 * 
 * @version $Id$
 */
public class RefreshExecutable extends AbstractSelectionExecutable
{
    /**
     * The command used to notify all the plug-ins that the content of the rich text area is about to be submitted.
     */
    private static final Command SUBMIT = new Command("submit");

    /**
     * The command used to notify all the rich text area listeners when its content has been reset.
     */
    private static final Command RESET = new Command("reset");

    /**
     * Used to prevent typing in the rich text area while waiting for the updated content from the server.
     */
    private final LoadingPanel waiting = new LoadingPanel();

    /**
     * The object used to reload the rich text area.
     */
    private final Reloader reloader;

    /**
     * Creates a new executable that can be used to refresh the specified rich text area. We use a {@link Reloader} to
     * submit the content of the rich text area to the given URL and then use the response to reset the content of the
     * rich text area.
     * 
     * @param rta the execution target
     * @param url the URL to take the content from
     */
    public RefreshExecutable(RichTextArea rta, String url)
    {
        super(rta);
        reloader = new Reloader(rta, url);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelectionExecutable#execute(String)
     */
    public boolean execute(String param)
    {
        // Check if there is a refresh in progress.
        if (waiting.isLoading()) {
            return false;
        }

        // Prevent typing while waiting for the updated content.
        waiting.startLoading(rta);
        waiting.setFocus(true);

        // Request the updated content.
        CommandManager cmdManager = rta.getCommandManager();
        refresh(cmdManager.execute(SUBMIT) ? cmdManager.getStringValue(SUBMIT) : rta.getHTML());

        return true;
    }

    /**
     * Sends a request to the server to parse and re-render the content of the given rich text area.
     * 
     * @param html the HTML content of the rich text area
     */
    private void refresh(String html)
    {
        Map<String, String> params = new HashMap<String, String>();
        params.put("html", html);

        reloader.reload(params, new AsyncCallback<Object>()
        {
            public void onFailure(Throwable caught)
            {
                Console.getInstance().error(caught.getLocalizedMessage());
                // Try to focus the rich text area.
                rta.setFocus(true);
                waiting.stopLoading();
            }

            public void onSuccess(Object result)
            {
                // Reset the content of the rich text area.
                rta.getCommandManager().execute(RESET);
                // Store the initial value of the rich text area in case it is submitted without gaining focus.
                rta.getCommandManager().execute(SUBMIT, true);
                // Try to focus the rich text area.
                rta.setFocus(true);
                waiting.stopLoading();
            }
        });
    }
}
