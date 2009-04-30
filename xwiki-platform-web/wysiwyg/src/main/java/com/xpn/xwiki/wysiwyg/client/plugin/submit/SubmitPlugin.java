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

import org.xwiki.gwt.dom.client.Element;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.StatelessUIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.submit.exec.SubmitExecutable;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.HiddenConfig;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Binds a {@link RichTextArea} to a form field.
 * 
 * @version $Id$
 */
public class SubmitPlugin extends AbstractPlugin implements FocusListener
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
     * Extends the root of the editor UI. Examples of similar root extensions are the tool bar and the menu bar.
     */
    private final StatelessUIExtension rootExtension = new StatelessUIExtension("root");

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

        if (getTextArea().getCommandManager().isSupported(SUBMIT)) {
            Element hook = (Element) Document.get().getElementById(hookId);
            // Additional data to be sent to the server, besides the content of the rich text area.
            HiddenConfig hiddenConfig = new HiddenConfig();
            // All the parameters of this hidden configuration will be prefixed with the name of the hook.
            hiddenConfig.setNameSpace(hook.getAttribute("name"));
            // This flag is needed in order to detect that a server request contains rich text area data.
            hiddenConfig.addFlag("wysiwyg");
            // The storage syntax for this rich text area.
            hiddenConfig.setParameter(SYNTAX, config.getParameter(SYNTAX, DEFAULT_SYNTAX));
            rootExtension.addFeature(SUBMIT.toString(), hiddenConfig);

            getTextArea().addFocusListener(this);
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
            getTextArea().removeFocusListener(this);
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
}
