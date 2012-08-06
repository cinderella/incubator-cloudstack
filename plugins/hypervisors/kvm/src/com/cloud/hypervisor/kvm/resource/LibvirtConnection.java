// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.hypervisor.kvm.resource;

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

public class LibvirtConnection {
    private static final Logger s_logger = Logger
            .getLogger(LibvirtConnection.class);
    static private Connect _connection;
    static private String _hypervisorURI;

    static public Connect getConnection() throws LibvirtException {
        if (_connection == null) {
            _connection = new Connect(_hypervisorURI, false);
        } else {
            try {
                _connection.getVersion();
            } catch (LibvirtException e) {
                s_logger.debug("Connection with libvirtd is broken, due to "
                        + e.getMessage());
                _connection = new Connect(_hypervisorURI, false);
            }
        }

        return _connection;
    }

    static void initialize(String hypervisorURI) {
        _hypervisorURI = hypervisorURI;
    }
}
