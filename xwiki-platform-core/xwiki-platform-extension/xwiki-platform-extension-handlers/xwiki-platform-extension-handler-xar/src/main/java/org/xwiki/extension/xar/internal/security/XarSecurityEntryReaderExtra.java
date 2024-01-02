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
package org.xwiki.extension.xar.internal.security;

import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.XarExtensionConfiguration;
import org.xwiki.extension.xar.XarExtensionConfiguration.DocumentProtection;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.SecurityEntryReaderExtra;
import org.xwiki.security.authorization.SecurityRule;

/**
 * Inject extra rule to protect extension document.
 * 
 * @version $Id$
 * @since 10.5RC1
 */
@Component
@Named("xar")
@Singleton
public class XarSecurityEntryReaderExtra implements SecurityEntryReaderExtra
{
    @Inject
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository xarRepository;

    @Inject
    private XarExtensionConfiguration configuration;

    @Inject
    private XarSecurityTool securityTool;

    @Override
    public Collection<SecurityRule> read(SecurityReference entityReference) throws AuthorizationException
    {
        DocumentProtection protection = this.configuration.getDocumentProtection();

        if (protection.isDeny() && entityReference.getType().ordinal() >= EntityType.DOCUMENT.ordinal()) {
            EntityReference reference = entityReference.extractReference(EntityType.DOCUMENT);

            DocumentReference documentReference = reference instanceof DocumentReference docReference ? docReference
                : new DocumentReference(reference);

            // Edit protection
            if (!((XarInstalledExtensionRepository) this.xarRepository).isAllowed(documentReference, Right.EDIT)) {
                return Collections
                    .singletonList(new XarSecurityRule(Right.EDIT, protection.isSimple(), this.securityTool));
            }

            // Delete protection
            if (!((XarInstalledExtensionRepository) this.xarRepository).isAllowed(documentReference, Right.DELETE)) {
                return Collections
                    .singletonList(new XarSecurityRule(Right.DELETE, protection.isSimple(), this.securityTool));
            }
        }

        return null;
    }
}
