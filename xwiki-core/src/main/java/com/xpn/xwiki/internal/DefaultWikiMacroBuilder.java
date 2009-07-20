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

package com.xpn.xwiki.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroBuilder;
import org.xwiki.rendering.macro.wikibridge.WikiMacroBuilderException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameterDescriptor;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * The default implementation of {@link WikiMacroBuilder}.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
public class DefaultWikiMacroBuilder extends AbstractLogEnabled implements WikiMacroBuilder
{
    /**
     * Constant for representing XWiki.WikiMacroClass xwiki class.
     */
    String WIKI_MACRO_CLASS = "XWiki.WikiMacroClass";

    /**
     * Constant for representing macro name property.
     */
    String MACRO_NAME_PROPERTY = "name";
    
    /**
     * Constant for representing macro name property.
     */
    String MACRO_DESCRIPTION_PROPERTY = "description";
    
    /**
     * Constant for representing macro name property.
     */
    String MACRO_CONTENT_PROPERTY = "content";
    
    /**
     * Constant for representing XWiki.WikiMacroParameterClass xwiki class.
     */
    String WIKI_MACRO_PARAMETER_CLASS = "XWiki.WikiMacroParameterClass";
    
    /**
     * Constant for representing parameter name property.
     * 
     * Same as MACRO_NAME_PROPERTY (Check style Fix)
     */
    String PARAMETER_NAME_PROPERTY = MACRO_NAME_PROPERTY; 
    
    /**
     * Constant for representing parameter description property.
     * 
     * Same as MACRO_DESCRIPTION_PROPERTY (Check style Fix)
     */
    String PARAMETER_DESCRIPTION_PROPERTY = MACRO_DESCRIPTION_PROPERTY; 
    
    /**
     * Constant for representing parameter mandatory property.
     */
    String PARAMETER_MANDATORY_PROPERTY = "mandatory";    
    
    /**
     * The {@link ComponentManager} component.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * The {@link Execution} component used for accessing XWikiContext.
     */
    @Requirement
    private Execution execution;

    /**
     * Utility method for accessing XWikiContext.
     * 
     * @return the XWikiContext.
     */
    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    /**
     * {@inheritDoc}
     */
    public WikiMacro buildMacro(String documentName) throws WikiMacroBuilderException
    {
        XWikiDocument doc = null;
        try {
            doc = getContext().getWiki().getDocument(documentName, getContext());
        } catch (XWikiException ex) {
            throw new WikiMacroBuilderException(String.format(
                "Could not build macro from : [%s], unable to load document", documentName), ex);
        }
        return buildMacro(doc);
    }

    /**
     * Creates a {@link WikiMacro} from an {@link XWikiDocument} which contains a macro definition.
     * 
     * @param doc the {@link XWikiDocument} to look for a macro definition.
     * @return a {@link WikiMacro} found inside the document.
     * @throws WikiMacroBuilderException invalid macro definition / no macro definition found.
     */
    private WikiMacro buildMacro(XWikiDocument doc) throws WikiMacroBuilderException
    {
        // Check whether this document contains a macro definition.
        BaseObject macroDefinition = doc.getObject(WIKI_MACRO_CLASS);
        if (null == macroDefinition) {
            throw new WikiMacroBuilderException(String.format("No macro definition found in document : [%s]", doc
                .getFullName()));
        }

        // Extract macro definition.
        String macroName = macroDefinition.getStringValue(MACRO_NAME_PROPERTY);
        String macroDescription = macroDefinition.getStringValue(MACRO_DESCRIPTION_PROPERTY);
        String macroContent = macroDefinition.getStringValue(MACRO_CONTENT_PROPERTY);

        // Verify macro name.
        if (StringUtils.isEmpty(macroName)) {
            throw new WikiMacroBuilderException(String.format(
                "Incomplete macro definition in [%s], macro name is empty", doc.getFullName()));
        }

        // Verify macro description.
        if (StringUtils.isEmpty(macroDescription)) {
            getLogger().warn(
                String.format("Incomplete macro definition in [%s], macro description is empty", doc.getFullName()));
        }

        // Verify macro content.
        if (StringUtils.isEmpty(macroContent)) {
            throw new WikiMacroBuilderException(String.format(
                "Incomplete macro definition in [%s], macro content is empty", doc.getFullName()));
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

                // Verify parameter name.
                if (StringUtils.isEmpty(parameterName)) {
                    throw new WikiMacroBuilderException(String.format(
                        "Incomplete macro definition in [%s], macro parameter name is empty", doc.getFullName()));
                }

                // Verify parameter description.
                if (StringUtils.isEmpty(parameterDescription)) {
                    String errorMessage = "Incomplete macro definition in [%s], macro parameter description is empty";
                    getLogger().warn(String.format(errorMessage, doc.getFullName()));
                }

                // Create the parameter descriptor.
                parameterDescriptors.add(new WikiMacroParameterDescriptor(parameterName, parameterDescription,
                    parameterMandatory));
            }
        }

        // Create the macro descriptor.
        MacroDescriptor macroDescriptor = new WikiMacroDescriptor(macroDescription, parameterDescriptors);

        // Create & return the macro.
        return new WikiMacro(doc.getFullName(), macroName, macroDescriptor, macroContent, doc.getSyntaxId(),
            componentManager);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsMacro(String documentName)
    {
        boolean result = true;
        try {
            XWikiDocument doc = getContext().getWiki().getDocument(documentName, getContext());
            BaseObject macroDefinition = doc.getObject(WIKI_MACRO_CLASS);
            result = (null != macroDefinition); 
        } catch (XWikiException ex) {
            result = false;
        }                
        return result;
    }        
}
