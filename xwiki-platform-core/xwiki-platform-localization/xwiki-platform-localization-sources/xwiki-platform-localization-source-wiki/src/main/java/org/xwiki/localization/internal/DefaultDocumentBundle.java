package org.xwiki.localization.internal;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;

/**
 * Wiki document based implementation of Bundle.
 * 
 * @see AbstractDocumentBundle
 * @version $Id$
 * @since 4.3M2
 */
public class DefaultDocumentBundle extends AbstractDocumentBundle
{
    /**
     * @param documentReference the document reference
     * @param componentManager used to lookup components needed to manipulate wiki documents
     * @throws ComponentLookupException failed to lookup some required components
     */
    public DefaultDocumentBundle(DocumentReference documentReference, ComponentManager componentManager)
        throws ComponentLookupException
    {
        super(documentReference, componentManager);
    }
}
