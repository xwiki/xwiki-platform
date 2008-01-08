package org.xwiki.platform.patchservice.api;

import java.util.List;

public interface RWPatch extends Patch
{
    void setSpecVersion(String specVersion);

    void setId(PatchId id);

    void setDescription(String description);

    void setOriginator(Originator originator);

    void setOperations(List operations);

    void addOperation(Operation op);

    void clearOperations();
}
