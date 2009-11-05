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
package com.xpn.xwiki.wysiwyg.server.plugin.macro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroCategoryManager;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;

import com.xpn.xwiki.wysiwyg.client.plugin.macro.MacroDescriptor;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.MacroService;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.ParameterDescriptor;

/**
 * XWiki specific implementation of {@link MacroService}.
 * 
 * @version $Id$
 */
public class XWikiMacroService implements MacroService
{
    /**
     * Default XWiki logger to report errors correctly.
     */
    private static final Log LOG = LogFactory.getLog(XWikiMacroService.class);

    /**
     * The syntax factory used to create {@link Syntax} instances from string syntax identifiers.
     */
    @Requirement
    private SyntaxFactory syntaxFactory;

    /**
     * The macro manager used to retrieve macros.
     */
    @Requirement
    private MacroManager macroManager;

    /**
     * The macro category manager used to retrieve macro categories.
     */
    @Requirement
    private MacroCategoryManager categoryManager;

    /**
     * {@inheritDoc}
     * 
     * @see MacroService#getMacroDescriptor(String, String)
     */
    public MacroDescriptor getMacroDescriptor(String macroId, String syntaxId)
    {
        try {
            MacroId macroIdObject = new MacroId(macroId, syntaxFactory.createSyntaxFromIdString(syntaxId));
            Macro< ? > macro = macroManager.getMacro(macroIdObject);
            org.xwiki.rendering.macro.descriptor.MacroDescriptor descriptor = macro.getDescriptor();

            ParameterDescriptor contentDescriptor = null;
            if (descriptor.getContentDescriptor() != null) {
                contentDescriptor = new ParameterDescriptor();
                contentDescriptor.setId("content");
                contentDescriptor.setDescription(descriptor.getContentDescriptor().getDescription());
                // Just a hack to distinguish between regular strings and large strings.
                contentDescriptor.setType(StringBuffer.class.getName());
                contentDescriptor.setMandatory(descriptor.getContentDescriptor().isMandatory());
            }

            Map<String, ParameterDescriptor> parameterDescriptorMap = new HashMap<String, ParameterDescriptor>();
            for (Map.Entry<String, org.xwiki.rendering.macro.descriptor.ParameterDescriptor> entry : descriptor
                .getParameterDescriptorMap().entrySet()) {
                parameterDescriptorMap.put(entry.getKey(), createMacroParameterDescriptor(entry.getValue()));
            }

            MacroDescriptor result = new MacroDescriptor();
            result.setId(macroIdObject.getId());
            result.setName(descriptor.getName());
            result.setDescription(descriptor.getDescription());
            // NOTE: we should set the category also, but we need a new method in MacroCategoryManager.
            result.setContentDescriptor(contentDescriptor);
            result.setParameterDescriptorMap(parameterDescriptorMap);

            return result;
        } catch (Exception e) {
            LOG.error("Exception while retrieving macro descriptor.", e);
            throw new RuntimeException(e.getLocalizedMessage());
        }
    }

    /**
     * Creates a {@link ParameterDescriptor} from a {@link org.xwiki.rendering.macro.descriptor.ParameterDescriptor}.
     * 
     * @param descriptor a macro parameter descriptor from the rendering package
     * @return a macro parameter descriptor from the WYSIWYG package
     */
    private ParameterDescriptor createMacroParameterDescriptor(
        org.xwiki.rendering.macro.descriptor.ParameterDescriptor descriptor)
    {
        ParameterDescriptor result = new ParameterDescriptor();
        result.setId(descriptor.getId());
        result.setDescription(descriptor.getDescription());
        result.setType(getMacroParameterType(descriptor.getType()));
        Object defaultValue = descriptor.getDefaultValue();
        if (defaultValue != null) {
            result.setDefaultValue(String.valueOf(defaultValue));
        }
        result.setMandatory(descriptor.isMandatory());
        return result;
    }

    /**
     * NOTE: We can't send the {@link Class} instance to the client side because it isn't serializable, its source file
     * is not available at build time and currently GWT doesn't support reflection.
     * 
     * @param parameterClass a {@link Class} that defines the values a macro parameter can have
     * @return a {@link String} representation of the specified class that can be used on the client side to assert that
     *         a value is of this type
     */
    private String getMacroParameterType(Class< ? > parameterClass)
    {
        if (parameterClass.isEnum()) {
            return "enum" + Arrays.asList(parameterClass.getEnumConstants());
        } else {
            return parameterClass.getName();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see MacroService#getMacroDescriptors(String)
     */
    public List<MacroDescriptor> getMacroDescriptors(String syntaxId)
    {
        try {
            Syntax syntax = syntaxFactory.createSyntaxFromIdString(syntaxId);
            List<MacroDescriptor> descriptors = new ArrayList<MacroDescriptor>();
            for (String category : categoryManager.getMacroCategories(syntax)) {
                for (MacroId macroId : categoryManager.getMacroIds(category, syntax)) {
                    MacroDescriptor descriptor = getMacroDescriptor(macroId.getId(), syntaxId);
                    descriptor.setCategory(category);
                    descriptors.add(descriptor);
                }
            }

            Collections.sort(descriptors, new Comparator<MacroDescriptor>()
            {
                public int compare(MacroDescriptor alice, MacroDescriptor bob)
                {
                    return alice.getName().compareTo(bob.getName());
                }
            });

            return descriptors;
        } catch (Exception e) {
            LOG.error(String
                .format("Exception while retrieving the list of macro descriptors for syntax %s.", syntaxId), e);
            throw new RuntimeException(e.getLocalizedMessage());
        }
    }
}
