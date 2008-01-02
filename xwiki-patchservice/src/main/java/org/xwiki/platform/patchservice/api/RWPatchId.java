package org.xwiki.platform.patchservice.api;

import java.util.Date;

public interface RWPatchId extends PatchId
{
    void setHostId(String hostId);

    void setLogicalTime(LogicalTime logicalTime);

    void setTime(Date time);

    void setDocumentId(String documentId);
}
