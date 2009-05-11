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
package com.xpn.xwiki.wysiwyg.client.plugin.submit;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.JavaScriptObject;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.StatelessUIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.submit.exec.EnableExecutable;
import com.xpn.xwiki.wysiwyg.client.plugin.submit.exec.ResetExecutable;
import com.xpn.xwiki.wysiwyg.client.plugin.submit.exec.SubmitExecutable;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.HiddenConfig;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandListener;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandManager;

/**
 * Binds a {@link RichTextArea} to a form field.
 * 
 * @version $Id$
 */
public class SubmitPlugin extends AbstractPlugin implements FocusListener, CommandListener
{
    /**
     * The name attribute, used by HTML form elements to pass data to the server when the form is submitted.
     */
    private static final String NAME_ATTRIBUTE = "name";

    /**
     * The name of the syntax configuration parameter.
     */
    private static final String SYNTAX = "syntax";

    /**
     * Default syntax. Can be overwritten from the configuration.
     */
    private static final String DEFAULT_SYNTAX = "xhtml/1.0";

    /**
     * The command used to store the value of the rich text area before submitting the including form.
     */
    private static final Command SUBMIT = new Command("submit");

    /**
     * The command used to enable or disable the rich text area. Use this command to prevent the content of the rich
     * text area to be submitted.
     */
    private static final Command ENABLE = new Command("enable");

    /**
     * This flag is needed in order to detect that a server request contains rich text area data.
     */
    private static final String WYSIWYG_FLAG = "wysiwyg";

    /**
     * The JavaScript object that catches the submit event and calls {@link #onSubmit()}. We couldn't use a FormPanel
     * because it overwrites the onsubmit property of the form element instead of registering itself as a listener.
     */
    protected JavaScriptObject submitHandler;

    /**
     * Extends the root of the editor UI. Examples of similar root extensions are the tool bar and the menu bar.
     */
    private final StatelessUIExtension rootExtension = new StatelessUIExtension("root");

    /**
     * Additional data to be sent to the server, besides the content of the rich text area.
     */
    private HiddenConfig hiddenConfig;

    /**
     * The HTML form that contains the rich text area.
     */
    private Element form;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        String hookId = getConfig().getParameter("hookId");
        getTextArea().getCommandManager().registerCommand(SUBMIT, new SubmitExecutable(hookId));
        getTextArea().getCommandManager().registerCommand(ENABLE, new EnableExecutable());
        getTextArea().getCommandManager().registerCommand(new Command("reset"), new ResetExecutable());

        if (getTextArea().getCommandManager().isSupported(SUBMIT)) {
            Element hook = (Element) Document.get().getElementById(hookId);
            // See if the hook is inside an HTML form.
            form = (Element) DOMUtils.getInstance().getFirstAncestor(hook, "form");
            if (form != null && hook.hasAttribute(NAME_ATTRIBUTE)) {
                // Put additional hidden data on the HTML form.
                hiddenConfig = new HiddenConfig();
                // All the parameters of this hidden configuration will be prefixed with the name of the hook.
                hiddenConfig.setNameSpace(hook.getAttribute(NAME_ATTRIBUTE));
                // This flag is needed in order to detect that a server request contains rich text area data.
                hiddenConfig.addFlag(WYSIWYG_FLAG);
                // The storage syntax for this rich text area.
                hiddenConfig.setParameter(SYNTAX, config.getParameter(SYNTAX, DEFAULT_SYNTAX));

                rootExtension.addFeature(SUBMIT.toString(), hiddenConfig);
                getUIExtensionList().add(rootExtension);

                // Listen to submit event.
                hookSubmitEvent(form);
            }

            getTextArea().addFocusListener(this);
            getTextArea().getCommandManager().addCommandListener(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        if (rootExtension.getFeatures().length > 0) {
            unhookSubmitEvent(form);
            form = null;
            hiddenConfig = null;
            submitHandler = null;
            rootExtension.clearFeatures();
        }

        getTextArea().removeFocusListener(this);
        getTextArea().getCommandManager().removeCommandListener(this);

        super.destroy();
    }

    /**
     * @return the JavaScript object that catches the submit event and calls {@link #onSubmit()}
     */
    protected native JavaScriptObject getSubmitHandler()
    /*-{
        if (!this.@com.xpn.xwiki.wysiwyg.client.plugin.submit.SubmitPlugin::submitHandler) {
            var _this = this;
            this.@com.xpn.xwiki.wysiwyg.client.plugin.submit.SubmitPlugin::submitHandler = function() {
                _this.@com.xpn.xwiki.wysiwyg.client.plugin.submit.SubmitPlugin::onSubmit()();
            };
        }
        return this.@com.xpn.xwiki.wysiwyg.client.plugin.submit.SubmitPlugin::submitHandler;
    }-*/;

    /**
     * Registers {@link #getSubmitHandler()} as a listener for submit events generated by the given HTML form element.
     * 
     * @param form the HTML form element whose submit event should be listened
     */
    protected native void hookSubmitEvent(Element form)
    /*-{
        var handler = this.@com.xpn.xwiki.wysiwyg.client.plugin.submit.SubmitPlugin::getSubmitHandler()();
        form.addEventListener('submit', handler, false);
    }-*/;

    /**
     * Unregisters {@link #getSubmitHandler()} as a listener for submit events generated by the given HTML form element.
     * 
     * @param form the HTML form element whose submit event shouldn't be listened anymore
     */
    protected native void unhookSubmitEvent(Element form)
    /*-{
        var handler = this.@com.xpn.xwiki.wysiwyg.client.plugin.submit.SubmitPlugin::getSubmitHandler()();
        form.removeEventListener('submit', handler, false);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see FocusListener#onFocus(Widget)
     */
    public void onFocus(Widget sender)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see FocusListener#onLostFocus(Widget)
     */
    public void onLostFocus(Widget sender)
    {
        if (sender == getTextArea()) {
            onSubmit();
        }
    }

    /**
     * Called when the HTML form hosting the rich text area is submitted.
     */
    protected void onSubmit()
    {
        // Submit the content of the rich text area only if it is enabled.
        if (getTextArea().getCommandManager().isExecuted(ENABLE)) {
            getTextArea().getCommandManager().execute(SUBMIT);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onBeforeCommand(CommandManager, Command, String)
     */
    public boolean onBeforeCommand(CommandManager sender, Command command, String param)
    {
        // ignore
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onCommand(CommandManager, Command, String)
     */
    public void onCommand(CommandManager sender, Command command, String param)
    {
        if (hiddenConfig != null && sender == getTextArea().getCommandManager() && ENABLE.equals(command)) {
            if (getTextArea().getCommandManager().isExecuted(ENABLE)) {
                hiddenConfig.addFlag(WYSIWYG_FLAG);
            } else {
                hiddenConfig.removeFlag(WYSIWYG_FLAG);
            }
        }
    }
}
