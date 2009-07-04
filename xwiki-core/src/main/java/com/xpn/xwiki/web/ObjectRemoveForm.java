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
 *
 */

package com.xpn.xwiki.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ObjectRemoveForm extends ObjectAddForm
{
    private int classId;

    private static final Log LOG = LogFactory.getLog(ObjectRemoveForm.class);

    @Override
    public void readRequest()
    {
        super.readRequest();
        try {
            setClassId(Integer.parseInt(getRequest().getParameter("classid")));
        } catch (Exception ex) {
            setClassId(-1);
            LOG.warn("No or bad classid found while processing an objectremove request: "
                + getRequest().getParameter("classid"));
        }
    }

    public int getClassId()
    {
        return this.classId;
    }

    public void setClassId(int classId)
    {
        this.classId = classId;
    }

}
