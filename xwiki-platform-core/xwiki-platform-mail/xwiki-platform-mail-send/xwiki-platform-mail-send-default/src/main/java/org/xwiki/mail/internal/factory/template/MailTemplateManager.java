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
package org.xwiki.mail.internal.factory.template;

import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * Provides helper component for sending mail from template.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Role
public interface MailTemplateManager
{
    /**
     * Evaluate a xproperty from a template document containing one or several XWiki.Mail xobjects (one per
     * supported language). The xobject used is matched according to the passed language parameter value.
     *
     * @param templateReference the reference to the template document containing the XWiki.Mail xobject
     * @param property the name of xproperty
     * @param velocityVariables the list of velocity variables to set in the Velocity Context when evaluating
     * @param locale the language value used to find a matching XWiki.Mail xobject (there can be one xobject per
     *        language)
     * @return the evaluated property
     * @throws MessagingException when an error occurs
     * @since 6.1
     */
    String evaluate(DocumentReference templateReference, String property, Map<String, String> velocityVariables,
        Locale locale) throws MessagingException;

    /**
     * Evaluate a xproperty from a template document containing one or several XWiki.Mail xobjects (one per
     * supported language). The xobject used is matched according to the default language defined in the wiki.
     *
     * @param templateReference the reference to the template document containing the XWiki.Mail xobject
     * @param property the name of xproperty
     * @param velocityVariables the list of velocity variables to set in the Velocity Context when evaluating
     * @return the evaluated property
     * @throws MessagingException when an error occurs
     */
    String evaluate(DocumentReference templateReference, String property, Map<String, String> velocityVariables)
        throws MessagingException;
}
