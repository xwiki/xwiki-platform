package org.xwiki.plugin.activitystream.api;

/**
 * The priorities possible for an Activity Event. This allows to filter complex events
 * by only keeping the most high level event or by getting page level event.
 *
 */
public interface ActivityEventPriority {

    /**
     * The event is a storage level event (low level)
     */
    int STORAGE = 10;

    /**
     * The event is a notification level event (low level)
     */
    int NOTIFICATION = 20;

    /**
     * The event is a action level event (high level)
     */
    int ACTION = 30;

    /**
     * The event is a program level event (higgest level)
     */
    int PROGRAM = 40;

}
