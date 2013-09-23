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

import org.xwiki.component.annotation.Role;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The service interface used on the server.
 * 
 * @version $Id$
 */
@Role
@RemoteServiceRelativePath("MacroService.gwtrpc")
public interface MacroService extends RemoteService
{
    /**
     * @param macroId a macro identifier
     * @param syntaxId a syntax identifier
     * @return an object describing the specified macro
     */
    MacroDescriptor getMacroDescriptor(String macroId, String syntaxId);

    /**
     * @param syntaxId a syntax identifier
     * @return the list of all the macro descriptors for the specified syntax
     */
    List<MacroDescriptor> getMacroDescriptors(String syntaxId);
}
