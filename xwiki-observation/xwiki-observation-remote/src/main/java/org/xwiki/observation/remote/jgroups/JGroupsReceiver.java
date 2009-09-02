package org.xwiki.observation.remote.jgroups;

import org.jgroups.Receiver;
import org.xwiki.component.annotation.ComponentRole;

/**
 * Provide a @ComponentRole for JGRoups {@link Receiver} interface. Can be implemented using channel name as hint to
 * provide a specific {@link Receiver} to a channel.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@ComponentRole
public interface JGroupsReceiver extends Receiver
{
}
