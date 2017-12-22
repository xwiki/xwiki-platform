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
package org.xwiki.ratings;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provides Ratings configuration.
 *
 * @version $Id$
 * @since 8.2.1
 */
@Role
public interface RatingsConfiguration
{   
    /**
     * @param documentReference the documentReference for which to return the configuration document
     * @return the configuration document
     */
    XWikiDocument getConfigurationDocument(DocumentReference documentReference);
    
    /**
     * Retrieves configuration parameter from the current space's WebPreferences and fallback to XWiki.RatingsConfig if
     * it does not exist.
     * 
     * @param documentReference the document being rated or for which the existing ratings are fetched
     * @param parameterName the parameter for which to retrieve the value
     * @param defaultValue the default value for the parameter
     * @return the value of the given parameter name from the current configuration context
     */
    String getConfigurationParameter(DocumentReference documentReference, String parameterName, String defaultValue);
}
