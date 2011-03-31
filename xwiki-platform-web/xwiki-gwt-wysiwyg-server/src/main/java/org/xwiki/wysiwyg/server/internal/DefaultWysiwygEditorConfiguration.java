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
package org.xwiki.wysiwyg.server.internal;

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.wysiwyg.server.WysiwygEditorConfiguration;

import com.xpn.xwiki.XWikiContext;

/**
 * Default WYSIWYG editor configuration source.
 * 
 * @version $Id$
 */
@Component
public class DefaultWysiwygEditorConfiguration implements WysiwygEditorConfiguration
{
    /**
     * The integer number used to determine if a boolean configuration property is set.
     */
    private static final Integer ONE = new Integer(1);

    /**
     * The component used to access documents. This is temporary till XWiki model is moved into components.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to get a reference to the current wiki.
     */
    @Requirement
    private ModelContext modelContext;

    /** Execution context handler, needed for accessing the XWikiContext. */
    @Requirement
    private Execution execution;

    /**
     * @param propertyName the property name
     * @return the value of the specified property of the {@link #CONFIG_CLASS_REFERENCE} object attached to
     *         {@link #CONFIG_DOCUMENT_REFERENCE}.
     */
    private Object getProperty(String propertyName)
    {
        String currentWiki = modelContext.getCurrentEntityReference().getName();
        DocumentReference configDocumentReference = new DocumentReference(currentWiki, "XWiki", "WysiwygEditorConfig");
        DocumentReference configClassReference =
            new DocumentReference("WysiwygEditorConfigClass", configDocumentReference.getLastSpaceReference());
        Object value = documentAccessBridge.getProperty(configDocumentReference, configClassReference, propertyName);
        if (value == null) {
            String mainWiki = getMainWiki();
            if (!StringUtils.equals(currentWiki, mainWiki)) {
                configDocumentReference.getWikiReference().setName(mainWiki);
                value = documentAccessBridge.getProperty(configDocumentReference, configClassReference, propertyName);
            }
        }
        return value;
    }

    /**
     * @return the name of the main wiki
     */
    private String getMainWiki()
    {
        return ((XWikiContext) execution.getContext().getProperty("xwikicontext")).getMainXWiki();
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#areExternalImagesAllowed()
     */
    public Boolean areExternalImagesAllowed()
    {
        Integer externalImages = (Integer) getProperty("externalImages");
        return externalImages == null ? null : ONE.equals(externalImages);
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
    public Integer getColorsPerRow()
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
    public Boolean isAttachmentSelectionLimited()
    {
        Integer attachmentSelectionLimited = (Integer) getProperty("attachmentSelectionLimited");
        return attachmentSelectionLimited == null ? null : ONE.equals(attachmentSelectionLimited);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#isImageSelectionLimited()
     */
    public Boolean isImageSelectionLimited()
    {
        Integer imageSelectionLimited = (Integer) getProperty("imageSelectionLimited");
        return imageSelectionLimited == null ? null : ONE.equals(imageSelectionLimited);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygEditorConfiguration#isSourceEditorEnabled()
     */
    public Boolean isSourceEditorEnabled()
    {
        Integer sourceEditorEnabled = (Integer) getProperty("sourceEditorEnabled");
        return sourceEditorEnabled == null ? null : ONE.equals(sourceEditorEnabled);
    }
}
