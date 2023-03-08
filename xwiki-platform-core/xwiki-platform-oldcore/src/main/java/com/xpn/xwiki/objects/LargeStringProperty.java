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
package com.xpn.xwiki.objects;

import org.xwiki.logging.event.LogEvent;
import org.xwiki.store.merge.MergeManagerResult;

import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;

public class LargeStringProperty extends BaseStringProperty
{
    private static final long serialVersionUID = 1L;

    @Override
    protected void mergeValue(Object previousValue, Object newValue, MergeConfiguration configuration,
        MergeResult mergeResult)
    {
        MergeManagerResult<String, String> valueMergeManagerResult = getMergeManager()
            .mergeLines((String) previousValue, (String) newValue, getValue(), configuration);
        for (LogEvent logEvent : valueMergeManagerResult.getLog()) {
            String newMessage = String.format("%s [Location: %s]", logEvent.getMessage(), getObject().getReference());
            LogEvent copyLog =
                new LogEvent(logEvent.getLevel(), newMessage, logEvent.getArgumentArray(), logEvent.getThrowable());
            mergeResult.getLog().add(copyLog);
        }
        mergeResult.setModified(mergeResult.isModified() || valueMergeManagerResult.isModified());
        setValue(valueMergeManagerResult.getMergeResult());
    }
}
