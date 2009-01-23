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
package com.xpn.xwiki.wysiwyg.client.plugin.importer;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.importer.ui.ImporterDialog;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.PopupListener;
import com.xpn.xwiki.wysiwyg.client.widget.SourcesPopupEvents;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.SelectionPreserver;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Office Importer wysiwyg plugin.
 * 
 * @version $Id$
 */
public class ImporterPlugin extends AbstractPlugin implements ClickListener, PopupListener, AsyncCallback<String>
{
    /**
     * Import button placed on the tool bar.
     */
    private PushButton importPushButton;

    /**
     * Importer dialog used to communicate with the user.
     */
    private ImporterDialog importerDialog;

    /**
     * A {@link SelectionPreserver} for saving / restoring user selections on the main wysiwyg editor.
     */
    private SelectionPreserver selectionPreserver;

    /**
     * The toolbar extension used to add the link buttons to the toolbar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        if (getTextArea().getCommandManager().isSupported(Command.INSERT_HTML)) {
            importPushButton = new PushButton(Images.INSTANCE.importer().createImage(), this);
            importPushButton.setTitle(Strings.INSTANCE.importerToolTip());
            toolBarExtension.addFeature("importer", importPushButton);
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getUIExtensionList().add(toolBarExtension);
            selectionPreserver = new SelectionPreserver(textArea);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void destroy()
    {
        if (importPushButton != null) {
            importPushButton.removeFromParent();
            importPushButton.removeClickListener(this);
            importPushButton = null;
        }
        if (importerDialog != null) {
            importerDialog.hide();
            importerDialog.removeFromParent();
            importerDialog.removePopupListener(this);
            importerDialog = null;
        }
        if (toolBarExtension.getFeatures().length > 0) {
            toolBarExtension.clearFeatures();
        }
        super.destroy();
    }

    /**
     * {@inheritDoc}
     */
    public void onClick(Widget sender)
    {
        if (sender == importPushButton) {
            // save the selection
            selectionPreserver.saveSelection();
            getImporterDialog().center();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onPopupClosed(SourcesPopupEvents sender, boolean autoClosed)
    {
        if (importerDialog.isClipBoardImport()) {
            String inputHtml = importerDialog.getHtmlPaste();            
            Map<String, String> params = new HashMap<String, String>();
            if (importerDialog.isFilterStyles()) {
                params.put("filterStyles", "strict");
            }
            params.put("namespacesAware", "false"); // For Office2007
            WysiwygService.Singleton.getInstance().cleanOfficeHTML(inputHtml, "wysiwyg", params, this);            
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onSuccess(String result)
    {
        selectionPreserver.restoreSelection();
        getTextArea().getCommandManager().execute(Command.INSERT_HTML, result);
    }

    /**
     * {@inheritDoc}
     */
    public void onFailure(Throwable caught)
    {
        Window.alert(caught.getMessage());
        selectionPreserver.restoreSelection();
    }

    /**
     * @return The importer dialog instance.
     */
    private ImporterDialog getImporterDialog()
    {
        if (null == importerDialog) {
            importerDialog = new ImporterDialog();
            importerDialog.addPopupListener(this);
        }
        return importerDialog;
    }
}
