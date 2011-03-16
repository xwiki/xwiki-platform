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
package com.xpn.xwiki.wysiwyg.server.internal;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.wysiwyg.server.WysiwygEditorConfiguration;

/**
 * Default WYSIWYG editor configuration source.
 * 
 * @version $Id$
 */
@Component
public class DefaultWysiwygEditorConfiguration implements WysiwygEditorConfiguration
{
    /**
     * A reference to the configuration XClass.
     */
    private static final DocumentReference CONFIG_CLASS_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "WysiwygEditorConfigClass");

    /**
     * A reference to the document that holds an instance of {@link #CONFIG_CLASS_REFERENCE}.
     */
    private static final DocumentReference CONFIG_DOCUMENT_REFERENCE =
        new DocumentReference(CONFIG_CLASS_REFERENCE.getWikiReference().getName(), CONFIG_CLASS_REFERENCE
            .getLastSpaceReference().getName(), "WysiwygEditorConfig");

    /**
     * The component used to access documents. This is temporary till XWiki model is moved into components.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    /**
     * @param propertyName the property name
     * @return the value of the specified property of the {@link #CONFIG_CLASS_REFERENCE} object attached to
     *         {@link #CONFIG_DOCUMENT_REFERENCE}.
     */
    private Object getProperty(String propertyName)
    {
        return documentAccessBridge.getProperty(CONFIG_DOCUMENT_REFERENCE, CONFIG_CLASS_REFERENCE, propertyName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#areExternalImagesAllowed()
     */
    public boolean areExternalImagesAllowed()
    {
        return (Integer) getProperty("externalImages") == 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#getColorPalette()
     */
    public String getColorPalette()
    {
        return (String) getProperty("colorPalette");
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#getColorsPerRow()
     */
    public int getColorsPerRow()
    {
        return (Integer) getProperty("colorsPerRow");
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#getFontNames()
     */
    public String getFontNames()
    {
        return (String) getProperty("fontNames");
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#getFontSizes()
     */
    public String getFontSizes()
    {
        return (String) getProperty("fontSizes");
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#getMenuBar()
     */
    public String getMenuBar()
    {
        return (String) getProperty("menuBar");
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#getPlugins()
     */
    public String getPlugins()
    {
        return (String) getProperty("plugins");
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#getStyleNames()
     */
    public String getStyleNames()
    {
        return (String) getProperty("styleNames");
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#getToolBar()
     */
    public String getToolBar()
    {
        return (String) getProperty("toolBar");
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#isAttachmentSelectionLimited()
     */
    public boolean isAttachmentSelectionLimited()
    {
        return (Integer) getProperty("attachmentSelectionLimited") == 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#isImageSelectionLimited()
     */
    public boolean isImageSelectionLimited()
    {
        return (Integer) getProperty("imageSelectionLimited") == 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#isSourceEditorEnabled()
     */
    public boolean isSourceEditorEnabled()
    {
        return (Integer) getProperty("sourceEditorEnabled") == 1;
    }
}
