/*
 * Copyright (c) 2012-2014 "Monowai Developments Limited"
 *
 * This file is part of AuditBucket.
 *
 * AuditBucket is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuditBucket is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuditBucket.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.auditbucket.registration;

import com.auditbucket.helper.DatagioException;
import com.auditbucket.helper.SecurityHelper;
import com.auditbucket.registration.bean.RegistrationBean;
import com.auditbucket.registration.dao.neo4j.model.SystemUserNode;
import com.auditbucket.registration.model.Company;
import com.auditbucket.registration.model.SystemUser;
import com.auditbucket.registration.service.CompanyService;
import com.auditbucket.registration.service.KeyGenService;
import com.auditbucket.registration.service.SystemUserService;
import com.auditbucket.track.service.SchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationServiceNeo4j implements com.auditbucket.registration.service.RegistrationService {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    KeyGenService keyGenService;

    @Autowired
    SchemaService schemaService;

    @Autowired
    private SecurityHelper securityHelper;

    public static SystemUser GUEST = new SystemUserNode("Guest", null, null, false);
    private Logger logger = LoggerFactory.getLogger(RegistrationServiceNeo4j.class);

    @Override
    @Transactional
    public SystemUser registerSystemUser(Company company, RegistrationBean regBean) throws DatagioException {

        SystemUser systemUser = systemUserService.findByLogin(regBean.getLogin());

        if (systemUser != null) {
            logger.debug("Returning existing SU {}", systemUser);
            return systemUser;
        }

        regBean.setCompany(company);
        return makeSystemUser(regBean);
    }

    @Override
    public SystemUser registerSystemUser(RegistrationBean regBean) throws DatagioException {
        // Non-transactional method
        Company company = companyService.findByName(regBean.getCompanyName());
        if (company == null) {
            company = companyService.create(regBean.getCompanyName());
            // indexes have to happen outside of data update transactions
            // else you'll get a Heuristic exception failure
            schemaService.ensureSystemIndexes(company);

        }

        return registerSystemUser(company, regBean);
    }

    @Transactional
    public SystemUser makeSystemUser(RegistrationBean regBean) {
        logger.debug("Creating new system user {}",regBean);
        return systemUserService.save(regBean);


    }

    /**
     * @return currently logged-in SystemUser or Guest if anonymous
     */
    @Transactional
    public SystemUser getSystemUser() {
        String systemUser = securityHelper.getUserName(false, false);
        if (systemUser == null)
            return GUEST;
        SystemUser iSystemUser = systemUserService.findByLogin(systemUser);
        if (iSystemUser == null) {
            // Authenticated in the security system, but not in the graph
            return new SystemUserNode(systemUser, null, null, true);
        } else {
            return iSystemUser;
        }
    }

    @Transactional
    public SystemUser getSystemUser(String apiKey) {
        SystemUser su = systemUserService.findByApiKey(apiKey);
        if (su == null)
            return getSystemUser();
        return su;
    }

    public Company resolveCompany(String apiKey) throws DatagioException {
        Company c = securityHelper.getCompany(apiKey);
        if (c == null)
            throw new DatagioException("Invalid API Key");
        return c;
    }
}
