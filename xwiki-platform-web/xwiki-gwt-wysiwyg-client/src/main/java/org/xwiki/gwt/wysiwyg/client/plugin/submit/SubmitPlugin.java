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
package org.xwiki.gwt.wysiwyg.client.plugin.submit;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.JavaScriptObject;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.HiddenConfig;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.StatelessUIExtension;
import org.xwiki.gwt.wysiwyg.client.plugin.submit.exec.SubmitExecutable;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;

/**
 * Binds a {@link RichTextArea} to a form field.
 * 
 * @version $Id$
 */
public class SubmitPlugin extends AbstractPlugin implements BlurHandler, CommandListener, ClosingHandler
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
     * This flag tells the server that it needs to convert the editor output from HTML to the storage syntax before
     * processing it.
     */
    private static final String REQUIRES_HTML_CONVERSION = "RequiresHTMLConversion";

    /**
     * The JavaScript object that catches the submit event and calls {@link #onSubmit()}. We couldn't use a FormPanel
     * because it overwrites the onsubmit property of the form element instead of registering itself as a listener.
     */
    @SuppressWarnings("unused")
    private JavaScriptObject submitHandler;

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
     * @see AbstractPlugin#init(RichTextArea, Config)
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        String hookId = getConfig().getParameter("hookId");
        getTextArea().getCommandManager().registerCommand(SUBMIT, new SubmitExecutable(textArea, hookId));

        if (getTextArea().getCommandManager().isSupported(SUBMIT)) {
            Element hook = (Element) Document.get().getElementById(hookId);
            // See if the hook is inside an HTML form.
            form = (Element) DOMUtils.getInstance().getFirstAncestor(hook, "form");
            // We don't use hook.hasAttribute because the name attribute appears as unspecified in IE if it has been set
            // from JavaScript.
            if (form != null && !StringUtils.isEmpty(hook.getAttribute(NAME_ATTRIBUTE))) {
                // Put additional hidden data on the HTML form.
                hiddenConfig = new HiddenConfig();
                // All the parameters of this hidden configuration will be prefixed with the name of the hook.
                hiddenConfig.setNameSpace(hook.getAttribute(NAME_ATTRIBUTE));
                // This flag tells the server that the editor output requires HTML conversion.
                if (textArea.isEnabled()) {
                    hiddenConfig.addFlag(REQUIRES_HTML_CONVERSION);
                }
                // The storage syntax for this rich text area.
                hiddenConfig.setParameter(SYNTAX, config.getParameter(SYNTAX, DEFAULT_SYNTAX));

                rootExtension.addFeature(SUBMIT.toString(), hiddenConfig);
                getUIExtensionList().add(rootExtension);

                // Listen to submit event.
                hookSubmitEvent(form);
            }

            // Save the initial content of the rich text area, after all the plug-ins have been initialized.
            // Note that we can't use the "loaded" action event because event handlers are registered after the current
            // event is processed. In this case the "loaded" action event is fired after all the plug-ins have been
            // initialized but still while the rich text area "load" event is handled (and the reason for this is to
            // ensure the order of the action events).
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
                public void execute()
                {
                    onSubmit();
                }
            });

            // Submit the content when the rich text area looses the focus or the user navigates away.
            saveRegistration(getTextArea().addBlurHandler(this));
            saveRegistration(Window.addWindowClosingHandler(this));

            // Prevent the rich text area from being submitted when it is disabled.
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
            hiddenConfig.removeFromParent();
            hiddenConfig = null;
            submitHandler = null;
            rootExtension.clearFeatures();
        }

        getTextArea().getCommandManager().removeCommandListener(this);

        super.destroy();
    }

    /**
     * @return the JavaScript object that catches the submit event and calls {@link #onSubmit()}
     */
    protected native JavaScriptObject getSubmitHandler()
    /*-{
        if (!this.@org.xwiki.gwt.wysiwyg.client.plugin.submit.SubmitPlugin::submitHandler) {
            var _this = this;
            this.@org.xwiki.gwt.wysiwyg.client.plugin.submit.SubmitPlugin::submitHandler = function() {
                _this.@org.xwiki.gwt.wysiwyg.client.plugin.submit.SubmitPlugin::onSubmit()();
            };
        }
        return this.@org.xwiki.gwt.wysiwyg.client.plugin.submit.SubmitPlugin::submitHandler;
    }-*/;

    /**
     * Registers {@link #getSubmitHandler()} as a listener for submit events generated by the given HTML form element.
     * 
     * @param form the HTML form element whose submit event should be listened
     */
    protected native void hookSubmitEvent(Element form)
    /*-{
        var handler = this.@org.xwiki.gwt.wysiwyg.client.plugin.submit.SubmitPlugin::getSubmitHandler()();
        form.addEventListener('submit', handler, false);
    }-*/;

    /**
     * Unregisters {@link #getSubmitHandler()} as a listener for submit events generated by the given HTML form element.
     * 
     * @param form the HTML form element whose submit event shouldn't be listened anymore
     */
    protected native void unhookSubmitEvent(Element form)
    /*-{
        var handler = this.@org.xwiki.gwt.wysiwyg.client.plugin.submit.SubmitPlugin::getSubmitHandler()();
        form.removeEventListener('submit', handler, false);
    }-*/;

    /**
     * {@inheritDoc}
     * 
     * @see BlurHandler#onBlur(BlurEvent)
     */
    public void onBlur(BlurEvent event)
    {
        if (event.getSource() == getTextArea()) {
            onSubmit();
        }
    }

    /**
     * Called when the HTML form hosting the rich text area is submitted.
     */
    protected void onSubmit()
    {
        // Submit the content of the rich text area only if it is enabled.
        if (getTextArea().isAttached() && getTextArea().isEnabled()) {
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
        if (hiddenConfig != null && sender == getTextArea().getCommandManager() && Command.ENABLE.equals(command)) {
            if (getTextArea().getCommandManager().isExecuted(Command.ENABLE)) {
                hiddenConfig.addFlag(REQUIRES_HTML_CONVERSION);
            } else {
                hiddenConfig.removeFlag(REQUIRES_HTML_CONVERSION);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClosingHandler#onWindowClosing(ClosingEvent)
     */
    public void onWindowClosing(ClosingEvent event)
    {
        // Allow the browser to cache the content of the rich text area when the user navigates away from the edit page.
        onSubmit();
    }
}
