package org.xwiki.platform.patchservice.api;

import java.util.Date;

public interface PatchId extends XmlSerializable
{
    String getHostId();

    LogicalTime getLogicalTime();

    Date getTime();

    String getDocumentId();
}
