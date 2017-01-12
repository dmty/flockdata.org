/*
 *
 *  Copyright (c) 2012-2017 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.company.dao;

import org.flockdata.data.Company;
import org.flockdata.data.SystemUser;
import org.flockdata.engine.data.graph.CompanyNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * @author mholdsworth
 * @since 20/04/2013
 * @tag Company, Neo4j,
 */
@Repository
public class CompanyDaoNeo {

    private final CompanyRepository companyRepo;

    @Autowired
    public CompanyDaoNeo(CompanyRepository companyRepo) {
        this.companyRepo = companyRepo;
    }

    public Company update(Company company) {
        return companyRepo.save((CompanyNode)company);
    }

    public Company findByPropertyValue(String property, Object value) {
        return companyRepo.findBySchemaPropertyValue(property, value);
    }

    public Collection<Company> findCompanies(Long sysUserId) {
        return companyRepo.getCompaniesForUser(sysUserId);
    }

    public Collection<Company> findCompanies(String userApiKey) {
        return companyRepo.findCompanies(userApiKey);
    }

    public Company create(Company company) {

        return companyRepo.save((CompanyNode)company);
    }

    public SystemUser getAdminUser(Long companyId, String name) {
        return companyRepo.getAdminUser(companyId, name);
    }

    public Company create(String companyName, String uniqueKey) {
        return create(new CompanyNode(companyName, uniqueKey));
    }

}
