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

import json
import os
import random
import sys

class GlobalConfig:
    """Updates global configuration values"""
    def __init__(self, api):
        self._api = api

    def update(self, key, value):
        jsonresult = self._api.GET({'command': 'updateConfiguration', 'name':key,
                      'value':value})
        if  jsonresult is  None:
           print "Failed to update configuration"
           return 1

        return 0
