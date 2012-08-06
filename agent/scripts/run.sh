#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

#run.sh runs the agent client.
java $1 -Xms128M -Xmx384M -cp cglib-nodep-2.2.jar:trilead-ssh2-build213.jar:cloud-api.jar:cloud-core-extras.jar:cloud-utils.jar:cloud-agent.jar:cloud-console-proxy.jar:cloud-console-common.jar:freemarker.jar:log4j-1.2.15.jar:ws-commons-util-1.0.2.jar:xmlrpc-client-3.1.3.jar:cloud-core.jar:xmlrpc-common-3.1.3.jar:javaee-api-5.0-1.jar:gson-1.3.jar:commons-httpclient-3.1.jar:commons-logging-1.1.1.jar:commons-codec-1.4.jar:commons-collections-3.2.1.jar:commons-pool-1.4.jar:apache-log4j-extras-1.0.jar:libvirt-0.4.5.jar:jna.jar:.:/etc/cloud:./*:/usr/share/java/*:./conf com.cloud.agent.AgentShell
