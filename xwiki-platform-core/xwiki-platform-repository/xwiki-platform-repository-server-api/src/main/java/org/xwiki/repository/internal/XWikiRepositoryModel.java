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
package org.xwiki.repository.internal;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

public interface XWikiRepositoryModel
{
    // References

    String EXTENSION_CLASSNAME = "ExtensionCode.ExtensionClass";

    String EXTENSIONVERSION_CLASSNAME = "ExtensionCode.ExtensionVersionClass";

    String EXTENSIONDEPENDENCY_CLASSNAME = "ExtensionCode.ExtensionDependencyClass";

    String EXTENSIONPROXY_CLASSNAME = "ExtensionCode.ExtensionProxyClass";

    EntityReference EXTENSION_CLASSREFERENCE = new EntityReference("ExtensionClass", EntityType.DOCUMENT,
        new EntityReference("ExtensionCode", EntityType.SPACE));

    EntityReference EXTENSIONVERSION_CLASSREFERENCE = new EntityReference("ExtensionVersionClass", EntityType.DOCUMENT,
        new EntityReference("ExtensionCode", EntityType.SPACE));

    EntityReference EXTENSIONDEPENDENCY_CLASSREFERENCE = new EntityReference("ExtensionDependencyClass",
        EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    EntityReference EXTENSIONPROXY_CLASSREFERENCE = new EntityReference("ExtensionProxyClass", EntityType.DOCUMENT,
        new EntityReference("ExtensionCode", EntityType.SPACE));

    EntityReference EXTENSION_TEMPLATEREFERENCE = new EntityReference("ExtensionTemplate", EntityType.DOCUMENT,
        new EntityReference("ExtensionCode", EntityType.SPACE));

    String CONFIGURATION_CLASSNAME = "ExtensionCode.RepositoryConfigClass";

    EntityReference CONFIGURATION_CLASSREFERENCE = new EntityReference("RepositoryConfigClass", EntityType.DOCUMENT,
        new EntityReference("ExtensionCode", EntityType.SPACE));

    EntityReference CONFIGURATION_REFERENCE = new EntityReference("RepositoryConfig", EntityType.DOCUMENT,
        new EntityReference("ExtensionCode", EntityType.SPACE));

    // Properties

    String PROP_EXTENSION_ID = "id";

    String PROP_EXTENSION_TYPE = "type";

    String PROP_EXTENSION_LICENSENAME = "licenseName";

    String PROP_EXTENSION_SUMMARY = "summary";

    String PROP_EXTENSION_DESCRIPTION = "description";

    String PROP_EXTENSION_NAME = "name";

    String PROP_EXTENSION_WEBSITE = "website";

    String PROP_EXTENSION_AUTHORS = "authors";

    String PROP_EXTENSION_FEATURES = "features";

    String PROP_VERSION_ID = "id";

    String PROP_VERSION_VERSION = "version";

    String PROP_VERSION_DOWNLOAD = "download";

    String PROP_DEPENDENCY_EXTENSIONVERSION = "extensionVersion";

    String PROP_DEPENDENCY_ID = "id";

    String PROP_DEPENDENCY_CONSTRAINT = "constraint";

    String PROP_PROXY_REPOSITORYID = "repositoryId";

    String PROP_PROXY_REPOSITORYTYPE = "repositoryType";

    String PROP_PROXY_REPOSITORYURI = "repositoryURI";

    String PROP_PROXY_AUTOUPDATE = "autoUpdate";

    String PROP_CONFIGURATION_DEFAULTIDPREFIX = "defaultIdPrefix";

    String PROP_CONFIGURATION_VALIDTYPEs = "validTypes";

    // Consolidation

    String PROP_EXTENSION_LASTVERSION = "lastVersion";

    String PROP_EXTENSION_VALIDEXTENSION = "validExtension";
}
