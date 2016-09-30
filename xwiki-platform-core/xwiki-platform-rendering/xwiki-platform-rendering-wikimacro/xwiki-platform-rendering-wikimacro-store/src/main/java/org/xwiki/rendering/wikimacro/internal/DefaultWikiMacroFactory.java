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
package org.xwiki.rendering.wikimacro.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.wikibridge.DefaultWikiMacro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroFactory;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameterDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * The default implementation of {@link org.xwiki.rendering.macro.wikibridge.WikiMacroFactory}.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
@Singleton
public class DefaultWikiMacroFactory implements WikiMacroFactory, WikiMacroConstants
{
    /**
     * The {@link ComponentManager} component.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The {@link Execution} component used for accessing XWikiContext.
     */
    @Inject
    private Execution execution;

    /**
     * Used to serialize references of documents.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Content parser used to parse the macro content.
     */
    @Inject
    private ContentParser parser;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Utility method for accessing XWikiContext.
     * 
     * @return the XWikiContext.
     */
    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    @Override
    public WikiMacro createWikiMacro(DocumentReference documentReference) throws WikiMacroException
    {
        XWikiDocument doc;
        try {
            doc = getContext().getWiki().getDocument(documentReference, getContext());
        } catch (XWikiException ex) {
            throw new WikiMacroException(String.format("Could not build macro from : [%s], unable to load document",
                documentReference), ex);
        }
        return buildMacro(doc);
    }

    /**
     * Creates a {@link WikiMacro} from an {@link XWikiDocument} which contains a macro definition.
     * 
     * @param doc the {@link XWikiDocument} to look for a macro definition
     * @return the {@link WikiMacro} found inside the document
     * @throws WikiMacroException when an invalid macro definition or no macro definition was found
     */
    private WikiMacro buildMacro(XWikiDocument doc) throws WikiMacroException
    {
        DocumentReference documentReference = doc.getDocumentReference();

        // Check whether this document contains a macro definition.
        BaseObject macroDefinition = doc.getObject(WIKI_MACRO_CLASS);
        if (null == macroDefinition) {
            throw new WikiMacroException(String.format("No macro definition found in document : [%s]",
                documentReference));
        }

        // Extract macro definition.
        String macroId = macroDefinition.getStringValue(MACRO_ID_PROPERTY);
        String macroName = macroDefinition.getStringValue(MACRO_NAME_PROPERTY);
        // The macro description as plain text
        String macroDescription = macroDefinition.getStringValue(MACRO_DESCRIPTION_PROPERTY);
        String macroDefaultCategory = macroDefinition.getStringValue(MACRO_DEFAULT_CATEGORY_PROPERTY);
        WikiMacroVisibility macroVisibility =
            WikiMacroVisibility.fromString(macroDefinition.getStringValue(MACRO_VISIBILITY_PROPERTY));
        boolean macroSupportsInlineMode = (macroDefinition.getIntValue(MACRO_INLINE_PROPERTY) == 0) ? false : true;
        String macroContentType = macroDefinition.getStringValue(MACRO_CONTENT_TYPE_PROPERTY);
        // The macro content description as plain text
        String macroContentDescription = macroDefinition.getStringValue(MACRO_CONTENT_DESCRIPTION_PROPERTY);
        String macroCode = macroDefinition.getStringValue(MACRO_CODE_PROPERTY);

        // Verify macro id.
        if (StringUtils.isEmpty(macroId)) {
            throw new WikiMacroException(String.format("Incomplete macro definition in [%s], macro id is empty",
                documentReference));
        }

        // Verify macro name.
        if (StringUtils.isEmpty(macroName)) {
            macroName = macroId;
            this.logger.debug(String.format("Incomplete macro definition in [%s], macro name is empty",
                documentReference));
        }

        // Verify macro description.
        if (StringUtils.isEmpty(macroDescription)) {
            this.logger.debug(String.format("Incomplete macro definition in [%s], macro description is empty",
                documentReference));
        }

        // Verify default macro category.
        if (StringUtils.isEmpty(macroDefaultCategory)) {
            macroDefaultCategory = null;
            this.logger.debug(String.format("Incomplete macro definition in [%s], default macro category is empty",
                documentReference));
        }

        // Verify macro content type.
        if (StringUtils.isEmpty(macroContentType)) {
            macroContentType = MACRO_CONTENT_OPTIONAL;
        }

        // Verify macro content description.
        if (!macroContentType.equals(MACRO_CONTENT_EMPTY) && StringUtils.isEmpty(macroContentDescription)) {
            String errorMsg = "Incomplete macro definition in [%s], macro content description is empty";
            this.logger.debug(String.format(errorMsg, documentReference));
            macroContentDescription = "Macro content";
        }

        // Verify macro code.
        if (StringUtils.isEmpty(macroCode)) {
            throw new WikiMacroException(String.format("Incomplete macro definition in [%s], macro code is empty",
                documentReference));
        }

        // Extract macro parameters.
        List<WikiMacroParameterDescriptor> parameterDescriptors = new ArrayList<WikiMacroParameterDescriptor>();
        Vector<BaseObject> macroParameters = doc.getObjects(WIKI_MACRO_PARAMETER_CLASS);
        if (null != macroParameters) {
            for (BaseObject macroParameter : macroParameters) {
                // Vectors can contain null values
                if (null == macroParameter) {
                    continue;
                }

                // Extract parameter definition.
                String parameterName = macroParameter.getStringValue(PARAMETER_NAME_PROPERTY);
                String parameterDescription = macroParameter.getStringValue(PARAMETER_DESCRIPTION_PROPERTY);
                boolean parameterMandatory =
                    (macroParameter.getIntValue(PARAMETER_MANDATORY_PROPERTY) == 0) ? false : true;
                String parameterDefaultValue = macroParameter.getStringValue(PARAMETER_DEFAULT_VALUE_PROPERTY);

                // Verify parameter name.
                if (StringUtils.isEmpty(parameterName)) {
                    throw new WikiMacroException(String.format(
                        "Incomplete macro definition in [%s], macro parameter name is empty", documentReference));
                }

                // Verify parameter description.
                if (StringUtils.isEmpty(parameterDescription)) {
                    String errorMessage = "Incomplete macro definition in [%s], macro parameter description is empty";
                    this.logger.debug(String.format(errorMessage, documentReference));
                }

                // If field empty, assume no default value was provided.
                if (StringUtils.isEmpty(parameterDefaultValue)) {
                    parameterDefaultValue = null;
                }

                // Create the parameter descriptor.
                parameterDescriptors.add(new WikiMacroParameterDescriptor(parameterName, parameterDescription,
                    parameterMandatory, parameterDefaultValue));
            }
        }

        // Create macro content descriptor.
        ContentDescriptor contentDescriptor = null;
        if (!macroContentType.equals(MACRO_CONTENT_EMPTY)) {
            contentDescriptor =
                new DefaultContentDescriptor(macroContentDescription, macroContentType.equals(MACRO_CONTENT_MANDATORY));
        }

        // Create macro descriptor.
        MacroId id = new MacroId(macroId, doc.getSyntax());
        MacroDescriptor macroDescriptor =
            new WikiMacroDescriptor(id, macroName, macroDescription, macroDefaultCategory, macroVisibility,
                contentDescriptor, parameterDescriptors);

        XDOM xdom;
        try {
            xdom = parser.parse(macroCode, doc.getSyntax(), documentReference);
        } catch (MissingParserException ex) {
            throw new WikiMacroException("Could not find a parser for macro content", ex);
        } catch (ParseException ex) {
            throw new WikiMacroException("Error while parsing macro content", ex);
        }

        // Create & return the macro.
        return new DefaultWikiMacro(documentReference, doc.getAuthorReference(), macroSupportsInlineMode,
            macroDescriptor, xdom, doc.getSyntax(), this.componentManager);
    }

    @Override
    public boolean containsWikiMacro(DocumentReference documentReference)
    {
        boolean result;
        try {
            XWikiDocument doc = getContext().getWiki().getDocument(documentReference, getContext());
            BaseObject macroDefinition = doc.getObject(WIKI_MACRO_CLASS);
            result = (null != macroDefinition);
        } catch (XWikiException ex) {
            result = false;
        }
        return result;
    }

    @Override
    public boolean isAllowed(DocumentReference documentReference, WikiMacroVisibility visibility)
    {
        boolean isAllowed = false;

        XWikiContext xcontext = getContext();

        DocumentReference authorReference;

        XWikiDocument doc;
        try {
            doc = xcontext.getWiki().getDocument(documentReference, getContext());
            authorReference = doc.getAuthorReference();
        } catch (XWikiException ex) {
            doc = null;
            authorReference = null;
        }

        try {
            switch (visibility) {
                case GLOBAL:
                    // Verify that the user has programming rights
                    if (doc != null && authorReference != null) {
                        isAllowed =
                            xcontext
                                .getWiki()
                                .getRightService()
                                .hasAccessLevel("programming", this.serializer.serialize(authorReference),
                                    this.serializer.serialize(doc.getDocumentReference()), xcontext);
                    } else {
                        isAllowed = xcontext.getWiki().getRightService().hasProgrammingRights(xcontext);
                    }
                    break;
                case WIKI:
                    // Verify that the user has admin right on the macro wiki
                    if (doc != null && authorReference != null) {
                        isAllowed =
                            xcontext
                                .getWiki()
                                .getRightService()
                                .hasAccessLevel("admin", this.serializer.serialize(authorReference),
                                    doc.getDocumentReference().getWikiReference().getName() + "XWiki.XWikiPreferences",
                                    xcontext);
                    } else {
                        isAllowed = xcontext.getWiki().getRightService().hasWikiAdminRights(xcontext);
                    }
                    break;
                default:
                    isAllowed = true;
            }
        } catch (XWikiException ex) {
            isAllowed = false;
        }

        return isAllowed;
    }
}
