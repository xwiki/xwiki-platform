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
package com.xpn.xwiki.wysiwyg.client.plugin.sync;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.diff.Diff;
import com.xpn.xwiki.wysiwyg.client.diff.DifferentiationFailedException;
import com.xpn.xwiki.wysiwyg.client.diff.PatchFailedException;
import com.xpn.xwiki.wysiwyg.client.diff.Revision;
import com.xpn.xwiki.wysiwyg.client.diff.ToString;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.sync.SyncResult;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.util.Timer;
import com.xpn.xwiki.wysiwyg.client.util.TimerListener;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

public class SyncPlugin extends AbstractPlugin implements ClickListener, TimerListener, AsyncCallback<SyncResult>
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

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        pageName = config.getParameter("syncPage");
        if (pageName == null) {
            return;
        }

        sync = new PushButton(Images.INSTANCE.sync().createImage(), this);
        sync.setTitle(Strings.INSTANCE.sync());

        toolBarExtension.addFeature("sync", sync);
        getUIExtensionList().add(toolBarExtension);

        initialContent = getTextArea().getHTML();
        if (initialContent == null) {
            initialContent = "";
        }

        timer = new Timer();
        timer.addTimerListener(this);
        timer.scheduleRepeating(wysiwyg.getParamAsInt("sync_delay", DEFAULT_SYNC_DELAY));
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        sync.removeFromParent();
        sync.removeClickListener(this);
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
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == sync) {
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
            return;
        }
        syncInProgress = true;

        // Compute our revision
        syncedRevision = null;
        syncedContent = getTextArea().getHTML();
        if (!initialContent.equals(syncedContent)) {
            try {
                syncedRevision =
                    Diff.diff(ToString.stringToArray(initialContent), ToString.stringToArray(syncedContent));
            } catch (DifferentiationFailedException e) {
                getWysiwyg().showError(e);
            }
        }

        // Commit our revision and, at the same time, checkout the latest revision
        WysiwygService.Singleton.getInstance().syncEditorContent(syncedRevision, pageName, version, this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AsyncCallback#onFailure(Throwable)
     */
    public synchronized void onFailure(Throwable caught)
    {
        getWysiwyg().showError(caught);
        syncInProgress = false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AsyncCallback#onSuccess(Object)
     */
    public synchronized void onSuccess(SyncResult result)
    {
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
                String newHTMLContent =
                    ToString.arrayToString(newRevision.patch(ToString.stringToArray(initialContent)));
                initialContent = newHTMLContent;
                getTextArea().setHTML(newHTMLContent);
            } else {
                // We have the latest version
                initialContent = syncedContent;
            }
            version = syncResult.getVersion();
        } catch (PatchFailedException e) {
            getWysiwyg().showError(e);
        } finally {
            syncInProgress = false;
        }
    }
}
