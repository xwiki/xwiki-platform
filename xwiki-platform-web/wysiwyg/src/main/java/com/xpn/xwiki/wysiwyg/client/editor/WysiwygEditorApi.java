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
package com.xpn.xwiki.wysiwyg.client.editor;

import org.xwiki.gwt.dom.client.JavaScriptObject;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.util.NativeAsyncCallback;
import com.xpn.xwiki.wysiwyg.client.util.internal.DefaultConfig;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * This class exposes a {@link WysiwygEditor} to the native JavaScript code.
 * 
 * @version $Id$
 */
public class WysiwygEditorApi
{
    /**
     * The configuration parameter which identifies the hook element.
     */
    public static final String HOOK_ID = "hookId";

    /**
     * The attribute of the hook element where the editor saves its value.
     */
    public static final String VALUE = "value";

    /**
     * The underlying {@link WysiwygEditor} which is exposed in native JavaScript code.
     */
    private WysiwygEditor editor;

    /**
     * Creates a new {@link WysiwygEditor} based on the given configuration object.
     * 
     * @param jsConfig the {@link JavaScriptObject} used to configure the newly created editor
     */
    public WysiwygEditorApi(JavaScriptObject jsConfig)
    {
        if (!isRichTextEditingSupported()) {
            return;
        }

        Config config = new DefaultConfig(jsConfig);

        // Get the element that will be replaced by the WYSIWYG editor.
        Element hook = DOM.getElementById(config.getParameter(HOOK_ID));
        if (hook == null) {
            return;
        }

        // Prepare the DOM by creating a container for the editor.
        Element container = DOM.createDiv();
        String containerId = hook.getId() + "_container" + Math.round(Math.random() * 1000);
        container.setId(containerId);
        hook.getParentElement().insertBefore(container, hook);

        editor = WysiwygEditorFactory.getInstance().newEditor(config, Wysiwyg.getInstance());

        // Attach the editor to the browser's document.
        if (Boolean.TRUE.toString().equals(editor.getConfig().getParameter("debug", "false"))) {
            RootPanel.get(containerId).add(new WysiwygEditorDebugger(editor));
        } else {
            RootPanel.get(containerId).add(editor.getUI());
        }
    }

    /**
     * @return {@code true} if the current browser supports rich text editing, {@code false} otherwise
     */
    public static boolean isRichTextEditingSupported()
    {
        RichTextArea textArea = new RichTextArea(null);
        return textArea.getBasicFormatter() != null;
    }

    /**
     * Releases the editor so that it can be garbage collected before the page is unloaded. Call this method before the
     * editor is physically detached from the DOM document.
     */
    public void release()
    {
        if (editor != null) {
            // Logical detach.
            Widget container = editor.getUI();
            while (container.getParent() != null) {
                container = container.getParent();
            }
            RootPanel.detachNow(container);
            editor = null;
        }
    }

    /**
     * @return the plain HTML text area element used by the editor
     */
    public Element getPlainTextArea()
    {
        return editor == null ? null : editor.getPlainTextEditor().getTextArea().getElement();
    }

    /**
     * @return the rich text area element used by the editor
     */
    public Element getRichTextArea()
    {
        return editor == null ? null : editor.getRichTextEditor().getTextArea().getElement();
    }

    /**
     * Sends a request to the server to convert the HTML output of the rich text editor to source text and calls one of
     * the given functions when the response is received.
     * 
     * @param onSuccess the JavaScript function to call on success
     * @param onFailure the JavaScript function to call on failure
     */
    public void getSourceText(JavaScriptObject onSuccess, JavaScriptObject onFailure)
    {
        Element hook = DOM.getElementById(editor.getConfig().getParameter(HOOK_ID));
        NativeAsyncCallback<String> callback = new NativeAsyncCallback<String>(onSuccess, onFailure);
        if (editor.getRichTextEditor().getTextArea().isEnabled()) {
            // We have to convert the HTML of the rich text area to source text.
            // Notify the plug-ins that the content of the rich text area is about to be submitted.
            editor.getRichTextEditor().getTextArea().getCommandManager().execute(new Command("submit"));
            // At this point we should have the HTML, adjusted by plug-ins, in the value property of the hook element.
            // Make the request to convert the HTML to source text.
            WysiwygService.Singleton.getInstance().fromHTML(hook.getPropertyString(VALUE),
                editor.getConfig().getParameter("syntax", WysiwygEditor.DEFAULT_SYNTAX), callback);
        } else {
            // We take the source text from the value property of the hook element.
            callback.onSuccess(hook.getPropertyString(VALUE));
        }
    }

    /**
     * Publishes the JavaScript API that can be used to create and control {@link WysiwygEditor}s.
     */
    public static native void publish()
    /*-{
        // We can't use directly the WysiwygEditorApi constructor because currently there's no way to access (as in save
        // a reference to) the GWT instance methods without having an instance.
        $wnd.WysiwygEditor = function(config) {
            if (typeof config == 'object') {
                this.instance = @com.xpn.xwiki.wysiwyg.client.editor.WysiwygEditorApi::new(Lorg/xwiki/gwt/dom/client/JavaScriptObject;)(config);
            }
        }
        $wnd.WysiwygEditor.prototype.release = function() {
            this.instance.@com.xpn.xwiki.wysiwyg.client.editor.WysiwygEditorApi::release()();
        }
        $wnd.WysiwygEditor.prototype.getPlainTextArea = function() {
            return this.instance.@com.xpn.xwiki.wysiwyg.client.editor.WysiwygEditorApi::getPlainTextArea()();
        }
        $wnd.WysiwygEditor.prototype.getRichTextArea = function() {
            return this.instance.@com.xpn.xwiki.wysiwyg.client.editor.WysiwygEditorApi::getRichTextArea()();
        }
        $wnd.WysiwygEditor.prototype.getSourceText = function(onSuccess, onFailure) {
            this.instance.@com.xpn.xwiki.wysiwyg.client.editor.WysiwygEditorApi::getSourceText(Lorg/xwiki/gwt/dom/client/JavaScriptObject;Lorg/xwiki/gwt/dom/client/JavaScriptObject;)(onSuccess, onFailure);
        }
    }-*/;
}
