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
package org.xwiki.messagestream.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.messagestream.MessageStream;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Service exposing the {@link MessageStream} functionality, allowing to post messages from the current user.
 * 
 * @version $Id$
 */
@Component
@Named("messageStream")
@Singleton
public class MessageStreamScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    static final String ERROR_KEY = "scriptservice.messageStream.error";

    private static final EntityReference CONFIG_DOCUMENT_REFERENCE =
        new EntityReference("MessageStreamConfig", EntityType.DOCUMENT,
            new EntityReference("XWiki", EntityType.SPACE));

    /**
     * Provides access to the current context.
     */
    @Inject
    protected Execution execution;

    /** The wrapped stream that is exposed in this service. */
    @Inject
    private MessageStream stream;

    /**
     * Post a message to the user's stream, visible to everyone.
     * 
     * @param message the message to store
     * @return {@code true} if the message was successfully posted, {@code false} otherwise
     */
    public boolean postPublicMessage(String message)
    {
        try {
            this.stream.postPublicMessage(message);
            return true;
        } catch (Exception e) {
            setError(e);
            return false;
        }
    }

    /**
     * Post a message to the user's personal stream, displayed on his profile page and aggregated into their follower's
     * streams.
     * 
     * @param message the message to store
     * @return {@code true} if the message was successfully posted, {@code false} otherwise
     */
    public boolean postPersonalMessage(String message)
    {
        try {
            this.stream.postPersonalMessage(message);
            return true;
        } catch (Exception e) {
            setError(e);
            return false;
        }
    }

    /**
     * Post a private message to another user.
     * 
     * @param message the message to send
     * @param user the target user
     * @return {@code true} if the message was successfully posted, {@code false} otherwise
     */
    public boolean postDirectMessageToUser(String message, DocumentReference user)
    {
        try {
            this.stream.postDirectMessageToUser(message, user);
            return true;
        } catch (Exception e) {
            setError(e);
            return false;
        }
    }

    /**
     * Post a message to a specific group of users.
     * 
     * @param message the message to send
     * @param group the target group
     * @return {@code true} if the message was successfully posted, {@code false} otherwise
     */
    public boolean postMessageToGroup(String message, DocumentReference group)
    {
        try {
            this.stream.postMessageToGroup(message, group);
            return true;
        } catch (Exception e) {
            setError(e);
            return false;
        }
    }

    /**
     * Delete an existing message, identified by its unique ID, if the current user is the author of that message.
     * 
     * @param id the unique ID of the message
     * @return {@code true} if the message was successfully deleted, {@code false} otherwise
     */
    public boolean deleteMessage(String id)
    {
        try {
            this.stream.deleteMessage(id);
            return true;
        } catch (Exception e) {
            setError(e);
            return false;
        }
    }

    /**
     * @return true if the Message Stream feature is active or false otherwise
     * @since 8.4RC1
     */
    public boolean isActive()
    {
        boolean result = false;

        // TODO: Introduce a MessageStreamConfiguration class
        try {
            ExecutionContext ec = this.execution.getContext();
            if (ec != null) {
                XWikiContext xcontext = (XWikiContext) ec.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
                if (xcontext != null) {
                    XWiki xwiki = xcontext.getWiki();
                    XWikiDocument configDocument = xwiki.getDocument(CONFIG_DOCUMENT_REFERENCE, xcontext);
                    BaseObject configObject = configDocument.getXObject(CONFIG_DOCUMENT_REFERENCE);
                    int active = configObject.getIntValue("active");
                    result = (active == 1);
                }
            }
        } catch (Exception e) {
            setError(e);
            return false;
        }

        return result;
    }

    /**
     * Get the error generated while performing the previously called action.
     *
     * @return the exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    protected void setError(Exception e)
    {
        this.execution.getContext().setProperty(ERROR_KEY, e);
    }
}
