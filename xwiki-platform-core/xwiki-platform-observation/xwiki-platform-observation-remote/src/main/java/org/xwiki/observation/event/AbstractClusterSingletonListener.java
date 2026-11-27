/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.observation.event;

import java.util.List;

import jakarta.inject.Inject;

import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.ClusterSingletonManager;
import org.xwiki.observation.remote.ClusterSingletonNodeChangedEvent;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.stability.Unstable;

/**
 * Base class for the listeners whose work must run on a single member of the cluster, the cluster singleton node (see
 * {@link ClusterSingletonManager}), for example because it works on a resource shared by all the members.
 * <ul>
 * <li>When one of the events passed to the constructor is received (and it's not a remote event),
 * {@link #processSingletonEvent(Event, Object, Object)} is called if the current instance is the singleton node. The
 * first of these events also marks the listener as ready, generally it's the event which starts the work of the
 * listener (for example {@code ApplicationReadyEvent}).</li>
 * <li>When the current instance becomes the singleton node after the listener is ready,
 * {@link #onSingletonNodeGained()} is called so that the work which the previous singleton node was doing can be
 * resumed (catch-up).</li>
 * <li>When the current instance stops being the singleton node after the listener is ready (including when the
 * instance stops), {@link #onSingletonNodeLost()} is called so that the periodic work can be stopped.</li>
 * </ul>
 * <p>
 * The listener remembers the state it last acted on and reads the current state from the
 * {@link ClusterSingletonManager} when a change is notified, so a change which was already taken into account (for
 * example by the ready event) or which is outdated does not trigger the work twice, and nothing is started once the
 * instance is stopping.
 * <p>
 * A listener registered after the first of its events was sent (for example by an extension installed at runtime)
 * never becomes ready on its own: it should call {@link #initializeSingletonState()} when it's registered (for
 * example from its component initialization), which calls {@link #onSingletonNodeGained()} if the current instance is
 * the singleton node.
 * <p>
 * The decision and the action are serialized: {@link #processSingletonEvent(Event, Object, Object)} (when
 * {@link #isSingletonNodeRequired()} is {@code true}), {@link #onSingletonNodeGained()} and
 * {@link #onSingletonNodeLost()} are called while holding a lock of the listener, the same lock which guards the state
 * the listener last acted on. So a change of singleton node processed by another thread (the changes are notified from
 * a dedicated thread of the {@link ClusterSingletonManager}, and the loss from the thread stopping the application)
 * cannot happen between the moment the listener decides to act and the action itself: a loss is always processed
 * after a start which was decided before it, and once the notification of the loss sent when the application stops
 * returns, no action of this listener is running or will be started. As a consequence, these methods are never called
 * concurrently for a given listener.
 * <p>
 * The implementations must not block (start a job or a thread instead): a concurrent change of singleton node, and in
 * particular the synchronous loss notified when the application stops, waits for the running action. They must not
 * wait for another thread which delivers an event to this listener, nor synchronously notify an event handled by
 * another cluster singleton listener whose actions could do the same, since that would be a lock cycle. Calling
 * {@link ClusterSingletonManager#isSingletonNode()} or notifying other events from an action is safe: the
 * {@link ClusterSingletonManager} never holds a lock while notifying the listeners and its own lock is never held while
 * waiting for anything else. Since the singleton node can change at any time (and two members can briefly both
 * consider themselves the singleton node, for example during a network partition), the work should be idempotent.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Unstable
public abstract class AbstractClusterSingletonListener extends AbstractEventListener
{
    /**
     * Used to know if the current instance is the cluster singleton node.
     */
    @Inject
    protected ClusterSingletonManager singletonManager;

    /**
     * Used to skip remote events.
     */
    @Inject
    protected RemoteObservationManagerContext remoteObservationManagerContext;

    /**
     * Guards {@link #ready} and {@link #singletonNode}, and is held while acting on them (calling
     * {@link #processSingletonEvent(Event, Object, Object)}, {@link #onSingletonNodeGained()} and
     * {@link #onSingletonNodeLost()}) so that a change of singleton node cannot be processed between a decision and its
     * action.
     */
    private final Object lock = new Object();

    private boolean ready;

    /**
     * The state this listener last acted on.
     */
    private boolean singletonNode;

    /**
     * Creates a listener for the passed events (and {@link ClusterSingletonNodeChangedEvent}).
     *
     * @param name the listener's name. It's a free form text identifying this listener instance in a unique manner.
     *            This name is used for some operations in {@link ObservationManager}.
     * @param events the list of events this listener is configured to receive, {@link ClusterSingletonNodeChangedEvent}
     *            is automatically added to it
     */
    protected AbstractClusterSingletonListener(String name, Event... events)
    {
        super(name, events, new ClusterSingletonNodeChangedEvent());
    }

    /**
     * Creates a listener for the passed events (and {@link ClusterSingletonNodeChangedEvent}).
     *
     * @param name the listener's name. It's a free form text identifying this listener instance in a unique manner.
     *            This name is used for some operations in {@link ObservationManager}.
     * @param events the list of events this listener is configured to receive, {@link ClusterSingletonNodeChangedEvent}
     *            is automatically added to it
     */
    protected AbstractClusterSingletonListener(String name, List<? extends Event> events)
    {
        super(name, events, new ClusterSingletonNodeChangedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof ClusterSingletonNodeChangedEvent) {
            if (isSingletonNodeRequired()) {
                onSingletonNodeChanged();
            }
        } else if (!isSingletonNodeRequired()) {
            // The work of this listener must currently run on every member: behave like a standard listener
            processSingletonEvent(event, source, data);
        } else if (!this.remoteObservationManagerContext.isRemoteState()) {
            // Act while holding the lock: a loss processed by another thread must not happen between the decision and
            // the action (it would be undone by the action)
            synchronized (this.lock) {
                boolean currentSingletonNode = this.singletonManager.isSingletonNode();
                if (!this.ready) {
                    this.ready = true;
                    this.singletonNode = currentSingletonNode;
                }

                if (currentSingletonNode) {
                    processSingletonEvent(event, source, data);
                }
            }
        }
    }

    private void onSingletonNodeChanged()
    {
        // Act while holding the lock: another change (for example the loss notified when the application stops) must
        // not happen between the decision and the action
        synchronized (this.lock) {
            if (this.ready) {
                // Don't trust the event data, which might be outdated: the current state is what matters
                boolean currentSingletonNode = this.singletonManager.isSingletonNode();
                if (currentSingletonNode != this.singletonNode) {
                    this.singletonNode = currentSingletonNode;
                    if (currentSingletonNode) {
                        onSingletonNodeGained();
                    } else {
                        onSingletonNodeLost();
                    }
                }
            }
        }
    }

    /**
     * Marks the listener as ready and remembers the current state, for a listener registered after the first of its
     * events was sent. Changes notified before this call are ignored. If the current instance is the cluster singleton
     * node, {@link #onSingletonNodeGained()} is called while holding the lock of the listener (like for any change), so
     * that a concurrent loss is processed after it: the caller should not start the work itself.
     *
     * @return {@code true} if the current instance is the cluster singleton node (in which case
     *         {@link #onSingletonNodeGained()} was called)
     */
    protected boolean initializeSingletonState()
    {
        synchronized (this.lock) {
            this.ready = true;
            this.singletonNode = this.singletonManager.isSingletonNode();
            if (this.singletonNode) {
                onSingletonNodeGained();
            }

            return this.singletonNode;
        }
    }

    /**
     * Indicates if the work of this listener must currently run on the cluster singleton node only. The default is
     * {@code true}. A listener whose work is duplicated only when the resource it works on is shared between the
     * members of the cluster (for example an index which can be stored either in a server shared by all the members or
     * in each member) can return {@code false} when that resource is not shared: all the events it listens to (local
     * and remote) are then passed to {@link #processSingletonEvent(Event, Object, Object)} on every member and the
     * changes of singleton node are ignored.
     *
     * @return {@code true} if the work of this listener must run on the cluster singleton node only
     */
    protected boolean isSingletonNodeRequired()
    {
        return true;
    }

    /**
     * Handle the events passed to the constructor, see {@link #onEvent(Event, Object, Object)} for details. When
     * {@link #isSingletonNodeRequired()} is {@code true} (the default), only the local events received while the
     * current instance is the cluster singleton node are passed to this method, while holding the lock of the listener
     * (so it must not block).
     *
     * @param event the event triggered
     * @param source the event source, for example the document if the event is a document update event
     * @param data some additional and optional data passed that can be acted on
     */
    protected abstract void processSingletonEvent(Event event, Object source, Object data);

    /**
     * Called when the current instance becomes the cluster singleton node, after the listener is ready (when the
     * current instance becomes the singleton node before that, the first event is passed to
     * {@link #processSingletonEvent(Event, Object, Object)} instead), and by {@link #initializeSingletonState()} when
     * the current instance is the singleton node. The previous singleton node may have stopped or crashed in the middle
     * of its work, so this is generally where the work is resumed or done again. Called while holding the lock of the
     * listener: it must not block. The default implementation does nothing.
     */
    protected void onSingletonNodeGained()
    {
        // Nothing to do by default
    }

    /**
     * Called when the current instance stops being the cluster singleton node (because a member with a higher
     * priority joined the cluster or because the current instance is stopping), after the listener is ready. This is
     * generally where the periodic work is stopped. Called while holding the lock of the listener: it must not block
     * (when the instance stops, the thread stopping the application waits for it). The default implementation does
     * nothing.
     */
    protected void onSingletonNodeLost()
    {
        // Nothing to do by default
    }
}
