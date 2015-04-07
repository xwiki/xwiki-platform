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
package org.xwiki.gwt.wysiwyg.client.plugin.macro;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Service interface used on the client by the macro plug-in. It should have all the methods from {@link MacroService}
 * with an additional {@link AsyncCallback} parameter. This is specific to GWT's architecture.
 * 
 * @version $Id$
 */
public interface MacroServiceAsync
{
    /**
     * Makes a request to the server to get the descriptor for the specified macro.
     * 
     * @param macroId a macro identifier
     * @param syntaxId a syntax identifier
     * @param wikiId a wiki identifier
     * @param async the call-back to be used for notifying the caller after receiving the response from the server
     * @since 6.4M1
     */
    void getMacroDescriptor(String macroId, String syntaxId, String wikiId, AsyncCallback<MacroDescriptor> async);

    /**
     * Makes a request to the server to get the descriptor for the specified macro.
     * 
     * @param macroId a macro identifier
     * @param syntaxId a syntax identifier
     * @param async the call-back to be used for notifying the caller after receiving the response from the server
     */
    void getMacroDescriptor(String macroId, String syntaxId, AsyncCallback<MacroDescriptor> async);

    /**
     * Makes a request to the server to get all the macro descriptors for the specified syntax.
     * 
     * @param syntaxId a syntax identifier
     * @param wikiId a wiki identifier
     * @param async the call-back to be used for notifying the caller after receiving the response from the server
     * @since 6.4M1
     */
    void getMacroDescriptors(String syntaxId, String wikiId, AsyncCallback<List<MacroDescriptor>> async);

    /**
     * Makes a request to the server to get all the macro descriptors for the specified syntax.
     * 
     * @param syntaxId a syntax identifier
     * @param async the call-back to be used for notifying the caller after receiving the response from the server
     */
    void getMacroDescriptors(String syntaxId, AsyncCallback<List<MacroDescriptor>> async);
}
