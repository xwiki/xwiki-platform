package org.xwiki.rendering.async.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.async.AsyncContext;

/**
 * Default implementation of {@link AsyncContext}.
 * 
 * @version $Id$$
 * @since 10.10RC1
 */
@Component
@Singleton
public class DefaultAsyncContext implements AsyncContext
{
    private static final String KEY_ENABLED = "rendering.async.enabled";

    @Inject
    private Execution execution;

    @Override
    public boolean isEnabled()
    {
        ExecutionContext econtext = this.execution.getContext();

        if (econtext != null) {
            return econtext.getProperty(KEY_ENABLED) == Boolean.TRUE;
        }

        return false;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        this.execution.getContext().setProperty(KEY_ENABLED, enabled);
    }
}
