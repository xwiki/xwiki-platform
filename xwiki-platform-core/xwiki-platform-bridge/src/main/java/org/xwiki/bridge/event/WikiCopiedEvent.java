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
package org.xwiki.bridge.event;

/**
 * An event triggered after a wiki has been copied.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the source wiki identifier as {@link String}</li>
 * <li>data: the current {com.xpn.xwiki.XWikiContext} instance</li>
 * </ul>
 * 
 * @version $Id$
 * @since 4.3M1
 */
public class WikiCopiedEvent extends AbstractWikiEvent
{
    /**
     * The version identifier for this Serializable class. Increment only if the <i>serialized</i> form of the class
     * changes.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The wiki where the documents have been copied.
     */
    private String targetWikiId;

    /**
     * Matches all {@link WikiCopiedEvent} events.
     */
    public WikiCopiedEvent()
    {

    }

    /**
     * Matches events affecting the same wikis.
     * 
     * @param sourceWikiId the source wiki identifier
     * @param targetWikiId the target wiki identifier
     */
    public WikiCopiedEvent(String sourceWikiId, String targetWikiId)
    {
        super(sourceWikiId);

        this.targetWikiId = targetWikiId;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        boolean matches = super.matches(otherEvent);

        if (matches) {
            WikiCopiedEvent wikiCopiedEvent = (WikiCopiedEvent) otherEvent;

            matches = getTargetWikiId() == null || getTargetWikiId().equals(wikiCopiedEvent.getTargetWikiId());
        }

        return matches;
    }

    /**
     * @return the source wiki identifier
     */
    public String getSourceWikiId()
    {
        return getWikiId();
    }

    /**
     * @return the target wiki identifier
     */
    public String getTargetWikiId()
    {
        return this.targetWikiId;
    }
}
