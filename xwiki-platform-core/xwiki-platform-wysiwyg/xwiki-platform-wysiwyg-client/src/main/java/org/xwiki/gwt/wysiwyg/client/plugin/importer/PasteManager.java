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
package org.xwiki.gwt.wysiwyg.client.plugin.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.gwt.dom.client.CopyEvent;
import org.xwiki.gwt.dom.client.CopyHandler;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.PasteEvent;
import org.xwiki.gwt.dom.client.PasteHandler;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.user.client.ui.LoadingPanel;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.SelectionPreserver;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * A {@link PasteHandler} that can filter the value that has been pasted in a {@link RichTextArea}.
 * 
 * @version $Id$
 * @since 5.0M2
 */
public class PasteManager implements PasteHandler, CopyHandler
{
    /**
     * The object used to filter the pasted content before cleaning it on the server.
     */
    private final PasteFilter pasteFilter = GWT.create(PasteFilter.class);

    /**
     * Used to prevent typing in the rich text area while waiting for the paste content to be cleaned on the server.
     */
    private final LoadingPanel waiting = new LoadingPanel();

    /**
     * The rich text area whose paste events we catch.
     */
    private RichTextArea textArea;

    /**
     * The component used to clean the paste content on the server side.
     */
    private ImportServiceAsync importService;

    /**
     * The object used to restore the selection or the position of the caret after a paste event.
     */
    private SelectionPreserver selectionPreserver;

    /**
     * The content that has been copied from the rich text area when the last copy event occurred. We need this
     * information to determine if the paste content comes from the same rich text area (in which case it doesn't
     * require cleaning) or from an external (probably untrusted) source.
     */
    private String copyContent;

    /**
     * Configures this instance to catch the paste events from the given rich text area and to clean the paste content
     * using the specified service.
     * 
     * @param textArea the rich text area whose paste event are caught
     * @param importService the component used to clean the paste content on the server side
     * @return the list of event handler registrations
     */
    public List<HandlerRegistration> initialize(RichTextArea textArea, ImportServiceAsync importService)
    {
        this.textArea = textArea;
        this.importService = importService;
        selectionPreserver = new SelectionPreserver(textArea);
        return addHandlers();
    }

    /**
     * Adds all required event handlers and returns their registrations so that they can be unregistered at the end.
     * 
     * @return the list of event handler registrations
     */
    protected List<HandlerRegistration> addHandlers()
    {
        List<HandlerRegistration> registrations = new ArrayList<HandlerRegistration>();
        registrations.add(textArea.addCopyHandler(this));
        registrations.add(textArea.addPasteHandler(this));
        return registrations;
    }

    @Override
    public void onPaste(PasteEvent event)
    {
        // If the selection has been saved but not yet restored then it means that multiple paste events were triggered
        // one after another (e.g. by keeping the paste shortcut key pressed). We can reuse the existing paste container
        // in this case. This way we perform the cleaning only once, after the sequence of paste events.
        if (selectionPreserver.hasSelection()) {
            return;
        }
        selectionPreserver.saveSelection();
        final Element pasteContainer = createPasteContainer();
        Scheduler.get().scheduleDeferred(new ScheduledCommand()
        {
            @Override
            public void execute()
            {
                onAfterPaste(pasteContainer);
            }
        });
    }

    /**
     * Creates the element where the paste content is inserted. We place the caret inside this element before each paste
     * event in order to isolate the paste content from the rest of the edited content. This allows us to clean the
     * paste content before inserting it in the right position.
     * 
     * @return the paste container
     */
    private Element createPasteContainer()
    {
        Document document = textArea.getDocument();
        // We use a DIV element because it supports both in-line and block content.
        Element pasteContainer = Element.as(document.createDivElement());
        // Position the container in the center of the current view-port so that the position of the scroll bars doesn't
        // change. Note that fixed position doesn't work because the code that scrolls the selection into view doesn't
        // compute correctly the position of the paste container. Also make sure the paste container is not visible.
        pasteContainer.getStyle().setPosition(Position.ABSOLUTE);
        centerPasteContainer(pasteContainer);
        pasteContainer.getStyle().setWidth(1, Unit.PX);
        pasteContainer.getStyle().setHeight(1, Unit.PX);
        pasteContainer.getStyle().setOverflow(Overflow.HIDDEN);
        // Insert a text node (a non-breaking space) in the paste container to make sure the selection stays inside.
        pasteContainer.appendChild(document.createTextNode("\u00A0"));
        // Insert the paste container and select its contents.
        document.getBody().appendChild(pasteContainer);
        selectPasteContainer(pasteContainer);
        return pasteContainer;
    }

    /**
     * Select the contents of the given paste container.
     * 
     * @param pasteContainer the paste container
     */
    protected void selectPasteContainer(Element pasteContainer)
    {
        textArea.getDocument().getSelection().selectAllChildren(pasteContainer.getFirstChild());
    }

    /**
     * We added this method just to be able to override it for IE9 so that we can overcome a GWT bug.
     * 
     * @param pasteContainer the paste container
     * @see <a href="http://code.google.com/p/google-web-toolkit/issues/detail?id=6256">getAbsoluteTop/getScrollTop
     *      returns wrong values for IE9 when body has been scrolled</a>
     * @see <a href="https://gwt-review.googlesource.com/#/c/2260/">Document#getScrollTop() and Document#getScrollLeft()
     *      are broken for nested documents in IE9</a>
     */
    protected void centerPasteContainer(Element pasteContainer)
    {
        Document document = textArea.getDocument();
        pasteContainer.getStyle().setLeft(document.getScrollLeft() + document.getClientWidth() / 2, Unit.PX);
        pasteContainer.getStyle().setTop(document.getScrollTop() + document.getClientHeight() / 2, Unit.PX);
    }

    /**
     * Called after a paste event, i.e. after the paste content has been inserted into the rich text area.
     * 
     * @param pasteContainer the element were the paste content was inserted
     */
    private void onAfterPaste(Element pasteContainer)
    {
        pasteFilter.filter(pasteContainer);
        String pasteContent = pasteContainer.xGetInnerHTML();
        pasteContainer.removeFromParent();

        selectionPreserver.restoreSelection();

        if (requiresCleaning(pasteContent)) {
            cleanPasteContent(pasteContent);
        } else if (pasteContent.length() > 0) {
            paste(pasteContent);
        }
    }

    @Override
    public void onCopy(CopyEvent event)
    {
        Selection selection = textArea.getDocument().getSelection();
        copyContent =
            selection.isCollapsed() || selection.getRangeCount() != 1 ? null : selection.getRangeAt(0).toHTML();
    }

    /**
     * Don't clean the paste content if:
     * <ul>
     * <li>it's empty or</li>
     * <li>it was copied from the same rich text area or</li>
     * <li>it's just plain text.</li>
     * </ul>
     * 
     * @param pasteContent the content that has been pasted
     * @return {@code true} if the paste content needs to be cleaned, {@code false} otherwise
     */
    private boolean requiresCleaning(String pasteContent)
    {
        return pasteContent.length() > 0 && !pasteContent.equals(copyContent)
            && (pasteContent.indexOf('<') >= 0 || pasteContent.indexOf('>') >= 0);
    }

    /**
     * Cleans the given paste content on the server side.
     * 
     * @param pasteContent the paste content to be cleaned
     */
    private void cleanPasteContent(final String pasteContent)
    {
        // Prevent typing while waiting for the clean content.
        waiting.startLoading(textArea);
        waiting.setFocus(true);

        Map<String, String> cleaningParameters = new HashMap<String, String>();
        cleaningParameters.put("filterStyles", "strict");
        cleaningParameters.put("namespacesAware", Boolean.toString(false));
        importService.cleanOfficeHTML(pasteContent, "wysiwyg", cleaningParameters, new AsyncCallback<String>()
        {
            @Override
            public void onFailure(Throwable caught)
            {
                // TODO: Warn the user that the automatic cleaning failed.
                onSuccess(pasteContent);
            }

            @Override
            public void onSuccess(String result)
            {
                waiting.stopLoading();
                textArea.setFocus(true);
                paste(result);
            }
        });
    }

    /**
     * Inserts the given content in the rich text area, in place of the current selection or at the current caret
     * position.
     * 
     * @param content the content to paste
     */
    private void paste(String content)
    {
        textArea.getCommandManager().execute(Command.INSERT_HTML, content);
        textArea.getDocument().getSelection().collapseToEnd();
    }

    /**
     * @return the rich text area whose paste events we catch
     */
    protected RichTextArea getTextArea()
    {
        return textArea;
    }
}
