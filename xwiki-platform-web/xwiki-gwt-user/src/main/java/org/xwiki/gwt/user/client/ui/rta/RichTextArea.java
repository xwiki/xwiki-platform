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
package org.xwiki.gwt.user.client.ui.rta;

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.dom.client.HasPasteHandlers;
import org.xwiki.gwt.dom.client.JavaScriptObject;
import org.xwiki.gwt.dom.client.PasteEvent;
import org.xwiki.gwt.dom.client.PasteHandler;
import org.xwiki.gwt.user.client.ActionEvent;
import org.xwiki.gwt.user.client.ActionHandler;
import org.xwiki.gwt.user.client.HasActionHandlers;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.DefaultCommandManager;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.DefaultExecutable;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.DeleteExecutable;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.InsertHTMLExecutable;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.UpdateExecutable;
import org.xwiki.gwt.user.client.ui.rta.internal.BehaviorAdjuster;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.dom.client.HasLoadHandlers;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.impl.RichTextAreaImpl;

/**
 * Extends the rich text area provided by GWT to add support for advanced editing.
 * 
 * @version $Id$
 */
public class RichTextArea extends com.google.gwt.user.client.ui.RichTextArea implements HasDoubleClickHandlers,
    HasLoadHandlers, LoadHandler, HasPasteHandlers, HasActionHandlers
{
    /**
     * @see #setHTML(String)
     */
    public static final String DIRTY = "__dirty";

    /**
     * @see #onLoad(LoadEvent)
     */
    public static final String LOADED = "__loaded";

    /**
     * @see #setEnabled(boolean)
     */
    public static final String DISABLED = "__disabled";

    /**
     * The command manager that executes commands on this rich text area.
     */
    private final CommandManager cm;

    /**
     * Overwrites the default behavior of the rich text area when DOM events are triggered by user actions and that
     * default behavior is either incomplete, unnatural, browser specific or buggy. This custom behavior can still be
     * prevented from a listener by calling {@link Event#preventDefault()} on the {@link #getCurrentEvent()}.
     */
    private final BehaviorAdjuster adjuster = GWT.create(BehaviorAdjuster.class);

    /**
     * The JavaScript function used to retrieve the edited document.
     */
    @SuppressWarnings("unused")
    private final JavaScriptObject documentGetter;

    /**
     * Creates a new rich text area.
     */
    public RichTextArea()
    {
        this(new DefaultCommandManager());

        // Register default executables.
        Command[] defaultCommands = new Command[] {Command.BACK_COLOR, Command.BOLD, Command.CREATE_LINK,
            Command.FONT_NAME, Command.FONT_SIZE, Command.FORE_COLOR, Command.FORMAT_BLOCK, Command.INDENT,
            Command.INSERT_HORIZONTAL_RULE, Command.INSERT_IMAGE, Command.INSERT_ORDERED_LIST, Command.INSERT_PARAGRAPH,
            Command.INSERT_UNORDERED_LIST, Command.ITALIC, Command.JUSTIFY_CENTER, Command.JUSTIFY_FULL,
            Command.JUSTIFY_LEFT, Command.JUSTIFY_RIGHT, Command.OUTDENT, Command.REDO, Command.REMOVE_FORMAT,
            Command.STRIKE_THROUGH, Command.SUB_SCRIPT, Command.SUPER_SCRIPT, Command.TELETYPE, Command.UNDERLINE,
            Command.UNDO, Command.UNLINK};
        for (int i = 0; i < defaultCommands.length; i++) {
            cm.registerCommand(defaultCommands[i], new DefaultExecutable(this, defaultCommands[i].toString()));
        }

        // Register custom executables.
        cm.registerCommand(Command.DELETE, new DeleteExecutable(this));
        cm.registerCommand(Command.INSERT_HTML, new InsertHTMLExecutable(this));
        cm.registerCommand(new Command("update"), new UpdateExecutable());
    }

    /**
     * Custom constructor allowing us to inject a mock command manager. It was mainly added to be used in unit tests.
     * 
     * @param cm custom command manager
     */
    public RichTextArea(CommandManager cm)
    {
        addLoadHandler(this);
        this.cm = cm;
        this.documentGetter = createDocumentGetter();
        adjuster.setTextArea(this);
    }

    /**
     * NOTE: If the current browser doesn't support rich text editing this method returns <code>null</code>. You should
     * test the returned value and fail save to an appropriate behavior!<br/>
     * The appropriate test would be: <code><pre>
     * if (rta.isAttached() && rta.getDocument() == null) {
     *   // The current browser doesn't support rich text editing.
     * }
     * </pre></code>
     * 
     * @return The DOM document being edited with this rich text area.
     */
    public native Document getDocument()
    /*-{
        return (this.@org.xwiki.gwt.user.client.ui.rta.RichTextArea::documentGetter)();
    }-*/;

    /**
     * NOTE: This method was added to optimize the access to the edited document. Ideally the document getter should be
     * placed in the implementation class which is browser specific but we can't add methods to the implementation base
     * class and placing it in a derived class would force us to make a test which we want to avoid.
     * 
     * @return a JavaScript function that can be used to retrieve the edited document
     */
    private native JavaScriptObject createDocumentGetter()
    /*-{
        var element = this.@com.google.gwt.user.client.ui.UIObject::getElement()();
        var contentDocumentGetter = function() {
            // We access the content document in a static way because only static references to overlay types are
            // allowed from JSNI.
            return @org.xwiki.gwt.dom.client.IFrameElement::getContentDocument(Lorg/xwiki/gwt/dom/client/IFrameElement;)(element);
        }
        var nullDocumentGetter = function() {
            return null;
        }
        return 'iframe' == element.nodeName.toLowerCase() ? contentDocumentGetter : nullDocumentGetter;
    }-*/;

    /**
     * @return the {@link CommandManager} associated with this instance.
     */
    public CommandManager getCommandManager()
    {
        return cm;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.RichTextArea#setHTML(String)
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3147
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3156
     */
    public void setHTML(String html)
    {
        // We use a dirty flag to overcome the Issue 3156. Precisely, we test this flag in the setHTMLImpl to avoid
        // overwriting the contents when setHTML haven't been called.
        getElement().setPropertyBoolean(DIRTY, true);
        super.setHTML(html);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.RichTextArea#onBrowserEvent(com.google.gwt.user.client.Event)
     */
    public void onBrowserEvent(com.google.gwt.user.client.Event event)
    {
        // We need to preview the event due to a GWT bug.
        // @see http://code.google.com/p/google-web-toolkit/issues/detail?id=729
        if (!previewEvent(event)) {
            return;
        }
        adjuster.onBeforeBrowserEvent((Event) event);
        super.onBrowserEvent(event);
        adjuster.onBrowserEvent((Event) event);
    }

    /**
     * We need to call DOM.previewEvent because there is a bug in GWT that prevents PopupPanel from previewing events
     * generated in in-line frames like the one in behind of this rich text area.
     * 
     * @param event a handle to the event being previewed.
     * @return <code>false</code> to cancel the event.
     * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=729
     */
    private native boolean previewEvent(com.google.gwt.user.client.Event event)
    /*-{
        return @com.google.gwt.user.client.DOM::previewEvent(Lcom/google/gwt/user/client/Event;)(event);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see HasDoubleClickHandlers#addDoubleClickHandler(DoubleClickHandler)
     */
    public HandlerRegistration addDoubleClickHandler(DoubleClickHandler handler)
    {
        return addDomHandler(handler, DoubleClickEvent.getType());
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasPasteHandlers#addPasteHandler(PasteHandler)
     */
    public HandlerRegistration addPasteHandler(PasteHandler handler)
    {
        return addDomHandler(handler, PasteEvent.getType());
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasLoadHandlers#addLoadHandler(LoadHandler)
     */
    public HandlerRegistration addLoadHandler(LoadHandler handler)
    {
        return addDomHandler(handler, LoadEvent.getType());
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadHandler#onLoad(LoadEvent)
     * @see #onAttach()
     */
    public void onLoad(LoadEvent event)
    {
        // Initialize the element only after the document to be edited is loaded.
        getElement().setPropertyBoolean(LOADED, true);
        // The loaded flag is needed to distinguish between the case when initElement is called after the element is
        // attached to the page and the case when initElement is called after the document to be edited is loaded.
        getImpl().initElement();
    }

    /**
     * NOTE: We need this method because {@link com.google.gwt.user.client.ui.RichTextArea#impl} is private.
     * 
     * @return the underlying rich text area browser-specific implementation
     */
    protected native RichTextAreaImpl getImpl()
    /*-{
        return this.@com.google.gwt.user.client.ui.RichTextArea::impl;
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.RichTextArea#isEnabled()
     * @see #setEnabled(boolean)
     */
    public boolean isEnabled()
    {
        return !getElement().getPropertyBoolean(DISABLED);
    }

    /**
     * {@inheritDoc}
     * <p>
     * NOTE: We overwrite this method to prevent the use of the {@code disabled} property which blocks rich text area
     * events in Internet Explorer. For instance, the load event is not fired if the {@code disabled} property is set to
     * {@code true} on the in-line frame element used by the rich text area. In order to be consistent with all the
     * browsers we chose to use a different property to mark the enabled/disabled state. This way we can disable the
     * rich text area while it is loading to prevent its content from being submitted and enable it when the load event
     * fires.
     * 
     * @see com.google.gwt.user.client.ui.RichTextArea#setEnabled(boolean)
     * @see #isEnabled()
     */
    public void setEnabled(boolean enabled)
    {
        getElement().setPropertyBoolean(DISABLED, !enabled);
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasActionHandlers#addActionHandler(String, ActionHandler)
     */
    public HandlerRegistration addActionHandler(String actionName, ActionHandler handler)
    {
        return addHandler(handler, ActionEvent.getType(actionName));
    }
}
