/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.pivotal.cla.service;

/**
 * @author Rob Winch
 *
 */
public class CurrentUserRequest {
	private OAuthAccessTokenParams oauthParams;

	private boolean requestAdminAccess;

	/**
	 * @return the requestAdminAccess
	 */
	public boolean isRequestAdminAccess() {
		return requestAdminAccess;
	}

	/**
	 * @param requestAdminAccess the requestAdminAccess to set
	 */
	public void setRequestAdminAccess(boolean requestAdminAccess) {
		this.requestAdminAccess = requestAdminAccess;
	}

	/**
	 * @return the oauthParams
	 */
	public OAuthAccessTokenParams getOauthParams() {
		return oauthParams;
	}

	/**
	 * @param oauthParams the oauthParams to set
	 */
	public void setOauthParams(OAuthAccessTokenParams oauthParams) {
		this.oauthParams = oauthParams;
	}
}