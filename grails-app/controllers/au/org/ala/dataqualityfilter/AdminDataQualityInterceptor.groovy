/*
 * Copyright (C) 2019 Atlas of Living Australia
 * All Rights Reserved.
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

package au.org.ala.dataqualityfilter

/**
 * Interceptor for Admin controller, adapted from grails 2 `beforeInterceptor` code.
 *  See {@link AdminDataQualityController}
 */
class AdminDataQualityInterceptor {
    def authService

    boolean before() {

        if (grailsApplication.config.security.oidc && !grailsApplication.config.security.oidc.enabled.toBoolean()) {
            // Standard Grails oidc config - bypass
            true
        } else if (!grailsApplication.config.getProperty("security.cas.casServerName") && grailsApplication.config.getProperty('security.cas.bypass', Boolean, false)) {
            // Standard Grails cas config - bypass
            true
        } else if (!grailsApplication.config.getProperty("casServerName") && grailsApplication.config.getProperty("disableCAS", Boolean, false)) {
            // old-style AUTH config - bypass
            true
        } else if (!authService?.userInRole(grailsApplication.config.getProperty("auth.admin_role", String, "ROLE_ADMIN"))) {
            log.debug "User not authorised to access the page: ${params.controller}/${params.action?:''}. Redirecting to index."
            flash.message = "You are not authorised to access the page: ${params.controller}/${params.action?:''}."
            render(status: 403)
            false
        } else {
            true
        }
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
