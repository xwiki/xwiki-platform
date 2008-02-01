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
package org.xwiki.plugin.spacemanager.api;

import com.xpn.xwiki.XWikiException;

public class SpaceManagerException extends XWikiException
{
    public static final int MODULE_PLUGIN_SPACEMANAGER = 100;

    public static final int ERROR_SPACE_ALREADY_EXISTS = 100001;
    public static final int ERROR_SPACE_TITLE_MISSING = 100002;
    public static final int ERROR_SPACE_DATA_INVALID = 100003;
    public static final int ERROR_SPACE_SENDER_EMAIL_INVALID = 100004;
    public static final int ERROR_SPACE_TARGET_EMAIL_INVALID = 100005;
    public static final int ERROR_SPACE_MANAGER_REQUIRES_MAILSENDER_PLUGIN = 100006;
    public static final int ERROR_SPACE_SENDING_EMAIL_FAILED = 100007;
    public static final int ERROR_SPACE_CANNOT_FIND_EMAIL_ADDRESS = 100008;
    public static final int ERROR_SPACE_CREATION_ABORTED_BY_EXTENSION = 100009;
    
    public static final int ERROR_VALIDATE_TITLE_TOO_SHORT = 100009;
    public static final int ERROR_VALIDATE_TITLE_TOO_LONG = 100010;

    public SpaceManagerException(){
	}

    public SpaceManagerException(int module, int code, String message){
        super(module, code, message);
    }

    public SpaceManagerException(int module, int code, String message, Exception e){
        super(module, code, message, e);
    }

    public SpaceManagerException( XWikiException e ){
        super();
        setModule(e.getModule());
        setCode(e.getCode());
        setException(e.getException());
        setArgs(e.getArgs());
        setMessage(e.getMessage());
	}
}
