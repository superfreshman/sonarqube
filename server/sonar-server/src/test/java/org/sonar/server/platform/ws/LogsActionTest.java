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
package org.sonar.server.platform.ws;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.ServerLogging;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogsActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ServerLogging serverLogging = mock(ServerLogging.class);
  private LogsAction underTest = new LogsAction(userSession, serverLogging);
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_logged_in() {
    expectedException.expect(ForbiddenException.class);

    actionTester.newRequest().execute();
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_root() {
    userSession.login();

    expectedException.expect(ForbiddenException.class);

    actionTester.newRequest().execute();
  }

  @Test
  public void get_logs() throws IOException {
    makeAuthenticatedUserRoot();

    File file = temp.newFile();
    FileUtils.write(file, "{logs}");
    when(serverLogging.getCurrentLogFile()).thenReturn(file);

    TestResponse response = actionTester.newRequest().execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{logs}");
  }

  @Test
  public void get_empty_logs_if_file_does_not_exist() throws IOException {
    makeAuthenticatedUserRoot();

    File file = temp.newFile();
    file.delete();
    when(serverLogging.getCurrentLogFile()).thenReturn(file);

    TestResponse response = actionTester.newRequest().execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("");
  }

  private void makeAuthenticatedUserRoot() {
    userSession.login().setRoot();
  }
}
