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
package com.cloud.domain;

import java.util.Date;

import com.cloud.user.OwnedBy;

/**
 * Domain defines the Domain object.
 */
public interface Domain extends OwnedBy {
    public static final long ROOT_DOMAIN = 1L;

    enum State {
        Active, Inactive
    };

    long getId();

    Long getParent();

    void setParent(Long parent);

    String getName();

    void setName(String name);

    Date getRemoved();

    String getPath();

    void setPath(String path);

    int getLevel();

    int getChildCount();

    long getNextChildSeq();

    State getState();

    void setState(State state);

    String getNetworkDomain();
}
