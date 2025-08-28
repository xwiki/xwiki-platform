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
package org.xwiki.query;

/**
 * Encapsulate a error while processing or executing a query.
 *
 * @version $Id$
 * @since 1.6M1
 */
public class QueryException extends Exception
{
    /**
     * Query object.
     */
    private final Query query;

    /**
     * @param message exception message
     * @param query Query object
     */
    public QueryException(String message, Query query)
    {
        super(message);

        this.query = query;
    }

    /**
     * @param message exception message
     * @param query Query object
     * @param cause nested exception
     */
    public QueryException(String message, Query query, Throwable cause)
    {
        super(message, cause);

        this.query = query;
    }

    @Override
    public String getMessage()
    {
        if (this.query == null) {
            return super.getMessage();
        } else {
            if (this.query.isNamed()) {
                return super.getMessage() + ". Named query = [" + this.query.getStatement() + "]";
            } else {
                return super.getMessage() + ". Query statement = [" + this.query.getStatement() + "]";
            }
        }
    }
}
