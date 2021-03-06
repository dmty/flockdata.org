/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
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

package org.flockdata.search.configure;

import javax.annotation.PostConstruct;
import org.flockdata.search.service.SearchAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author mholdsworth
 * @since 7/05/2016
 */
//@EnableDiscoveryClient
@Configuration
@Profile("discovery")
public class DiscoveryConfiguration {//implements InfoContributor {

  private final SearchAdmin searchAdmin;
  private Logger logger = LoggerFactory.getLogger("configuration");

  @Autowired
  public DiscoveryConfiguration(SearchAdmin searchAdmin) {
    this.searchAdmin = searchAdmin;
  }

  @PostConstruct
  public void logStatus() {
    logger.info("**** Discovery Configuration Client configuration deployed");
  }

//    @Override
//    public void contribute(Info.Builder builder) {
//        builder.withDetail("health",
//                searchAdmin.getHealth());
//
//    }

}
