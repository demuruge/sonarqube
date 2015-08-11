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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.component.ComponentTesting.newFileDto;
import static org.sonar.server.component.ComponentTesting.newProjectDto;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_GROUP_ID;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_GROUP_NAME;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PROJECT_KEY;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PROJECT_UUID;
import static org.sonar.server.permission.ws.RemoveGroupAction.ACTION;

@Category(DbTests.class)
public class RemoveGroupActionTest {
  UserSessionRule userSession = UserSessionRule.standalone();
  WsTester ws;
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private PermissionUpdater permissionUpdater;
  private ArgumentCaptor<PermissionChange> permissionChangeCaptor = ArgumentCaptor.forClass(PermissionChange.class);
  private DbSession dbSession = db.getSession();

  @Before
  public void setUp() {
    permissionUpdater = mock(PermissionUpdater.class);
    ws = new WsTester(new PermissionsWs(
      new RemoveGroupAction(db.getDbClient(), new PermissionWsCommons(db.getDbClient(), new ComponentFinder(db.getDbClient()), userSession), permissionUpdater)));
    userSession.login("admin").setGlobalPermissions(SYSTEM_ADMIN);
  }

  @Test
  public void call_permission_service_with_right_data() throws Exception {
    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    verify(permissionUpdater).removePermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.groupName()).isEqualTo("sonar-administrators");
    assertThat(permissionChange.permission()).isEqualTo(SYSTEM_ADMIN);
  }

  @Test
  public void remove_by_group_id() throws Exception {
    GroupDto group = insertGroup("sonar-administrators");
    commit();

    newRequest()
      .setParam(PARAM_GROUP_ID, group.getId().toString())
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    verify(permissionUpdater).removePermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.groupName()).isEqualTo("sonar-administrators");
  }

  @Test
  public void remove_with_project_uuid() throws Exception {
    insertComponent(newProjectDto("project-uuid").setKey("project-key"));
    commit();

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PROJECT_UUID, "project-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    verify(permissionUpdater).removePermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.componentKey()).isEqualTo("project-key");
  }

  @Test
  public void remove_with_project_key() throws Exception {
    insertComponent(newProjectDto("project-uuid").setKey("project-key"));
    commit();

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PROJECT_KEY, "project-key")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();

    verify(permissionUpdater).removePermission(permissionChangeCaptor.capture());
    PermissionChange permissionChange = permissionChangeCaptor.getValue();
    assertThat(permissionChange.componentKey()).isEqualTo("project-key");
  }

  @Test
  public void fail_when_project_does_not_exist() throws Exception {
    expectedException.expect(NotFoundException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PROJECT_UUID, "unknown-project-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_project_project_permission_without_project() throws Exception {
    expectedException.expect(BadRequestException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PERMISSION, UserRole.ISSUE_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_component_is_not_a_project() throws Exception {
    expectedException.expect(BadRequestException.class);
    insertComponent(newFileDto(newProjectDto("project-uuid"), "file-uuid"));
    commit();

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PROJECT_UUID, "file-uuid")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_get_request() throws Exception {
    expectedException.expect(ServerException.class);

    ws.newGetRequest(PermissionsWs.ENDPOINT, ACTION)
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_group_name_is_missing() throws Exception {
    expectedException.expect(BadRequestException.class);

    newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .execute();
  }

  @Test
  public void fail_when_permission_name_and_id_are_missing() throws Exception {
    expectedException.expect(IllegalArgumentException.class);

    newRequest()
      .setParam(PARAM_GROUP_NAME, "sonar-administrators")
      .execute();
  }

  @Test
  public void fail_when_group_id_does_not_exist() throws Exception {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Group with id '42' is not found");

    newRequest()
      .setParam(PARAM_PERMISSION, SYSTEM_ADMIN)
      .setParam(PARAM_GROUP_ID, "42")
      .execute();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest(PermissionsWs.ENDPOINT, ACTION);
  }

  private GroupDto insertGroup(String groupName) {
    return db.getDbClient().groupDao().insert(dbSession, new GroupDto().setName(groupName));
  }

  private void insertComponent(ComponentDto component) {
    db.getDbClient().componentDao().insert(dbSession, component);
  }

  private void commit() {
    dbSession.commit();
  }
}
