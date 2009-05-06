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

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.StatelessUIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.submit.exec.EnableExecutable;
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
public class SubmitPlugin extends AbstractPlugin implements FocusListener, FormHandler, CommandListener
{
    /**
     * The name of the syntax configuration parameter.
     */
    private static final String SYNTAX = "syntax";

    /**
     * Default syntax. Can be overwritten from the configuration.
     */
    private static final String DEFAULT_SYNTAX = "xwiki/2.0";

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
     * Extends the root of the editor UI. Examples of similar root extensions are the tool bar and the menu bar.
     */
    private final StatelessUIExtension rootExtension = new StatelessUIExtension("root");

    /**
     * Additional data to be sent to the server, besides the content of the rich text area.
     */
    private final HiddenConfig hiddenConfig = new HiddenConfig();

    /**
     * The HTML form that contains the rich text area.
     */
    private FormPanel form;

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

        if (getTextArea().getCommandManager().isSupported(SUBMIT)) {
            Element hook = (Element) Document.get().getElementById(hookId);
            // All the parameters of this hidden configuration will be prefixed with the name of the hook.
            hiddenConfig.setNameSpace(hook.getAttribute("name"));
            // This flag is needed in order to detect that a server request contains rich text area data.
            hiddenConfig.addFlag(WYSIWYG_FLAG);
            // The storage syntax for this rich text area.
            hiddenConfig.setParameter(SYNTAX, config.getParameter(SYNTAX, DEFAULT_SYNTAX));
            rootExtension.addFeature(SUBMIT.toString(), hiddenConfig);

            // See if the hook is inside an HTML form.
            Element formElement = (Element) DOMUtils.getInstance().getFirstAncestor(hook, "form");
            if (formElement != null) {
                // Listen to form events.
                form = FormPanel.wrap(formElement);
                form.addFormHandler(this);
            }

            getTextArea().addFocusListener(this);
            getTextArea().getCommandManager().addCommandListener(this);
            getUIExtensionList().add(rootExtension);
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
            if (form != null) {
                form.removeFormHandler(this);
            }

            getTextArea().removeFocusListener(this);
            getTextArea().getCommandManager().removeCommandListener(this);
            rootExtension.clearFeatures();
        }

        super.destroy();
    }

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
            getTextArea().getCommandManager().execute(SUBMIT);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see FormHandler#onSubmit(FormSubmitEvent)
     */
    public void onSubmit(FormSubmitEvent event)
    {
        if (!event.isCancelled()) {
            getTextArea().getCommandManager().execute(SUBMIT);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see FormHandler#onSubmitComplete(FormSubmitCompleteEvent)
     */
    public void onSubmitComplete(FormSubmitCompleteEvent event)
    {
        // ignore
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
        if (sender == getTextArea().getCommandManager() && ENABLE.equals(command)) {
            if (getTextArea().getCommandManager().isExecuted(ENABLE)) {
                hiddenConfig.addFlag(WYSIWYG_FLAG);
            } else {
                hiddenConfig.removeFlag(WYSIWYG_FLAG);
            }
        }
    }
}
