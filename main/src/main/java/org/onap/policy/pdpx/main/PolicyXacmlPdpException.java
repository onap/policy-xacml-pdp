/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pdpx.main;

/**
 * This exception will be called if an error occurs in policy xacml pdp external service.
 */
public class PolicyXacmlPdpException extends Exception {

    private static final long serialVersionUID = 276444549323645044L;

    /**
     * Instantiates a new policy xacml pdp exception with a message.
     *
     * @param message the message
     */
    public PolicyXacmlPdpException(final String message) {
        super(message);
    }

    /**
     * Instantiates a new policy xacml pdp exception with a message and a caused by exception.
     *
     * @param message the message
     * @param exp the exception that caused this exception to be thrown
     */
    public PolicyXacmlPdpException(final String message, final Exception exp) {
        super(message, exp);
    }
}
