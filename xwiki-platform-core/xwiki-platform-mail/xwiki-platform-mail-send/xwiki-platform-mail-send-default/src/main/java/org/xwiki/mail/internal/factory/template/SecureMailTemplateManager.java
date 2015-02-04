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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Secure implementation that checks if the current user has vew permissions on the mail template document. To be used
 * by scripts.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Component
@Named("secure")
@Singleton
public class SecureMailTemplateManager implements MailTemplateManager
{
    @Inject
    private MailTemplateManager templateManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private DocumentAccessBridge documentBridge;

    @Override
    public String evaluate(DocumentReference templateReference, String property, Map<String, String> velocityVariables,
        Locale language)
        throws MessagingException
    {
        // Verify that the current user has the view right on the Template document
        if (!this.authorizationManager.hasAccess(
            Right.VIEW, this.documentBridge.getCurrentUserReference(), templateReference))
        {
            throw new MessagingException(
                String.format("Current user [%s] has no permission to view Mail Template Document [%s]",
                    this.documentBridge.getCurrentUserReference(), templateReference));
        }

        return this.templateManager.evaluate(templateReference, property, velocityVariables, language);
    }

    @Override public String evaluate(DocumentReference documentReference, String property,
        Map<String, String> velocityVariables)
        throws MessagingException
    {
        return evaluate(documentReference, property, velocityVariables, null);
    }
}
