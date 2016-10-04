/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.permission;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface UserPermissionMapper {

  /**
   * List of user permissions ordered by alphabetical order of user names
   *
   * @param query non-null query including optional filters.
   * @param userLogins if null, then filter on all active users. If not null, then filter on logins, including disabled users.
   *                   Must not be empty. If not null then maximum size is {@link org.sonar.db.DatabaseUtils#PARTITION_SIZE_FOR_ORACLE}.
   */
  List<ExtendedUserPermissionDto> selectByQuery(@Param("query") PermissionQuery query, @Nullable @Param("userLogins") Collection<String> userLogins, RowBounds rowBounds);

  /**
   * Count the number of distinct users returned by {@link #selectByQuery(PermissionQuery, Collection, RowBounds)}
   * {@link PermissionQuery#getPageOffset()} and {@link PermissionQuery#getPageSize()} are ignored.
   *
   * @param useNull must always be null. It is needed for using the sql of {@link #selectByQuery(PermissionQuery, Collection, RowBounds)}
   */
  int countUsersByQuery(@Param("query") PermissionQuery query, @Nullable @Param("userLogins") Collection<String> useNull);

  /**
   * Count the number of users per permission for a given list of projects.
   * @param projectIds a non-null and non-empty list of project ids
   */
  List<CountPerProjectPermission> countUsersByProjectPermission(@Param("projectIds") List<Long> projectIds);

  void insert(UserPermissionDto dto);

  /**
   * Delete permissions by user and/or by project. In both cases scope can be restricted to a specified permission
   */
  void delete(@Nullable @Param("login") String login, @Nullable @Param("projectUuid") String projectUuid, @Nullable @Param("permission") String permission);
}
