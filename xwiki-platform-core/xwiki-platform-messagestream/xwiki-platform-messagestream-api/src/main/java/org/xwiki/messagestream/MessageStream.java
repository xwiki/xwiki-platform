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
package org.xwiki.messagestream;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;

/**
 * The message stream allows to post and retrieve short messages, from one user to one of a few possible targets: direct
 * private messages to another user, messages to a group of users, personal messages to all the users that "follow" the
 * sender.
 * 
 * @version $Id$
 * @since 3.0M3
 * @deprecated MessageStream should not be used anymore, please check extensions for alternatives.
 */
@Role
@Deprecated(since = "16.8.0RC1")
public interface MessageStream
{
    /**
     * Post a message to the current user's stream, visible to everyone.
     * 
     * @param message the message to store
     */
    void postPublicMessage(String message);

    /**
     * Post a message to the current user's personal stream, displayed on his profile page and aggregated into their
     * follower's streams.
     * 
     * @param message the message to store
     */
    void postPersonalMessage(String message);

    /**
     * Get the 30 most recent messages posted by the current user.
     * 
     * @return a list of recent personal messages
     */
    List<Event> getRecentPersonalMessages();

    /**
     * Get the most recent messages posted by the current user, at most {@code limit}, and skipping the first {@code
     * offset}.
     * 
     * @param limit the maximum number of messages to return
     * @param offset how many messages to skip
     * @return a list of recent personal messages, possibly empty
     */
    List<Event> getRecentPersonalMessages(int limit, int offset);

    /**
     * Get the 30 most recent personal messages posted by the specified user.
     * 
     * @param author the user that wrote the messages
     * @return a list of recent personal messages, possibly empty
     */
    List<Event> getRecentPersonalMessages(DocumentReference author);

    /**
     * Get the most recent direct messages sent to the current user, at most {@code limit}, and skipping the first
     * {@code offset}.
     * 
     * @param author the user that wrote the messages
     * @param limit the maximum number of messages to return
     * @param offset how many messages to skip
     * @return a list of recent personal messages, possibly empty
     */
    List<Event> getRecentPersonalMessages(DocumentReference author, int limit, int offset);

    /**
     * Post a private message to another user.
     * 
     * @param message the message to send
     * @param user the target user
     */
    void postDirectMessageToUser(String message, DocumentReference user);

    /**
     * Get the 30 most recent direct messages sent to the current user.
     * 
     * @return a list of recent direct messages received
     */
    List<Event> getRecentDirectMessages();

    /**
     * Get the most recent direct messages sent to the current user, at most {@code limit}, and skipping the first
     * {@code offset}.
     * 
     * @param limit the maximum number of messages to return
     * @param offset how many messages to skip
     * @return a list of recent direct messages received
     */
    List<Event> getRecentDirectMessages(int limit, int offset);

    /**
     * Post a message to a specific group of users.
     * 
     * @param message the message to send
     * @param group the target group
     */
    void postMessageToGroup(String message, DocumentReference group);

    /**
     * Get the 30 most recent messages sent to the specified group.
     * 
     * @param group the target group for which to retrieve messages
     * @return a list of recent messages sent to the target group
     */
    List<Event> getRecentMessagesForGroup(DocumentReference group);

    /**
     * Get the most recent messages sent to the specified group, at most {@code limit}, and skipping the first {@code
     * offset}.
     * 
     * @param group the target group for which to retrieve messages
     * @param limit the maximum number of messages to return
     * @param offset how many messages to skip
     * @return a list of recent messages sent to the target group
     */
    List<Event> getRecentMessagesForGroup(DocumentReference group, int limit, int offset);

    /**
     * Delete an existing message, identified by its unique ID, if the current user is the author of that message.
     * 
     * @param id the unique ID of the message
     */
    void deleteMessage(String id);
}
