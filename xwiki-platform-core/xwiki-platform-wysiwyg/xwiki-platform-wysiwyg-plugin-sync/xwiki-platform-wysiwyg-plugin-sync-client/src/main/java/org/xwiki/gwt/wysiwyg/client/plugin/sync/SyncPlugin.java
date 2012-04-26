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
package org.xwiki.gwt.wysiwyg.client.plugin.sync;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.RangeFactory;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.Console;
import org.xwiki.gwt.user.client.Timer;
import org.xwiki.gwt.user.client.TimerListener;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.wysiwyg.client.diff.Diff;
import org.xwiki.gwt.wysiwyg.client.diff.DifferentiationFailedException;
import org.xwiki.gwt.wysiwyg.client.diff.Revision;
import org.xwiki.gwt.wysiwyg.client.diff.ToString;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;

public class SyncPlugin extends AbstractPlugin implements ClickHandler, TimerListener, AsyncCallback<SyncResult>
{
    public static final int DEFAULT_SYNC_DELAY = 3000;

    private PushButton sync;

    private Timer timer;

    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    private String pageName;

    private int version = 0;

    private String initialContent;

    private String syncedContent;

    private Revision syncedRevision;

    private boolean syncInProgress = false;

    private int id;

    private boolean sendCursor = false;

    private boolean maintainCursor = true;

    /**
     * The service used to synchronize the content of multiple editors.
     */
    private final SyncServiceAsync syncService;

    /**
     * Creates a new synchronization plug-in that uses the given service to synchronize multiple editors.
     * 
     * @param syncService the synchronization service
     */
    public SyncPlugin(SyncServiceAsync syncService)
    {
        this.syncService = syncService;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(RichTextArea, Config)
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        // init the plugin id
        id = Math.abs(Random.nextInt());

        pageName = config.getParameter("syncPage");
        if (pageName == null) {
            return;
        }

        sync = new PushButton(new Image(Images.INSTANCE.sync()));
        saveRegistration(sync.addClickHandler(this));
        sync.setTitle(Strings.INSTANCE.sync());

        toolBarExtension.addFeature("sync", sync);
        getUIExtensionList().add(toolBarExtension);

        initialContent = (version == 0) ? "" : getTextArea().getHTML();
        if (initialContent == null) {
            initialContent = "";
        }

        timer = new Timer();
        timer.addTimerListener(this);
        timer.scheduleRepeating(Integer.parseInt(getConfig().getParameter("sync_delay",
            String.valueOf(DEFAULT_SYNC_DELAY))));
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        sync.removeFromParent();
        sync = null;

        toolBarExtension.clearFeatures();

        timer.removeTimerListener(this);
        timer.cancel();
        timer = null;

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickHandler#onClick(ClickEvent)
     */
    public void onClick(ClickEvent event)
    {
        if (event.getSource() == sync) {
            onSync();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see TimerListener#onElapsed(Timer)
     */
    public void onElapsed(Timer sender)
    {
        if (sender == timer) {
            onSync();
        }
    }

    public synchronized void onSync()
    {
        if (syncInProgress) {
            debugMessage("Cannot sync because sync is in progress");
            return;
        }
        syncInProgress = true;

        try {
            // Compute our revision
            syncedRevision = null;
            if (version != 0) {

                if (sendCursor) {
                    insertCursor(getTextArea().getDocument());
                }
                syncedContent = (version == 0) ? "" : getTextArea().getHTML();
                if (sendCursor) {
                    removeCursor(getTextArea().getDocument());
                }
                if ((version > 0) && !initialContent.equals(syncedContent)) {
                    try {
                        syncedRevision =
                            Diff.diff(ToString.stringToArray(initialContent), ToString.stringToArray(syncedContent));
                    } catch (DifferentiationFailedException e) {
                        showErrorAndResetSync(e);
                    }
                }
            } else {
                syncedContent = "";
            }

            // Commit our revision and, at the same time, checkout the latest revision
            // If we send -1 then we ask the server to reset it's content to the page content
            boolean syncReset = ((version == 0) && (getConfig().getParameter("syncReset", "0").equals("1")));
            syncService.syncEditorContent(syncedRevision, pageName, version, syncReset, this);
        } catch (Throwable th) {
            debugMessage("error in onSync ");
            showErrorAndResetSync(th);
        }
    }

    private void insertCursor(Document doc)
    {
        try {
            int color = id - 10 * (int) Math.floor(id / 10);
            // insertCursor(element, id, color);
            SpanElement cursorNode = doc.createSpanElement();
            cursorNode.setId("cursor-" + id);
            cursorNode.setClassName("cursor cursor-" + color);
            cursorNode.setAttribute("style", "background-color: #" + color + ";");
            Range range = doc.getSelection().getRangeAt(0);
            try {
                if (range != null) {
                    if (range.getStartContainer().equals(doc) && range.getEndContainer().equals(doc)
                        && range.getStartOffset() == 0 && range.getEndOffset() == 0) {
                        debugMessage("Cursor at start.. let's not handle it");
                    } else {
                        debugMessage("Start container: " + range.getStartContainer());
                        debugMessage("Start offset: " + range.getStartOffset());
                        debugMessage("End container: " + range.getEndContainer());
                        debugMessage("End offset: " + range.getEndOffset());
                        range.surroundContents(cursorNode);
                        debugMessage("surrounding range ok");
                    }
                }
            } catch (Exception e) {
                try {
                    debugMessage("error surrounding range");
                    debugMessage("Exception: " + e.getMessage());
                    e.printStackTrace();
                    debugMessage(e.toString());
                    if (range != null) {
                        debugMessage("Start container: " + range.getStartContainer());
                        debugMessage("Start offset: " + range.getStartOffset());
                        debugMessage("End container: " + range.getEndContainer());
                        debugMessage("End offset: " + range.getEndOffset());
                        try {
                            debugMessage("Range content: " + range.cloneContents().getInnerHTML());
                        } catch (Exception e3) {
                        }
                    }
                    Selection selection = doc.getSelection();
                    if (selection != null) {
                        debugMessage("Selection range count: " + selection.getRangeCount());
                    }
                } catch (Exception e2) {
                    debugMessage("Exception: " + e2.getMessage());
                }
            }

        } catch (Exception e) {
            debugMessage("Uncaught exception in insertCursor: " + e.getMessage());
        }

    }

    private void removeCursor(Document doc)
    {
        try {
            Node cursorNode = null;
            NodeList list = doc.getElementsByTagName("span");
            for (int i = 0; i < list.getLength(); i++) {
                Element element = (Element) list.getItem(i);
                if (element.getId().equals("cursor-" + id)) {
                    cursorNode = element;
                }
            }
            if (cursorNode != null) {
                debugMessage("found cursor element");
                Node firstNode = null;
                Node lastNode = null;
                Node pNode = cursorNode;
                NodeList childs = cursorNode.getChildNodes();
                // readd all childs of the cursor to the left of the cursor node
                int nb = childs.getLength();
                for (int i = nb - 1; i >= 0; i--) {
                    Node node = childs.getItem(i);
                    if (i == 0) {
                        firstNode = node;
                    }
                    if (i == nb - 1) {
                        lastNode = node;
                    }
                    // we want to insert the node in it's parent before the cursor Node
                    cursorNode.removeChild(node);
                    cursorNode.getParentNode().insertBefore(node, pNode);
                    pNode = node;
                }
                // remove the cursor node itself
                Node previousNode = cursorNode.getPreviousSibling();
                debugMessage("removing cursor node");
                cursorNode.getParentNode().removeChild(cursorNode);

                debugMessage("creating new range");
                Range range = RangeFactory.INSTANCE.createRange(doc);

                if (firstNode != null) {
                    debugMessage("set range with first node and last node");
                    range.setStartBefore(firstNode);
                    range.setEndAfter(lastNode);
                } else if (previousNode != null) {
                    debugMessage("set range with previous node");
                    Node nextNode = previousNode.getNextSibling();
                    if (nextNode != null) {
                        range.setStart(nextNode, 0);
                    } else {
                        range.setStartAfter(previousNode);
                        range.setEndAfter(previousNode);
                        range.collapse(true);
                    }
                }

                doc.getSelection().removeAllRanges();
                doc.getSelection().addRange(range);
                getTextArea().setFocus(true);
            } else {
                debugMessage("could not find cursor element");
            }
        } catch (Exception e) {
            debugMessage("Uncaught exception in insertCursor: " + e.getMessage());
        }

    }

    public void debugMessage(String text)
    {
        if ("true".equals(getConfig().getParameter("debug", "false"))) {
            Console.getInstance().info(text);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AsyncCallback#onFailure(Throwable)
     */
    public synchronized void onFailure(Throwable caught)
    {
        showErrorAndResetSync(caught);
    }

    private void showErrorAndResetSync(Throwable caught)
    {
        showError(caught, new AsyncCallback()
        {
            public void onFailure(Throwable throwable)
            {
                syncInProgress = false;
            }

            public void onSuccess(Object o)
            {
                syncInProgress = false;
            }
        });
    }

    /**
     * {@inheritDoc}
     * 
     * @see AsyncCallback#onSuccess(Object)
     */
    public synchronized void onSuccess(SyncResult result)
    {
        debugMessage("received result from server");

        // If result is null we have nothing to do
        if (result == null) {
            syncInProgress = false;
            return;
        }

        SyncResult syncResult = result;
        Revision newRevision = syncResult.getRevision();

        try {
            if (newRevision != null) {
                // We don't have the latest version
                // We need to take local changes that might have occured
                if (maintainCursor && (version != 0)) {
                    insertCursor(getTextArea().getDocument());
                }
                String localContent = (version == 0) ? "" : getTextArea().getHTML();

                String newHTMLContent = "";
                try {
                    newHTMLContent = ToString.arrayToString(newRevision.patch(ToString.stringToArray(initialContent)));
                } catch (Exception e) {
                    debugMessage("Exception while patching initial content: " + e.getMessage());
                    debugMessage("Initial content was: " + initialContent);
                    showErrorAndResetSync(e);
                }
                // this corresponds to the HTML that is the one known by the server
                String futureInitialContent = newHTMLContent;

                // we need to compute local changes, including the cursor
                if ((version != 0) && !localContent.equals(initialContent)) {
                    try {
                        // we need to rework the path to take into account the local content
                        Revision localRevision =
                            Diff.diff(ToString.stringToArray(initialContent), ToString.stringToArray(localContent));
                        Revision localRevision2 = SyncTools.relocateRevision(localRevision, newRevision);
                        newHTMLContent =
                            ToString.arrayToString(localRevision2.patch(ToString.stringToArray(newHTMLContent)));
                    } catch (Exception e) {
                        debugMessage("Exception while applying local revision: " + e.getMessage());
                    }
                }
                initialContent = futureInitialContent;

                // TODO improve by working on an cloned Document that is updated in one call in the textarea
                setHTML(newHTMLContent);

                // we should have retrieved the cursor so we need to remove it
                if ((maintainCursor || sendCursor) && (version != 0)) {
                    removeCursor(getTextArea().getDocument());
                }
            } else {
                // We have the latest version
                initialContent = syncedContent;
            }
            version = syncResult.getVersion();
            // normal ending let's reset the syncInProgress
            syncInProgress = false;
        } catch (Throwable e) {
            showErrorAndResetSync(e);
        }
    }

    private void setHTML(String newHTMLContent)
    {
        getTextArea().setHTML(newHTMLContent);
    }

    /**
     * @param title
     * @param message
     */
    public void showDialog(String title, String message, AsyncCallback cb)
    {
        try {
            Console.getInstance().error(title + "\n\n" + message);
            cb.onSuccess(null);
        } catch (Throwable e) {
            debugMessage("Error displaying failed " + e.getMessage());
            cb.onFailure(e);
        }
    }

    public void showError(Throwable caught, AsyncCallback< ? > cb)
    {
        if (caught != null) {
            caught.printStackTrace();
            debugMessage("Error is: " + caught.toString());
        }
        showError("", (caught == null) ? "" : caught.toString(), cb);
    }

    public void showError(String text, AsyncCallback< ? > cb)
    {
        showError("", text, cb);
    }

    public void showError(String code, String text, AsyncCallback< ? > cb)
    {
        debugMessage("Error ready to display");
        String message = "An error occured. Its code is " + code + ".\r\n\r\n" + text;
        debugMessage("Error displaying: " + message);
        showDialog("Synchronize", message, cb);
    }
}
