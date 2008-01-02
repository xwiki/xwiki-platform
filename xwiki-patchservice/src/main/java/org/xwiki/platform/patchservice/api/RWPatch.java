package org.xwiki.platform.patchservice.api;

import java.util.List;

public interface RWPatch extends Patch
{
    void setSpecVersion(String specVersion);

    void setId(PatchId id);

    void setOriginator(Originator originator);

    void setDescription(String description);

    void setOperations(List operations);

    void addOperation(Operation op);

    void clearOperations();

    /*
     * We shouldn't make our own hash/sign methods, but instead use Standard W3C XML signatures. See
     * http://santuario.apache.org/
     */
    // void setHash(String hash);
    //
    // void computeHash();
    //
    // void setSignature(String sign);
    //
    // void sign();
}
