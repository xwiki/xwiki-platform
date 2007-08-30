/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 */
package org.xwiki.component.logging;

public interface Logger
{
        /** Typecode for debugging messages. */
        int LEVEL_DEBUG = 0;

        /** Typecode for informational messages. */
        int LEVEL_INFO = 1;

        /** Typecode for warning messages. */
        int LEVEL_WARN = 2;

        /** Typecode for error messages. */
        int LEVEL_ERROR = 3;

        /** Typecode for disabled log levels. */
        int LEVEL_DISABLED = 5;

        void debug( String message );

        void debug( String message, Throwable throwable );

        boolean isDebugEnabled();

        void info( String message );

        void info( String message, Throwable throwable );

        boolean isInfoEnabled();

        void warn( String message );

        void warn( String message, Throwable throwable );

        boolean isWarnEnabled();

        void error( String message );

        void error( String message, Throwable throwable );

        boolean isErrorEnabled();

        Logger getChildLogger( String name );

        int getThreshold();

        void setThreshold( int threshold );

        String getName();
}
