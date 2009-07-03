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
package org.xwiki.configuration.internal;

import org.xwiki.component.annotation.Component;

/**
 * Configuration source taking its data in the Space Preferences wiki document
 * (using data from the XWiki.XWikiPreferences object attached to that document). 
 *  
 * @version $Id$
 * @since 2.0M2
 */
@Component("space")
public class SpacePreferencesConfigurationSource extends AbstractDocumentConfigurationSource
{
    private static final String DOCUMENT_NAME = "WebPreferences";
    
    private static final String CLASS_NAME = "XWiki.XWikiPreferences";

    @Override
    protected String getClassName()
    {
        return CLASS_NAME;
    }

    @Override
    protected String getDocumentName()
    {
        return DOCUMENT_NAME;
    }
}
