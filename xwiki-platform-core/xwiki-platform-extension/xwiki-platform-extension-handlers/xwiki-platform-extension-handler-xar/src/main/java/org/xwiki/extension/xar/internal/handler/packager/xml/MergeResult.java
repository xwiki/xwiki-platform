package org.xwiki.extension.xar.internal.handler.packager.xml;

import java.util.ArrayList;
import java.util.List;

public class MergeResult
{
    private boolean modified = false;

    private List<Exception> errors = new ArrayList<Exception>();

    private List<Exception> warnings = new ArrayList<Exception>();

    public void setModified(boolean modified)
    {
        this.modified = modified;
    }

    public boolean isModified()
    {
        return modified;
    }

    public List<Exception> getErrors()
    {
        return errors;
    }

    public List<Exception> getWarnings()
    {
        return warnings;
    }
}
