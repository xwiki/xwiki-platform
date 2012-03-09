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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
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
@Singleton
public class DefaultWysiwygEditorConfiguration implements WysiwygEditorConfiguration
{
    /**
     * The integer number used to determine if a boolean configuration property is set.
     */
    private static final Integer ONE = new Integer(1);

    /**
     * Space of the editor configuration document.
     */
    private static final String XWIKI_SPACE = "XWiki";

    /**
     * Name of the editor configuration document.
     */
    private static final String EDITOR_CONFIG_DOC = "WysiwygEditorConfig";

    /**
     * The component used to access documents. This is temporary till XWiki model is moved into components.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to get a reference to the current wiki.
     */
    @Inject
    private ModelContext modelContext;

    /** Execution context handler, needed for accessing the XWikiContext. */
    @Inject
    private Execution execution;

    /**
     * @param propertyName the property name
     * @return the value of the specified property of the {@link #CONFIG_CLASS_REFERENCE} object attached to
     *         {@link #CONFIG_DOCUMENT_REFERENCE}.
     */
    private Object getProperty(String propertyName)
    {
        String currentWiki = modelContext.getCurrentEntityReference().getName();
        DocumentReference configDocumentReference = new DocumentReference(currentWiki, XWIKI_SPACE, EDITOR_CONFIG_DOC);
        DocumentReference configClassReference =
            new DocumentReference("WysiwygEditorConfigClass", configDocumentReference.getLastSpaceReference());
        Object value = documentAccessBridge.getProperty(configDocumentReference, configClassReference, propertyName);
        if (value == null) {
            String mainWiki = getMainWiki();
            if (!StringUtils.equals(currentWiki, mainWiki)) {
                configDocumentReference = new DocumentReference(mainWiki, XWIKI_SPACE, EDITOR_CONFIG_DOC);
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

    @Override
    public Boolean areExternalImagesAllowed()
    {
        Integer externalImages = (Integer) getProperty("externalImages");
        return externalImages == null ? null : ONE.equals(externalImages);
    }

    @Override
    public String getColorPalette()
    {
        return (String) getProperty("colorPalette");
    }

    @Override
    public Integer getColorsPerRow()
    {
        return (Integer) getProperty("colorsPerRow");
    }

    @Override
    public String getFontNames()
    {
        return (String) getProperty("fontNames");
    }

    @Override
    public String getFontSizes()
    {
        return (String) getProperty("fontSizes");
    }

    @Override
    public String getMenuBar()
    {
        return (String) getProperty("menuBar");
    }

    @Override
    public String getPlugins()
    {
        return (String) getProperty("plugins");
    }

    @Override
    public String getStyleNames()
    {
        return (String) getProperty("styleNames");
    }

    @Override
    public String getToolBar()
    {
        return (String) getProperty("toolBar");
    }

    @Override
    public Boolean isAttachmentSelectionLimited()
    {
        Integer attachmentSelectionLimited = (Integer) getProperty("attachmentSelectionLimited");
        return attachmentSelectionLimited == null ? null : ONE.equals(attachmentSelectionLimited);
    }

    @Override
    public Boolean isImageSelectionLimited()
    {
        Integer imageSelectionLimited = (Integer) getProperty("imageSelectionLimited");
        return imageSelectionLimited == null ? null : ONE.equals(imageSelectionLimited);
    }

    @Override
    public Boolean isSourceEditorEnabled()
    {
        Integer sourceEditorEnabled = (Integer) getProperty("sourceEditorEnabled");
        return sourceEditorEnabled == null ? null : ONE.equals(sourceEditorEnabled);
    }

    @Override
    public Integer getHistorySize()
    {
        return (Integer) getProperty("historySize");
    }
}
