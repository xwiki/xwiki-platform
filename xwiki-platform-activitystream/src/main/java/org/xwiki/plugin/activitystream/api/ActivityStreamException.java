package org.xwiki.plugin.activitystream.api;

import com.xpn.xwiki.XWikiException;

/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * <p/>
 * This is free software;you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation;either version2.1of
 * the License,or(at your option)any later version.
 * <p/>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software;if not,write to the Free
 * Software Foundation,Inc.,51 Franklin St,Fifth Floor,Boston,MA
 * 02110-1301 USA,or see the FSF site:http://www.fsf.org.
 */

public class ActivityStreamException extends XWikiException {
    public static final int MODULE_PLUGIN_ActivityStream = 101;

    public static final int ERROR_INVITATION_INVITEE_MISSING = 101001;
    public static final int ERROR_INVITATION_INVITEE_EMAIL_INVALID = 101002;
    public static final int ERROR_INVITATION_ALREADY_EXISTS = 101003;
    public static final int ERROR_INVITATION_ALREADY_MEMBER = 101004;
    public static final int ERROR_INVITATION_SENDER_EMAIL_INVALID = 101005;
    public static final int ERROR_INVITATION_TARGET_EMAIL_INVALID = 101006;
    public static final int ERROR_INVITATION_SENDING_EMAIL_FAILED = 101007;
    public static final int ERROR_INVITATION_MANAGER_REQUIRES_MAILSENDER_PLUGIN = 101008;
    
    public static final int ERROR_INVITATION_DATA_INVALID = 101009;
    public static final int ERROR_INVITATION_CANNOT_FIND_EMAIL_ADDRESS = 101010;

    public ActivityStreamException(){
	}

    public ActivityStreamException(int module, int code, String message){
        super(module, code, message);
    }

    public ActivityStreamException(int module, int code, String message, Exception e){
        super(module, code, message, e);
    }

    public ActivityStreamException( XWikiException e ){
        super();
        setModule(e.getModule());
        setCode(e.getCode());
        setException(e.getException());
        setArgs(e.getArgs());
        setMessage(e.getMessage());
	}
}
