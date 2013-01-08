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
package org.xwiki.localization.jar.internal;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.localization.internal.AbstractResourceTranslationBundle;
import org.xwiki.localization.message.TranslationMessageParser;

/**
 * Classloader resource based implementation of Bundle.
 * 
 * @see AbstractResourceTranslationBundle
 * @version $Id$
 * @since 4.5M1
 */
public class JARResourceTranslationBundle extends AbstractResourceTranslationBundle
{
    private InstalledExtension installedExtension;

    /**
     * @param installedExtension the resource URL
     * @param componentManager used to lookup components needed to manipulate wiki documents
     * @param translationMessageParser the parser to use for each message
     * @throws ComponentLookupException failed to lookup some required components
     */
    public JARResourceTranslationBundle(InstalledExtension installedExtension, ComponentManager componentManager,
        TranslationMessageParser translationMessageParser) throws ComponentLookupException
    {
        super(resourceURL, componentManager, translationMessageParser);
    }
}
