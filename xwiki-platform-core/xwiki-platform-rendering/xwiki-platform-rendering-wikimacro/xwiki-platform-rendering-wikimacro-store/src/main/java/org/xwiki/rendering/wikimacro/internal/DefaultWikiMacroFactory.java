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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
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
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

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

    @Inject
    private AuthorizationManager authorization;

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
            throw new WikiMacroException(
                String.format("Could not build macro from : [%s], unable to load document", documentReference), ex);
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
            throw new WikiMacroException(
                String.format("No macro definition found in document : [%s]", documentReference));
        }

        // Extract macro definition.
        String macroId = getMacroId(macroDefinition);
        String macroName = getMacroName(macroDefinition, macroId);
        // The macro description as plain text
        String macroDescription = macroDefinition.getStringValue(MACRO_DESCRIPTION_PROPERTY);
        String macroDefaultCategory = macroDefinition.getStringValue(MACRO_DEFAULT_CATEGORY_PROPERTY);
        WikiMacroVisibility macroVisibility =
            WikiMacroVisibility.fromString(macroDefinition.getStringValue(MACRO_VISIBILITY_PROPERTY));
        boolean macroSupportsInlineMode = macroDefinition.getIntValue(MACRO_INLINE_PROPERTY) != 0;
        String macroContentType = StringUtils
            .defaultIfEmpty(macroDefinition.getStringValue(MACRO_CONTENT_TYPE_PROPERTY), MACRO_CONTENT_OPTIONAL);
        // The macro content description as plain text
        String macroContentDescription = macroDefinition.getStringValue(MACRO_CONTENT_DESCRIPTION_PROPERTY);

        // Verify macro description.
        if (StringUtils.isEmpty(macroDescription)) {
            this.logger.debug("Incomplete macro definition in [{}], macro description is empty", documentReference);
        }

        // Verify default macro category.
        if (StringUtils.isEmpty(macroDefaultCategory)) {
            macroDefaultCategory = null;
            this.logger.debug("Incomplete macro definition in [{}], default macro category is empty",
                documentReference);
        }

        // Verify macro content description.
        if (!macroContentType.equals(MACRO_CONTENT_EMPTY) && StringUtils.isEmpty(macroContentDescription)) {
            this.logger.debug("Incomplete macro definition in [{}], macro content description is empty",
                documentReference);
            macroContentDescription = "Macro content";
        }

        // Verify macro code.
        checkMacroCode(macroDefinition);

        // Extract macro parameters.
        List<WikiMacroParameterDescriptor> parameterDescriptors = buildParameterDescriptors(doc);

        // Create macro content descriptor.
        ContentDescriptor contentDescriptor = null;
        if (!macroContentType.equals(MACRO_CONTENT_EMPTY)) {
            contentDescriptor =
                new DefaultContentDescriptor(macroContentDescription, macroContentType.equals(MACRO_CONTENT_MANDATORY));
        }

        // Create macro descriptor.
        // Note that we register wiki macros for all syntaxes FTM and there's currently no way to restrict a wiki
        // macro for a given syntax only.
        MacroId id = new MacroId(macroId);
        MacroDescriptor macroDescriptor = new WikiMacroDescriptor.Builder().id(id).name(macroName)
            .description(macroDescription).defaultCategory(macroDefaultCategory).visibility(macroVisibility)
            .supportsInlineMode(macroSupportsInlineMode).contentDescriptor(contentDescriptor)
            .parameterDescriptors(parameterDescriptors).build();

        // Create & return the macro.
        try {
            return new DefaultWikiMacro(macroDefinition, macroDescriptor, this.componentManager);
        } catch (Exception e) {
            throw new WikiMacroException("Failed to create the macro", e);
        }
    }

    private String getMacroId(BaseObject macroDefinition) throws WikiMacroException
    {
        String macroId = macroDefinition.getStringValue(MACRO_ID_PROPERTY);

        // Verify macro id.
        if (StringUtils.isEmpty(macroId)) {
            throw new WikiMacroException(String.format("Incomplete macro definition in [%s], macro id is empty",
                macroDefinition.getReference()));
        }

        return macroId;
    }

    private String getMacroName(BaseObject macroDefinition, String macroId)
    {
        String macroName = macroDefinition.getStringValue(MACRO_NAME_PROPERTY);

        // Verify macro name.
        if (StringUtils.isEmpty(macroName)) {
            macroName = macroId;
            this.logger.debug("Incomplete macro definition in [{}], macro name is empty",
                macroDefinition.getReference());
        }

        return macroName;
    }

    private void checkMacroCode(BaseObject macroDefinition) throws WikiMacroException
    {
        String macroCode = macroDefinition.getStringValue(MACRO_CODE_PROPERTY);

        if (StringUtils.isEmpty(macroCode)) {
            throw new WikiMacroException(String.format("Incomplete macro definition in [%s], macro code is empty",
                macroDefinition.getReference()));
        }
    }

    private List<WikiMacroParameterDescriptor> buildParameterDescriptors(XWikiDocument doc) throws WikiMacroException
    {
        List<WikiMacroParameterDescriptor> parameterDescriptors = new ArrayList<>();
        Collection<BaseObject> macroParameters = doc.getObjects(WIKI_MACRO_PARAMETER_CLASS);
        if (macroParameters != null) {
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
                String type = macroParameter.getStringValue(PARAMETER_TYPE_PROPERTY);
                Type parameterType = null;
                if (!StringUtils.isEmpty(type)) {
                    try {
                        parameterType =
                                ReflectionUtils.unserializeType(type, Thread.currentThread().getContextClassLoader());
                    } catch (ClassNotFoundException e) {
                        throw new WikiMacroException("Couldn't unserialize the given type.", e);
                    }
                }

                // Verify parameter name.
                if (StringUtils.isEmpty(parameterName)) {
                    throw new WikiMacroException(
                        String.format("Incomplete macro definition in [%s], macro parameter name is empty",
                            doc.getDocumentReference()));
                }

                // Verify parameter description.
                if (StringUtils.isEmpty(parameterDescription)) {
                    this.logger.debug("Incomplete macro definition in [{}], macro parameter description is empty",
                        doc.getDocumentReference());
                }

                // If field empty, assume no default value was provided.
                if (StringUtils.isEmpty(parameterDefaultValue)) {
                    parameterDefaultValue = null;
                }

                // Create the parameter descriptor.
                parameterDescriptors.add(new WikiMacroParameterDescriptor(parameterName, parameterDescription,
                    parameterMandatory, parameterDefaultValue, parameterType));
            }
        }

        return parameterDescriptors;
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
        } catch (XWikiException e) {
            this.logger.error("Failed to get document", e);

            doc = null;
            authorReference = null;
        }

        switch (visibility) {
            case GLOBAL:
                // Verify that the user has programming rights
                isAllowed = doc != null && this.authorization.hasAccess(Right.PROGRAM, authorReference, null);
                break;
            case WIKI:
                // Verify that the user has admin right on the macro wiki
                isAllowed = doc != null && this.authorization.hasAccess(Right.ADMIN, authorReference,
                    doc.getDocumentReference().getWikiReference());
                break;
            default:
                isAllowed = true;
        }

        return isAllowed;
    }
}
