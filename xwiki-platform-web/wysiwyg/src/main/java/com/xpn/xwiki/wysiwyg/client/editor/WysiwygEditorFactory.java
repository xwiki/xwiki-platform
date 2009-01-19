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

import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.PluginFactoryManager;
import com.xpn.xwiki.wysiwyg.client.plugin.color.ColorPluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.font.FontPluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.format.FormatPluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImagePluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.importer.ImporterPluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.indent.IndentPluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.DefaultPluginFactoryManager;
import com.xpn.xwiki.wysiwyg.client.plugin.justify.JustifyPluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkPluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.list.ListPluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.separator.SeparatorPluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.symbol.SymbolPluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.sync.SyncPluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.table.TablePluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.text.TextPluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.undo.UndoPluginFactory;
import com.xpn.xwiki.wysiwyg.client.plugin.valign.VerticalAlignPluginFactory;
import com.xpn.xwiki.wysiwyg.client.syntax.SyntaxValidatorManager;
import com.xpn.xwiki.wysiwyg.client.syntax.internal.DefaultSyntaxValidator;
import com.xpn.xwiki.wysiwyg.client.syntax.internal.DefaultSyntaxValidatorManager;
import com.xpn.xwiki.wysiwyg.client.syntax.internal.XWikiSyntaxValidator;
import com.xpn.xwiki.wysiwyg.client.util.Config;

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
        svm.addSyntaxValidator(new DefaultSyntaxValidator("html/4.01"));
        svm.addSyntaxValidator(new DefaultSyntaxValidator("xhtml/1.0"));
        svm.addSyntaxValidator(new XWikiSyntaxValidator());
        // add additional SyntaxValidator for other syntaxes

        pfm = new DefaultPluginFactoryManager();
        pfm.addPluginFactory(SeparatorPluginFactory.getInstance());
        pfm.addPluginFactory(TextPluginFactory.getInstance());
        pfm.addPluginFactory(VerticalAlignPluginFactory.getInstance());
        pfm.addPluginFactory(JustifyPluginFactory.getInstance());
        pfm.addPluginFactory(ListPluginFactory.getInstance());
        pfm.addPluginFactory(IndentPluginFactory.getInstance());
        pfm.addPluginFactory(UndoPluginFactory.getInstance());
        pfm.addPluginFactory(FormatPluginFactory.getInstance());
        pfm.addPluginFactory(FontPluginFactory.getInstance());
        pfm.addPluginFactory(ColorPluginFactory.getInstance());
        pfm.addPluginFactory(SyncPluginFactory.getInstance());
        pfm.addPluginFactory(SymbolPluginFactory.getInstance());
        pfm.addPluginFactory(LinkPluginFactory.getInstance());
        pfm.addPluginFactory(TablePluginFactory.getInstance());
        pfm.addPluginFactory(ImagePluginFactory.getInstance());
        pfm.addPluginFactory(ImporterPluginFactory.getInstance());
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
     * @param config The configuration object.
     * @param wysiwyg The application context.
     * @return the newly created WYSIWYG editor.
     */
    public WysiwygEditor newEditor(Config config, Wysiwyg wysiwyg)
    {
        return new WysiwygEditor(wysiwyg, config, svm, pfm);
    }
}
