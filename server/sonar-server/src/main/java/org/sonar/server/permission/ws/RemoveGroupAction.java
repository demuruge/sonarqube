/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.permission.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionUpdater;

public class RemoveGroupAction implements PermissionsWsAction {

  public static final String ACTION = "remove_group";
  public static final String PARAM_PERMISSION = "permission";
  public static final String PARAM_GROUP_NAME = "groupName";
  public static final String PARAM_GROUP_ID = "groupId";

  private final DbClient dbClient;
  private final PermissionWsCommons permissionWsCommons;
  private final PermissionUpdater permissionUpdater;

  public RemoveGroupAction(DbClient dbClient, PermissionWsCommons permissionWsCommons, PermissionUpdater permissionUpdater) {
    this.dbClient = dbClient;
    this.permissionWsCommons = permissionWsCommons;
    this.permissionUpdater = permissionUpdater;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setDescription("Remove permission from a group.<br /> " +
        "The group id or group name must be provided, not both.<br />" +
        "Requires 'Administer System' permission.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_PERMISSION)
      .setDescription("Permission")
      .setRequired(true)
      .setPossibleValues(GlobalPermissions.ALL);

    action.createParam(PARAM_GROUP_NAME)
      .setDescription("Group name or 'anyone' (whatever the case)")
      .setExampleValue("sonar-administrators");

    action.createParam(PARAM_GROUP_ID)
      .setDescription("Group ID")
      .setExampleValue("42");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String permission = request.mandatoryParam(PARAM_PERMISSION);
    String groupNameParam = request.param(PARAM_GROUP_NAME);
    Long groupId = request.paramAsLong(PARAM_GROUP_ID);

    DbSession dbSession = dbClient.openSession(false);
    try {
      String groupName = permissionWsCommons.searchGroupName(dbSession, groupNameParam, groupId);

      permissionUpdater.removePermission(
        new PermissionChange()
          .setPermission(permission)
          .setGroup(groupName)
        );
    } finally {
      dbClient.closeSession(dbSession);
    }

    response.noContent();
  }
}
