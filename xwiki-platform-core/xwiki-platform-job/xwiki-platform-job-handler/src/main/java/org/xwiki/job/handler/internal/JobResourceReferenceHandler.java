package org.xwiki.job.handler.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.url.internal.ParentResourceReference;

/**
 * Sub-handlers for the {@link JobRootResourceReferenceHandler}.
 * 
 * @version $Id$
 * @since 10.2RC1
 */
@Role
public interface JobResourceReferenceHandler
{
    /**
     * @param reference the reference
     * @throws ResourceReferenceHandlerException when failing to handle the resource reference
     */
    void handle(ParentResourceReference reference) throws ResourceReferenceHandlerException;
}
