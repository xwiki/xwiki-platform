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
package org.xwiki.extension.repository.xwiki.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.ExtensionLicenseManager;
import org.xwiki.extension.ExtensionManagerConfiguration;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryException;
import org.xwiki.extension.repository.ExtensionRepositoryFactory;
import org.xwiki.extension.repository.ExtensionRepositoryId;

@Component
@Singleton
@Named("xwiki")
public class XWikiExtensionRepositoryFactory implements ExtensionRepositoryFactory, Initializable
{
    @Inject
    private ExtensionLicenseManager licenseManager;

    @Inject
    private ExtensionManagerConfiguration configuration;

    private Marshaller marshaller;

    private Unmarshaller unmarshaller;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            JAXBContext context = JAXBContext.newInstance("org.xwiki.extension.repository.xwiki.model.jaxb");
            this.marshaller = context.createMarshaller();
            this.unmarshaller = context.createUnmarshaller();
        } catch (Exception e) {
            throw new InitializationException("Failed to create JAXB context", e);
        }
    }

    public Marshaller getMarshaller()
    {
        return this.marshaller;
    }

    public Unmarshaller getUnmarshaller()
    {
        return this.unmarshaller;
    }

    // ExtensionRepositoryFactory

    @Override
    public ExtensionRepository createRepository(ExtensionRepositoryId repositoryId) throws ExtensionRepositoryException
    {
        try {
            return new XWikiExtensionRepository(repositoryId, this, this.licenseManager, this.configuration);
        } catch (Exception e) {
            throw new ExtensionRepositoryException("Failed to create repository [" + repositoryId + "]", e);
        }
    }
}
