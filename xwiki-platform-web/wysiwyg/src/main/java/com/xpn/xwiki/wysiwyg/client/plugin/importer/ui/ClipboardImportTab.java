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
package com.xpn.xwiki.wysiwyg.client.plugin.importer.ui;

import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.util.FocusCommand;
import com.xpn.xwiki.wysiwyg.client.util.Selectable;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.SelectionPreserver;

/**
 * Office Importer UI tab for importing clipboard content.
 * 
 * @version $Id$
 */
public class ClipboardImportTab extends Composite implements Selectable, LoadHandler
{
    /**
     * The text area where the user can paste his content.
     */
    private RichTextArea textArea;

    /**
     * Preserves the selection inside the {@link #textArea} while this tab is hidden.
     */
    private SelectionPreserver selectionPreserver;

    /**
     * Flag indicating if this tab is selected.
     */
    private boolean selected;

    /**
     * Flag indicating if the text area is loaded.
     */
    private boolean loaded;

    /**
     * Default constructor.
     */
    public ClipboardImportTab()
    {
        // Main container panel.
        FlowPanel mainPanel = new FlowPanel();

        // Info label.
        Panel infoLabel = new FlowPanel();
        infoLabel.setStyleName("xInfoLabel");
        infoLabel.add(new InlineLabel(Strings.INSTANCE.importerClipboardTabInfoLabel()));
        InlineLabel mandatoryLabel = new InlineLabel(Strings.INSTANCE.mandatory());
        mandatoryLabel.addStyleName("xMandatory");
        infoLabel.add(mandatoryLabel);
        Label helpLabel = new Label(Strings.INSTANCE.importerClipboardTabHelpLabel());
        helpLabel.setStyleName("xHelpLabel");
        mainPanel.add(infoLabel);
        mainPanel.add(helpLabel);

        // Text area panel.
        textArea = new RichTextArea();
        textArea.addLoadHandler(this);
        textArea.addStyleName("xImporterClipboardTabEditor");
        selectionPreserver = new SelectionPreserver(textArea);
        mainPanel.add(textArea);

        // Finalize.
        initWidget(mainPanel);
    }

    /**
     * Clears the content of the text area.
     */
    public void clearTextArea()
    {
        textArea.setHTML("");
    }

    /**
     * @return the pasted HTML
     */
    public String getHtmlPaste()
    {
        return textArea.getHTML();
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadHandler#onLoad(LoadEvent)
     */
    public void onLoad(LoadEvent event)
    {
        if (event.getSource() == textArea) {
            loaded = true;
            maybeFocusTextArea();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Selectable#setSelected(boolean)
     */
    public void setSelected(boolean selected)
    {
        if (this.selected != selected) {
            this.selected = selected;
            if (selected) {
                maybeFocusTextArea();
            } else {
                selectionPreserver.saveSelection();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Selectable#isSelected()
     */
    public boolean isSelected()
    {
        return selected;
    }

    /**
     * Focuses the text area if it is loaded and the tab is selected.
     */
    private void maybeFocusTextArea()
    {
        if (loaded && selected) {
            DeferredCommand.addCommand(new FocusCommand(textArea)
            {
                public void execute()
                {
                    super.execute();
                    selectionPreserver.restoreSelection();
                }
            });
        }
    }
}
