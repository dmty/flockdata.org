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

package org.flockdata.track;

import java.util.HashSet;
import java.util.Set;
import org.flockdata.data.EntityLog;

/**
 * @author mholdsworth
 * @since 7/07/2015
 */
public class EntityLogs {

  Set<EntityLog> entityLogs = new HashSet<>();

  public Set<EntityLog> getEntityLogs() {
    return entityLogs;
  }

  public void add(EntityLog entityLog) {
    this.entityLogs.add(entityLog);
  }
}
