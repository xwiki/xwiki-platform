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
package org.xwiki.model.internal.reference;

import java.io.Serializable;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.text.StringUtils;

/**
 * Generate a string representation of an entity reference (eg "wiki:space.page" for a document reference in the "wiki"
 * Wiki, the "space" Space and the "page" Page).
 * 
 * @version $Id$
 * @since 2.2M1
 */
@Component
@Singleton
public class DefaultStringEntityReferenceSerializer extends AbstractStringEntityReferenceSerializer
{
    @Inject
    private SymbolScheme symbolScheme;

    /**
     * Empty constructor, to be used by the Component Manager, which will also inject the Symbol Scheme.
     */
    public DefaultStringEntityReferenceSerializer()
    {
        // Empty constructor, to be used by the Component Manager, which will also inject the Symbol Scheme
    }

    /**
     * Constructor to be used when using this class as a POJO and not as a component.
     *
     * @param symbolScheme the scheme to use for serializing the passed references (i.e. defines the separators to use
     *            between the Entity types, and the characters to escape and how to escape them)
     */
    public DefaultStringEntityReferenceSerializer(SymbolScheme symbolScheme)
    {
        this.symbolScheme = symbolScheme;
    }

    @Override
    protected void serializeEntityReference(EntityReference currentReference, StringBuilder representation,
        boolean isLastReference, Object... parameters)
    {
        EntityType currentType = currentReference.getType();
        EntityReference parentReference = currentReference.getParent();

        // Since the representation is being built from the root reference (i.e. from left to right), we need to add a
        // separator if some content has already been added to the representation string (i.e. if a higher level entity
        // type has already been processed).
        if (parentReference != null && representation.length() > 0) {
            // Get the separator to use between the previous type and the current type
            Character separator =
                getSymbolScheme().getSeparatorSymbols().get(currentType).get(parentReference.getType());
            if (separator != null) {
                representation.append(separator);
            } else {
                // The reference is invalid, the parent type is not an allowed type. Thus there's no valid separator
                // to separate the 2 types. Use the "???" character to show the user it's invalid.
                representation.append("???");
            }
        }

        // Escape characters that require escaping for the current type
        representation.append(StringUtils.replaceEach(currentReference.getName(),
            getSymbolScheme().getSymbolsRequiringEscapes(currentType),
            getSymbolScheme().getReplacementSymbols(currentType)));

        // Add parameters if supported
        Map<String, Serializable> entityParameters = currentReference.getParameters();
        if (!entityParameters.isEmpty()) {
            Character parameterSeparator = getSymbolScheme().getParameterSeparator(currentType);
            if (parameterSeparator != null) {
                representation.append(parameterSeparator);

                serializeParameters(getSymbolScheme().getDefaultParameter(currentType), parameterSeparator,
                    entityParameters, representation,
                    getSymbolScheme().getParameterSymbolsRequiringEscapes(currentType),
                    getSymbolScheme().getParameterReplacementSymbols(currentType));
            }
        }

    }

    private void serializeParameters(String defaultParameter, char separator, Map<String, Serializable> parameters,
        StringBuilder representation, String[] parameterSymbolsRequiringEscapes, String[] parameterReplacementSymbols)
    {
        boolean first = true;
        for (Map.Entry<String, Serializable> entry : parameters.entrySet()) {
            if (entry.getValue() != null) {
                if (!first) {
                    representation.append(separator);
                }

                if (defaultParameter == null || !defaultParameter.equals(entry.getKey())) {
                    representation.append(entry.getKey());
                    representation.append('=');
                }
                representation.append(StringUtils.replaceEach(entry.getValue().toString(),
                    parameterSymbolsRequiringEscapes, parameterReplacementSymbols));

                first = false;
            }
        }
    }

    protected SymbolScheme getSymbolScheme()
    {
        return this.symbolScheme;
    }
}
