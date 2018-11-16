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
package org.xwiki.rendering.display.html.script;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.displayer.HTMLDisplayerException;
import org.xwiki.displayer.HTMLDisplayerManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Exposes {@link org.xwiki.displayer.HTMLDisplayer}s to scripts.
 *
 * @version $Id$
 * @since 10.10RC1
 */
@Unstable
@Named("htmldisplayer")
@Component
@Singleton
public class HTMLDisplayerScriptService implements ScriptService
{
    @Inject
    private HTMLDisplayerManager htmlDisplayerManager;

    /**
     * @param value the value on which the display is based on
     * @return the html element based on the value and the type
     * @throws HTMLDisplayerException if an error occurs during the display
     */
    String display(Object value) throws HTMLDisplayerException
    {
        return display(value, new HashMap<>());
    }

    /**
     * @param value the value on which the display is based on
     * @param parameters parameters used while generating the html. Could be the attributes of an input for instance.
     * @return the html element based on the value and the type
     * @throws HTMLDisplayerException if an error occurs during the display
     */
    String display(Object value, Map<String, String> parameters) throws HTMLDisplayerException
    {
        return display(value, parameters, "view");
    }

    /**
     * @param value the value on which the display is based on
     * @param parameters parameters used while generating the html. Could be the attributes of an input for instance.
     * @param mode the display mode (view, edit, ...)
     * @return the html element based on the value and the type
     * @throws HTMLDisplayerException if an error occurs during the display
     */
    String display(Object value, Map<String, String> parameters, String mode) throws HTMLDisplayerException
    {
        return htmlDisplayerManager.getHTMLDisplayer(value.getClass()).display(value, parameters, mode);
    }
}
