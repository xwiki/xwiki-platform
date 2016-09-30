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

import org.xwiki.gwt.dom.client.CopyEvent;
import org.xwiki.gwt.dom.client.CopyHandler;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Event;
import org.xwiki.gwt.dom.client.HasCopyHandlers;
import org.xwiki.gwt.dom.client.HasPasteHandlers;
import org.xwiki.gwt.dom.client.JavaScriptObject;
import org.xwiki.gwt.dom.client.PasteEvent;
import org.xwiki.gwt.dom.client.PasteHandler;
import org.xwiki.gwt.user.client.ActionEvent;
import org.xwiki.gwt.user.client.ActionHandler;
import org.xwiki.gwt.user.client.HasActionHandlers;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.DefaultCommandManager;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.DefaultCommandProvider;
import org.xwiki.gwt.user.client.ui.rta.internal.BehaviorAdjuster;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasDoubleClickHandlers;
import com.google.gwt.event.dom.client.HasLoadHandlers;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.gwt.user.client.ui.impl.RichTextAreaImpl;

/**
 * Extends the rich text area provided by GWT to add support for advanced editing.
 * 
 * @version $Id$
 */
public class RichTextArea extends com.google.gwt.user.client.ui.RichTextArea implements HasDoubleClickHandlers,
    HasLoadHandlers, LoadHandler, HasPasteHandlers, HasCopyHandlers, HasActionHandlers, ClosingHandler
{
    /**
     * @see #setHTML(String)
     */
    public static final String DIRTY = "__dirty";

    /**
     * Flag indicating that the load event was fired. Ensures the rich text area is initialized only once.
     * 
     * @see #onLoad(LoadEvent)
     */
    public static final String LOADED = "__loaded";

    /**
     * Flag indicating that the load event is currently being handled. Ensures the rich text area is initialized only
     * when the load event is fired.
     * 
     * @see #onLoad(LoadEvent)
     */
    public static final String INITIALIZING = "__initializing";

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
    private final JavaScriptObject documentGetter;

    /**
     * Creates a new rich text area.
     */
    public RichTextArea()
    {
        this(new DefaultCommandManager());
        new DefaultCommandProvider().provideTo(this);
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
     * test the returned value and fail save to an appropriate behavior!
     * <p>
     * The appropriate test would be: {@code
     * if (rta.isAttached() && rta.getDocument() == null) {
     *   // The current browser doesn't support rich text editing.
     * }
     * }
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
        var outer = this;
        var contentDocumentGetter = function() {
            // The in-line frame element can be replaced during the life time of a rich text area so we must get it
            // whenever the edited document is requested.
            var element = outer.@com.google.gwt.user.client.ui.UIObject::getElement()();
            // We access the content document in a static way because only static references to overlay types are
            // allowed from JSNI.
            return @org.xwiki.gwt.dom.client.IFrameElement::getContentDocument(Lorg/xwiki/gwt/dom/client/IFrameElement;)(element);
        }
        var nullDocumentGetter = function() {
            return null;
        }
        var tagName = this.@com.google.gwt.user.client.ui.UIObject::getElement()().nodeName.toLowerCase();
        return tagName == 'iframe' ? contentDocumentGetter : nullDocumentGetter;
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
     * <ul>
     * <li>http://code.google.com/p/google-web-toolkit/issues/detail?id=3147</li>
     * <li>http://code.google.com/p/google-web-toolkit/issues/detail?id=3156</li>
     * </ul>
     * 
     * @see com.google.gwt.user.client.ui.RichTextArea#setHTML(String)
     */
    @Override
    public void setHTML(String html)
    {
        // We use a dirty flag to overcome the Issue 3156. Precisely, we test this flag in the setHTMLImpl to avoid
        // overwriting the contents when setHTML haven't been called.
        getElement().setPropertyBoolean(DIRTY, true);
        super.setHTML(html);
    }

    @Override
    public void onBrowserEvent(com.google.gwt.user.client.Event event)
    {
        // We need to preview the event due to a GWT bug.
        // See http://code.google.com/p/google-web-toolkit/issues/detail?id=729.
        // Note that this makes the RichTextArea unusable on a modal dialog box because the test that checks if the
        // event target is a child of the panel fails for all the events triggered inside the in-line frame due to the
        // fact that they come from a different document than the one holding the panel. As a result all the
        // RichTextArea events are canceled when the RichTextArea is on a modal dialog box (the one provided by GWT).
        // Unfortunately changing the event target to point to the in-line frame is not possible.
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
     * <p>
     * See http://code.google.com/p/google-web-toolkit/issues/detail?id=729.
     * 
     * @param event a handle to the event being previewed.
     * @return <code>false</code> to cancel the event.
     */
    private native boolean previewEvent(com.google.gwt.user.client.Event event)
    /*-{
        return @com.google.gwt.user.client.DOM::previewEvent(Lcom/google/gwt/user/client/Event;)(event);
    }-*/;

    @Override
    public HandlerRegistration addDoubleClickHandler(DoubleClickHandler handler)
    {
        return addDomHandler(handler, DoubleClickEvent.getType());
    }

    @Override
    public HandlerRegistration addPasteHandler(PasteHandler handler)
    {
        return addDomHandler(handler, PasteEvent.getType());
    }

    @Override
    public HandlerRegistration addCopyHandler(CopyHandler handler)
    {
        // FIXME: We should use addDomHandler(handler, CopyEvent.getType()) but GWT currently doesn't support the copy
        // event type and throws "Trying to sink unknown event type copy".
        // See http://code.google.com/p/google-web-toolkit/issues/detail?id=4030 .
        return addHandler(handler, CopyEvent.getType());
    }

    @Override
    public HandlerRegistration addLoadHandler(LoadHandler handler)
    {
        return addDomHandler(handler, LoadEvent.getType());
    }

    @Override
    public void onLoad(LoadEvent event)
    {
        // The load event could be fired multiple times.
        if (!getElement().getPropertyBoolean(LOADED)) {
            // Make sure the rich text area is initialized only once.
            getElement().setPropertyBoolean(LOADED, true);
            // Make sure the rich text area is initialized only when the load event is fired.
            getElement().setPropertyBoolean(INITIALIZING, true);
            try {
                // The initializing flag is needed to distinguish between the case when initElement is called after the
                // element is attached to the page and the case when initElement is called after the document to be
                // edited is loaded.
                getImpl().initElement();
            } finally {
                getElement().setPropertyBoolean(INITIALIZING, false);
            }
        }
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

    @Override
    public HandlerRegistration addActionHandler(String actionName, ActionHandler handler)
    {
        return addHandler(handler, ActionEvent.getType(actionName));
    }

    @Override
    public void sinkEvents(int eventBitsToAdd)
    {
        // Events listeners are not registered right away but after the widget is attached to the browser's document for
        // the first time. This deferred sink behavior is not suited for the load event because the load event could be
        // fired before the load listener is registered. This can happen if the underlying element is loaded
        // synchronously (e.g. in-line frame with the source attribute unspecified).
        if (!isOrWasAttached() && (eventBitsToAdd & Event.ONLOAD) != 0) {
            // Sink the load event immediately.
            DOM.sinkEvents(getElement(), eventBitsToAdd | DOM.getEventsSunk(getElement()));
            DOM.setEventListener(getElement(), this);
            // We can't remove the listener on detach so we listen to window closing event to remove the listener.
            Window.addWindowClosingHandler(this);
        } else {
            // Preserve deferred sink behavior.
            super.sinkEvents(eventBitsToAdd);
        }
    }

    @Override
    protected void onDetach()
    {
        super.onDetach();

        // We need to keep the listener because onAttach is called after the rich text area is physically attached to
        // the document and in some browsers the in-line frame used by the rich text area is loaded synchronously. This
        // means that the load even can be fired before onAttach thus before the listener is set.
        // NOTE: We have to remove the listener when the host page unloads in order to break the circular reference.
        DOM.setEventListener(getElement(), this);
    }

    @Override
    public void onWindowClosing(ClosingEvent event)
    {
        // We can't remove the listener on detach so we remove it here.
        DOM.setEventListener(getElement(), null);
    }
}
