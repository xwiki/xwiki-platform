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

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

public interface XWikiRepositoryModel
{
    // References

    public final static String EXTENSION_CLASSNAME = "ExtensionCode.ExtensionClass";

    public final static String EXTENSIONVERSION_CLASSNAME = "ExtensionCode.ExtensionVersionClass";

    public final static String EXTENSIONDEPENDENCY_CLASSNAME = "ExtensionCode.ExtensionDependencyClass";

    public final static String EXTENSIONPROXY_CLASSNAME = "ExtensionCode.ExtensionProxyClass";

    public final static EntityReference EXTENSION_CLASSREFERENCE = new EntityReference("ExtensionClass",
        EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    public final static EntityReference EXTENSIONVERSION_CLASSREFERENCE = new EntityReference("ExtensionVersionClass",
        EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    public final static EntityReference EXTENSIONDEPENDENCY_CLASSREFERENCE = new EntityReference(
        "ExtensionDependencyClass", EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    public final static EntityReference EXTENSIONPROXY_CLASSREFERENCE = new EntityReference("ExtensionProxyClass",
        EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    public final static EntityReference EXTENSION_TEMPLATEREFERENCE = new EntityReference("ExtensionTemplate",
        EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    // Properties

    public final static String PROP_EXTENSION_ID = "id";

    public static final String PROP_EXTENSION_TYPE = "type";

    public static final String PROP_EXTENSION_LICENSENAME = "licenseName";

    public static final String PROP_EXTENSION_SUMMARY = "summary";

    public static final String PROP_EXTENSION_DESCRIPTION = "description";

    public static final String PROP_EXTENSION_NAME = "name";

    public static final String PROP_EXTENSION_WEBSITE = "website";

    public static final String PROP_EXTENSION_AUTHORS = "authors";

    public static final String PROP_EXTENSION_FEATURES = "features";

    public final static String PROP_VERSION_ID = "id";

    public final static String PROP_VERSION_VERSION = "version";

    public final static String PROP_VERSION_DOWNLOAD = "download";

    public static final String PROP_DEPENDENCY_EXTENSIONVERSION = "extensionVersion";

    public static final String PROP_DEPENDENCY_ID = "id";

    public static final String PROP_DEPENDENCY_CONSTRAINT = "constraint";

    public static final String PROP_PROXY_REPOSITORYID = "repositoryId";

    public static final String PROP_PROXY_REPOSITORYTYPE = "repositoryType";

    public static final String PROP_PROXY_REPOSITORYURI = "repositoryURI";

    // Consolidation

    public final static String PROP_EXTENSION_LASTVERSION = "lastVersion";

    public final static String PROP_EXTENSION_VALIDEXTENSION = "validExtension";
}
