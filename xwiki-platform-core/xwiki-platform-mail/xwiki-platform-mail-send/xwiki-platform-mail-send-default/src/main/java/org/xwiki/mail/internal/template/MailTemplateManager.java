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
package org.xwiki.mail.internal.template;

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
     * Evaluate xproperty from template document containing a XWiki.Mail xobject.
     *
     * @param documentReference the document reference of template containing XWiki.Mail xobject
     * @param property the name of xproperty
     * @param velocityVariables the list of velocity variables
     * @param language the value of language xproperty to select
     * @return Evaluated property
     * @throws MessagingException when an error occurs
     */
    String evaluate(DocumentReference documentReference, String property, Map<String, String> velocityVariables,
        String language)
        throws MessagingException;

    /**
     * Evaluate xproperty from template document containing a XWiki.Mail xobject.
     *
     * @param documentReference the document reference of template containing XWiki.Mail xobject
     * @param property the name of xproperty
     * @param velocityVariables the list of velocity variables
     * @return Evaluated property
     * @throws MessagingException when an error occurs
     */
    String evaluate(DocumentReference documentReference, String property, Map<String, String> velocityVariables)
        throws MessagingException;
}
