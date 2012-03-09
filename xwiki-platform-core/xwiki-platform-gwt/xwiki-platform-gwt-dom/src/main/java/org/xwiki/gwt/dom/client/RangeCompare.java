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
package org.xwiki.gwt.dom.client;

/**
 * Passed as a parameter to the compareBoundaryPoints method.
 * 
 * @version $Id$
 */
public enum RangeCompare
{
    /**
     * Compare start boundary-point of sourceRange to start boundary-point of Range on which compareBoundaryPoints is
     * invoked.
     */
    START_TO_START("StartToStart"),

    /**
     * Compare start boundary-point of sourceRange to end boundary-point of Range on which compareBoundaryPoints is
     * invoked.
     */
    START_TO_END("StartToEnd"),

    /**
     * Compare end boundary-point of sourceRange to end boundary-point of Range on which compareBoundaryPoints is
     * invoked.
     */
    END_TO_END("EndToEnd"),

    /**
     * Compare end boundary-point of sourceRange to start boundary-point of Range on which compareBoundaryPoints is
     * invoked.
     */
    END_TO_START("EndToStart");

    /**
     * The value of this constant, which will be passed to JNI methods. We need it in order to overwrite
     * {@link #toString()} method.
     */
    private String value;

    /**
     * Creates a new RangeCompare constant based on the specified value. This value will be returned by
     * {@link #toString()}.
     * 
     * @param value The value of the created constant.
     */
    RangeCompare(String value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return this.value;
    }

    /**
     * @return The reverse the end points that are compared.
     */
    public RangeCompare reverse()
    {
        switch (this) {
            case START_TO_START:
            case END_TO_END:
                return this;
            case START_TO_END:
                return END_TO_START;
            case END_TO_START:
                return START_TO_END;
            default:
                return null;
        }
    }

    /**
     * @param firstEndPoint true for START and false for END.
     * @param secondEndPoint true for START and false for END.
     * @return the value associated with the specified end points.
     */
    public static RangeCompare valueOf(boolean firstEndPoint, boolean secondEndPoint)
    {
        if (firstEndPoint) {
            // START_TO_
            return secondEndPoint ? START_TO_START : START_TO_END;
        } else {
            // END_TO_
            return secondEndPoint ? END_TO_START : END_TO_END;
        }
    }
}
