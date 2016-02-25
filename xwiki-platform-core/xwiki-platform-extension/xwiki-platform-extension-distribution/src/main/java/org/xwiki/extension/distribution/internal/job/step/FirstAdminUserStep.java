package org.xwiki.extension.distribution.internal.job.step;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.rightsmanager.RightsManager;

/**
 * Register a new owner if no user exist.
 * 
 * @version $Id$
 * @since 8.0RC1
 */
@Component
@Named(FirstAdminUserStep.ID)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class FirstAdminUserStep extends AbstractDistributionStep
{
    /**
     * The identifier of the step.
     */
    public static final String ID = "firstadminuser";

    @Inject
    private transient Logger logger;

    @Inject
    private transient Provider<XWikiContext> xcontextProvider;

    /**
     * Default constructor.
     */
    public FirstAdminUserStep()
    {
        super(ID);
    }

    @Override
    public void prepare()
    {
        if (getState() == null) {
            setState(State.COMPLETED);

            if (isMainWiki()) {
                try {
                    if (RightsManager.getInstance().countAllGlobalUsersOrGroups(true, null,
                        this.xcontextProvider.get()) == 0) {
                        // If there is no user register one
                        setState(null);
                    }
                } catch (XWikiException e) {
                    this.logger.error("Failed to count global users", e);
                }
            }
        }
    }
}
