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
package org.xwiki.guidedtour.internal;

import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.guidedtour.api.exceptions.DuplicatedIdException;
import org.xwiki.guidedtour.api.exceptions.InvalidIdException;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Base class for Guided Tour REST resources, providing common utilities like authorization checks, CSRF validation and
 * error handling.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
public abstract class AbstractGuidedTourResource
{
    @Inject
    protected ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private Logger logger;

    @Inject
    private Provider<Container> containerProvider;

    @Inject
    private CSRFToken csrf;

    /**
     * Utility method to execute a REST action with common authorization checks and error handling.
     *
     * @param logMessage the message to log for this action, with placeholders for parameters
     * @param logParams the parameters to fill in the log message placeholders
     * @param action the action to execute, which should return a Response
     * @return the Response returned by the action if successful
     */
    public Response execute(String logMessage, Callable<Response> action, Object... logParams)
    {
        try {
            this.logger.debug("Executing: " + logMessage, logParams);
            this.validateCSRF();
            this.contextualAuthorizationManager.checkAccess(Right.VIEW);
            return action.call();
        } catch (AccessDeniedException | SecurityException e) {
            this.logger.warn("Authorization error: " + logMessage, appendException(logParams, e));
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } catch (InvalidIdException e) {
            this.logger.warn("Resource not found: " + logMessage, appendException(logParams, e));
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (DuplicatedIdException e) {
            this.logger.warn("Conflict: " + logMessage, appendException(logParams, e));
            throw new WebApplicationException(Response.Status.CONFLICT);
        } catch (Exception e) {
            this.logger.error("Internal error: " + logMessage, appendException(logParams, e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Utility method to validate the CSRF token from the request. Throws a SecurityException if the token is invalid.
     */
    private void validateCSRF()
    {
        ServletRequest request = (ServletRequest) this.containerProvider.get().getRequest();
        String token = request.getRequest().getHeader("xwiki-form-token");
        if (!this.csrf.isTokenValid(token)) {
            throw new SecurityException("Invalid CSRF token.");
        }
    }

    /**
     * Appends a {@link Throwable} to the end of a parameter array. This is necessary because SLF4J cannot extract a
     * {@code Throwable} from a nested array. It requires a single, flattened array where the exception is the final
     * element. Otherwise, the parameters array will be treated as a single parameter, and the exception will not be
     * logged properly.
     */
    private Object[] appendException(Object[] params, Throwable e)
    {
        if (params == null || params.length == 0) {
            return new Object[] { e };
        }
        Object[] combined = new Object[params.length + 1];
        System.arraycopy(params, 0, combined, 0, params.length);
        combined[params.length] = e;
        return combined;
    }
}
