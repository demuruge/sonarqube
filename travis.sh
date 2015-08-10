#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v16 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

function prepareIts {
  installTravisTools
  start_xvfb
}

function installMaven33 {
  mkdir ~/maven
  curl -sSL http://apache.mirrors.ovh.net/ftp.apache.org/dist/maven/maven-3/3.3.3/binaries/apache-maven-3.3.3-bin.tar.gz | tar zx -C ~/maven
  export M2_HOME=~/maven/apache-maven-3.3.3
  export PATH=$M2_HOME/bin:$PATH
}

installMaven33

case "$JOB" in

H2)
  mvn verify -B -e -V
  ;;

POSTGRES)
  installTravisTools

  psql -c 'create database sonar;' -U postgres

  runDatabaseCI "postgresql" "jdbc:postgresql://localhost/sonar" "postgres" ""
  ;;

MYSQL)
  installTravisTools

  mysql -e "CREATE DATABASE sonar CHARACTER SET UTF8;" -uroot
  mysql -e "CREATE USER 'sonar'@'localhost' IDENTIFIED BY 'sonar';" -uroot
  mysql -e "GRANT ALL ON sonar.* TO 'sonar'@'localhost';" -uroot
  mysql -e "FLUSH PRIVILEGES;" -uroot

  runDatabaseCI "mysql" "jdbc:mysql://localhost/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance" "sonar" "sonar"
  ;;

WEB)
  prepareIts
  /sbin/start-stop-daemon --start --quiet --pidfile /tmp/custom_xvfb_99.pid --make-pidfile --background --exec /usr/bin/Xvfb -- :99 -ac -screen 0 1280x1024x16
  wget http://selenium-release.storage.googleapis.com/2.46/selenium-server-standalone-2.46.0.jar
  nohup java -jar selenium-server-standalone-2.46.0.jar &
  sleep 3
  cd server/sonar-web && npm install && npm test
  ;;

PRANALYSIS)
  if [ -n "$SONAR_GITHUB_OAUTH" ] && [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then
    echo "Start pullrequest analysis"
    mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar -B -e -V -Dmaven.test.failure.ignore=true -Dclirr=true \
     -Dsonar.analysis.mode=incremental \
     -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
     -Dsonar.github.repository=$SONAR_GITHUB_REPOSITORY \
     -Dsonar.github.login=$SONAR_GITHUB_LOGIN \
     -Dsonar.github.oauth=$SONAR_GITHUB_OAUTH \
     -Dsonar.host.url=$SONAR_HOST_URL \
     -Dsonar.login=$SONAR_LOGIN \
     -Dsonar.password=$SONAR_PASSWD
  fi
  ;;

ITS)
  prepareIts
  mvn install -Pit,dev -DskipTests -Dsonar.runtimeVersion=DEV -Dcategory=$IT_CATEGORY -Dmaven.test.redirectTestOutputToFile=false -e
  ;;

esac
