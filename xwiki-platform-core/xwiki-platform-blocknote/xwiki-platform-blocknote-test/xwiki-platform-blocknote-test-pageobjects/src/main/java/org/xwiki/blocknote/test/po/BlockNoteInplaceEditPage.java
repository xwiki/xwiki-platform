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
package org.xwiki.blocknote.test.po;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.realtime.test.po.RealtimeInplaceEditablePage;

/**
 * Represents the in-place edit mode when the page content is edited in realtime with BlockNote.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class BlockNoteInplaceEditPage extends RealtimeInplaceEditablePage
{
    /**
     * Navigate to the specified page, in view mode, from where it can be edited in-place.
     *
     * @param pageReference the page to navigate to
     * @return the in-place editable page
     */
    public static BlockNoteInplaceEditPage gotoPage(EntityReference pageReference)
    {
        getUtil().gotoPage(pageReference);
        return new BlockNoteInplaceEditPage();
    }

    @Override
    public BlockNoteInplaceEditPage editInplace()
    {
        super.editInplace();
        return this;
    }

    @Override
    public BlockNoteInplaceEditPage waitForInplaceEditor()
    {
        super.waitForInplaceEditor();
        return this;
    }

    @Override
    public BlockNoteInplaceEditPage waitForInplaceEditor(boolean waitForConnection)
    {
        super.waitForInplaceEditor(waitForConnection);
        return this;
    }

    /**
     * Makes the collaboration server unreachable from the current page, until {@link #restoreCollaborationServer()} is
     * called or another page is loaded. Call this before editing in-place, so that the editor fails to connect to the
     * realtime editing session.
     *
     * @return this instance
     * @since 18.9.0
     */
    public BlockNoteInplaceEditPage simulateUnreachableCollaborationServer()
    {
        // Redirect the WebSocket connections to the Yjs end-point to a port where nothing listens, which fails like an
        // unreachable server. The WebSocket constructor is looked up each time a connection is attempted, so we can
        // restore the access to the collaboration server later, without reloading the page.
        getDriver().executeScript("""
            if (!window.collaborationServerAccess) {
              const NativeWebSocket = window.WebSocket;
              const access = window.collaborationServerAccess = {blocked: true};
              window.WebSocket = class extends NativeWebSocket {
                constructor(url, protocols) {
                  url = String(url);
                  if (access.blocked && /\\/websocket\\/.+\\/yjs(\\?|$)/.test(url)) {
                    url = url.replace(/^(wss?:\\/\\/)[^\\/]+/, '$1localhost:1');
                  }
                  super(url, protocols);
                }
              };
            }
            window.collaborationServerAccess.blocked = true;
            """);
        return this;
    }

    /**
     * Makes the collaboration server reachable again, after {@link #simulateUnreachableCollaborationServer()}, and
     * waits for the editor to connect to the realtime editing session.
     *
     * @return this instance
     * @since 18.9.0
     */
    public BlockNoteInplaceEditPage restoreCollaborationServer()
    {
        getDriver().executeScript("window.collaborationServerAccess.blocked = false;");
        getToolbar().waitUntilConnected();
        return this;
    }

    /**
     * @return the editor used to edit the page content
     */
    public BlockNoteEditor getContentEditor()
    {
        return new BlockNoteEditor("content");
    }
}
