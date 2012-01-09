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
package org.xwiki.gwt.wysiwyg.client;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.wysiwyg.client.plugin.PluginFactoryManager;
import org.xwiki.gwt.wysiwyg.client.plugin.color.ColorPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.embed.EmbedPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.font.FontPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.format.FormatPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.history.HistoryPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImagePluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.importer.ImportPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.indent.IndentPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.DefaultPluginFactoryManager;
import org.xwiki.gwt.wysiwyg.client.plugin.justify.JustifyPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.line.LinePluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.list.ListPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.readonly.ReadOnlyPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.separator.SeparatorPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.style.StylePluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.submit.SubmitPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.symbol.SymbolPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.table.TablePluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.text.TextPluginFactory;
import org.xwiki.gwt.wysiwyg.client.plugin.valign.VerticalAlignPluginFactory;
import org.xwiki.gwt.wysiwyg.client.syntax.SyntaxValidatorManager;
import org.xwiki.gwt.wysiwyg.client.syntax.internal.DefaultSyntaxValidator;
import org.xwiki.gwt.wysiwyg.client.syntax.internal.DefaultSyntaxValidatorManager;

/**
 * Factory for {@link WysiwygEditor}. Holds the responsibility of injecting the {@link PluginFactoryManager} and
 * {@link SyntaxValidatorManager} in each editor created.
 * 
 * @version $Id$
 */
public final class WysiwygEditorFactory
{
    /**
     * The singleton factory instance.
     */
    private static WysiwygEditorFactory instance;

    /**
     * The {@link SyntaxValidatorManager} injected in each editor created.
     */
    private SyntaxValidatorManager svm;

    /**
     * The {@link PluginFactoryManager} injected in each editor created.
     */
    private PluginFactoryManager pfm;

    /**
     * Initializes the {@link SyntaxValidatorManager} and {@link PluginFactoryManager} instances that will be injected
     * in the future editors.
     */
    private WysiwygEditorFactory()
    {
        svm = new DefaultSyntaxValidatorManager();
        svm.addSyntaxValidator(new DefaultSyntaxValidator("xhtml/1.0"));
        // add additional SyntaxValidator for other syntaxes

        pfm = new DefaultPluginFactoryManager();
        pfm.addPluginFactory(ReadOnlyPluginFactory.getInstance());
        pfm.addPluginFactory(LinePluginFactory.getInstance());
        pfm.addPluginFactory(SubmitPluginFactory.getInstance());
        pfm.addPluginFactory(SeparatorPluginFactory.getInstance());
        pfm.addPluginFactory(TextPluginFactory.getInstance());
        pfm.addPluginFactory(VerticalAlignPluginFactory.getInstance());
        pfm.addPluginFactory(JustifyPluginFactory.getInstance());
        pfm.addPluginFactory(ListPluginFactory.getInstance());
        pfm.addPluginFactory(IndentPluginFactory.getInstance());
        pfm.addPluginFactory(HistoryPluginFactory.getInstance());
        pfm.addPluginFactory(FormatPluginFactory.getInstance());
        pfm.addPluginFactory(FontPluginFactory.getInstance());
        pfm.addPluginFactory(ColorPluginFactory.getInstance());
        // The experimental real-time editing feature.
        // pfm.addPluginFactory(SyncPluginFactory.getInstance());
        pfm.addPluginFactory(SymbolPluginFactory.getInstance());
        pfm.addPluginFactory(LinkPluginFactory.getInstance());
        pfm.addPluginFactory(TablePluginFactory.getInstance());
        pfm.addPluginFactory(ImagePluginFactory.getInstance());
        pfm.addPluginFactory(ImportPluginFactory.getInstance());
        pfm.addPluginFactory(MacroPluginFactory.getInstance());
        pfm.addPluginFactory(EmbedPluginFactory.getInstance());
        pfm.addPluginFactory(StylePluginFactory.getInstance());
        // add additional PluginFactory for other plug-ins
    }

    /**
     * @return the singleton factory instance.
     */
    public static synchronized WysiwygEditorFactory getInstance()
    {
        if (instance == null) {
            instance = new WysiwygEditorFactory();
        }
        return instance;
    }

    /**
     * Creates a new editor for the given configuration in the specified context.
     * 
     * @param config the configuration object
     * @return the newly created WYSIWYG editor
     */
    public WysiwygEditor newEditor(Config config)
    {
        return new WysiwygEditor(config, svm, pfm);
    }
}
