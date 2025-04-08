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
package org.xwiki.refactoring.splitter.criterion.naming;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;

/**
 * Parameters for {@link NamingCriterion}.
 * 
 * @version $Id$
 * @since 14.10.2
 * @since 15.0RC1
 */
public class NamingCriterionParameters
{
    private static final String PARAM_BASE_DOCUMENT_REFERENCE = "baseDocumentReference";

    private static final String PARAM_USE_TERMINAL_PAGES = "useTerminalPages";

    private final Map<String, Object> parameters = new HashMap<>();

    /**
     * Get the value of a given parameter.
     * 
     * @param <T> the parameter type
     * @param key the parameter name
     * @return the value of the specified parameter
     */
    public <T> T getParameter(String key)
    {
        return getParameter(key, null);
    }

    /**
     * Get the value of a given parameter.
     * 
     * @param <T> the parameter type
     * @param key the parameter value
     * @param defaultValue the value to return if the specified parameter is not set
     * @return the value of the specified parameter, if set, otherwise the default value
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue)
    {
        return (T) this.parameters.getOrDefault(key, defaultValue);
    }

    /**
     * Set the value of a parameter.
     * 
     * @param key the parameter name
     * @param value the parameter value
     * @return the previous value
     */
    public Object setParameter(String key, Object value)
    {
        return this.parameters.put(key, value);
    }

    /**
     * @return the base document reference used when creating document references for split parts
     */
    public DocumentReference getBaseDocumentReference()
    {
        return getParameter(PARAM_BASE_DOCUMENT_REFERENCE);
    }

    /**
     * Set the base document reference.
     * 
     * @param baseDocumentReference the new base document reference
     */
    public void setBaseDocumentReference(DocumentReference baseDocumentReference)
    {
        setParameter(PARAM_BASE_DOCUMENT_REFERENCE, baseDocumentReference);
    }

    /**
     * @return {@code true} if the created document references should point to terminal pages, {@code false} otherwise
     */
    public boolean isUseTerminalPages()
    {
        return Boolean.TRUE.equals(getParameter(PARAM_USE_TERMINAL_PAGES));
    }

    /**
     * Set whether to generate terminal document references for the split parts or not.
     * 
     * @param useTerminalPages {@code true} to generate terminal document references, {@code false} otherwise
     */
    public void setUseTerminalPages(boolean useTerminalPages)
    {
        setParameter(PARAM_USE_TERMINAL_PAGES, useTerminalPages);
    }
}
