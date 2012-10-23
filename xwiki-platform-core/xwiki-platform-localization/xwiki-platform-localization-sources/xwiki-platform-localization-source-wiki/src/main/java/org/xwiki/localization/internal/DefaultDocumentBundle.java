package org.xwiki.localization.internal;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;

public class DefaultDocumentBundle extends AbstractDocumentBundle
{
    public static final String HINT = "document";

    public DefaultDocumentBundle(DocumentReference reference, ComponentManager componentManager)
        throws ComponentLookupException
    {
        super(reference, componentManager);
    }
}
