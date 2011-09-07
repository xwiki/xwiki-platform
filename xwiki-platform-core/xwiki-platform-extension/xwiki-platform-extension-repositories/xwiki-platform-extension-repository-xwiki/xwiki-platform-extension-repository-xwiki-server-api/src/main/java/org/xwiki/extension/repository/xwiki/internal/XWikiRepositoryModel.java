package org.xwiki.extension.repository.xwiki.internal;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

public interface XWikiRepositoryModel
{
    public final static String EXTENSION_CLASSNAME = "ExtensionCode.ExtensionClass";

    public final static String EXTENSIONVERSION_CLASSNAME = "ExtensionCode.ExtensionVersionClass";

    public final static String EXTENSIONDEPENDENCY_CLASSNAME = "ExtensionCode.ExtensionVersionClass";

    public final static EntityReference EXTENSION_CLASSREFERENCE = new EntityReference("ExtensionClass",
        EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    public final static EntityReference EXTENSIONVERSION_CLASSREFERENCE = new EntityReference("ExtensionVersionClass",
        EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    public final static EntityReference EXTENSIONDEPENDENCY_CLASSREFERENCE = new EntityReference(
        "ExtensionDependencyClass", EntityType.DOCUMENT, new EntityReference("ExtensionCode", EntityType.SPACE));

    public final static String PROP_VERSION_VERSION = "version";

    public final static String PROP_EXTENSION_LASTVERSION = "lastVersion";
}
