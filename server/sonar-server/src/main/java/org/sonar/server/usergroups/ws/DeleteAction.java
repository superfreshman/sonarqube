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
package org.sonar.server.usergroups.ws;

import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.user.GroupDto;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_ID;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.defineWsGroupParameters;

public class DeleteAction implements UserGroupsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupWsSupport support;
  private final Settings settings;

  public DeleteAction(DbClient dbClient, UserSession userSession, GroupWsSupport support, Settings settings) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.support = support;
    this.settings = settings;
  }

  @Override
  public void define(NewController context) {
    WebService.NewAction action = context.createAction("delete")
      .setDescription(format("Delete a group. The default groups cannot be deleted.<br/>" +
        "'%s' or '%s' must be provided.<br />" +
        "Requires System Administrator permission.",
        PARAM_GROUP_ID, PARAM_GROUP_NAME))
      .setHandler(this)
      .setSince("5.2")
      .setPost(true);

    defineWsGroupParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkPermission(GlobalPermissions.SYSTEM_ADMIN);

    DbSession dbSession = dbClient.openSession(false);
    try {
      GroupDto group = support.findGroup(dbSession, request);
      long groupId = group.getId();

      checkNotTryingToDeleteDefaultGroup(dbSession, groupId);
      checkNotTryingToDeleteLastSystemAdminGroup(dbSession, group.getName());
      removeGroupMembers(dbSession, groupId);
      removeGroupPermissions(dbSession, groupId);
      removeFromPermissionTemplates(dbSession, groupId);
      dbClient.groupDao().deleteById(dbSession, groupId);

      dbSession.commit();
      response.noContent();
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private void checkNotTryingToDeleteDefaultGroup(DbSession dbSession, long groupId) {
    String defaultGroupName = settings.getString(CoreProperties.CORE_DEFAULT_GROUP);
    GroupDto defaultGroup = dbClient.groupDao().selectOrFailByName(dbSession, defaultGroupName);
    checkArgument(groupId != defaultGroup.getId(),
      format("Default group '%s' cannot be deleted", defaultGroupName));
  }

  private void checkNotTryingToDeleteLastSystemAdminGroup(DbSession dbSession, String groupName) {
    boolean hasAdminPermission = dbClient.roleDao()
      .selectGroupPermissions(dbSession, groupName, null)
      .contains(GlobalPermissions.SYSTEM_ADMIN);
    boolean isOneRemainingAdminGroup = dbClient.groupPermissionDao().countGroups(dbSession, GlobalPermissions.SYSTEM_ADMIN, null) == 1;
    boolean hasNoStandaloneAdminUser = dbClient.userPermissionDao().countUsers(dbSession,
      PermissionQuery.builder().setPermission(GlobalPermissions.SYSTEM_ADMIN).withAtLeastOnePermission().build()) == 0;
    boolean isLastAdminGroup = hasAdminPermission && isOneRemainingAdminGroup && hasNoStandaloneAdminUser;

    checkArgument(!isLastAdminGroup, "The last system admin group '%s' cannot be deleted", groupName);
  }

  private void removeGroupMembers(DbSession dbSession, long groupId) {
    dbClient.userGroupDao().deleteMembersByGroupId(dbSession, groupId);
  }

  private void removeGroupPermissions(DbSession dbSession, long groupId) {
    dbClient.roleDao().deleteGroupRolesByGroupId(dbSession, groupId);
  }

  private void removeFromPermissionTemplates(DbSession dbSession, long groupId) {
    dbClient.permissionTemplateDao().deleteByGroup(dbSession, groupId);
  }
}
