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
package com.cloud.user;

import java.util.List;
import java.util.Map;

import com.cloud.acl.ControlledEntity;
import com.cloud.acl.SecurityChecker.AccessType;
import com.cloud.api.commands.DeleteUserCmd;
import com.cloud.api.commands.ListAccountsCmd;
import com.cloud.api.commands.ListUsersCmd;
import com.cloud.api.commands.RegisterCmd;
import com.cloud.api.commands.UpdateAccountCmd;
import com.cloud.api.commands.UpdateUserCmd;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.Pair;

public interface AccountService {

    /**
     * Creates a new user and account, stores the password as is so encrypted passwords are recommended.
     * 
     * @param userName
     *            TODO
     * @param password
     *            TODO
     * @param firstName
     *            TODO
     * @param lastName
     *            TODO
     * @param email
     *            TODO
     * @param timezone
     *            TODO
     * @param accountName
     *            TODO
     * @param accountType
     *            TODO
     * @param domainId
     *            TODO
     * @param networkDomain
     *            TODO
     * 
     * @return the user if created successfully, null otherwise
     */
    UserAccount createUserAccount(String userName, String password, String firstName, String lastName, String email, String timezone, String accountName, short accountType, Long domainId, String networkDomain,
            Map<String, String> details);

    /**
     * Deletes a user by userId
     * 
     * @param accountId
     *            - id of the account do delete
     * 
     * @return true if delete was successful, false otherwise
     */
    boolean deleteUserAccount(long accountId);

    /**
     * Disables a user by userId
     * 
     * @param userId
     *            - the userId
     * @return UserAccount object
     */
    UserAccount disableUser(long userId);

    /**
     * Enables a user
     * 
     * @param userId
     *            - the userId
     * @return UserAccount object
     */
    UserAccount enableUser(long userId);

    /**
     * Locks a user by userId. A locked user cannot access the API, but will still have running VMs/IP addresses
     * allocated/etc.
     * 
     * @param userId
     * @return UserAccount object
     */
    UserAccount lockUser(long userId);

    /**
     * Update a user by userId
     * 
     * @param userId
     * @return UserAccount object
     */
    UserAccount updateUser(UpdateUserCmd cmd);

    /**
     * Disables an account by accountName and domainId
     * 
     * @param accountName
     *            TODO
     * @param domainId
     *            TODO
     * @param accountId
     * @param disabled
     *            account if success
     * @return true if disable was successful, false otherwise
     */
    Account disableAccount(String accountName, Long domainId, Long accountId) throws ConcurrentOperationException, ResourceUnavailableException;

    /**
     * Enables an account by accountId
     * 
     * @param accountName
     *            - the enableAccount command defining the accountId to be deleted.
     * @param domainId
     *            TODO
     * @param accountId
     * @return account object
     */
    Account enableAccount(String accountName, Long domainId, Long accountId);

    /**
     * Locks an account by accountId. A locked account cannot access the API, but will still have running VMs/IP
     * addresses
     * allocated/etc.
     * 
     * @param accountName
     *            - the LockAccount command defining the accountId to be locked.
     * @param domainId
     *            TODO
     * @param accountId
     * @return account object
     */
    Account lockAccount(String accountName, Long domainId, Long accountId);

    /**
     * Updates an account name
     * 
     * @param cmd
     *            - the parameter containing accountId
     * @return updated account object
     */

    Account updateAccount(UpdateAccountCmd cmd);

    Account getSystemAccount();

    User getSystemUser();

    User createUser(String userName, String password, String firstName, String lastName, String email, String timeZone, String accountName, Long domainId);

    boolean deleteUser(DeleteUserCmd deleteUserCmd);

    boolean isAdmin(short accountType);

    Account finalizeOwner(Account caller, String accountName, Long domainId, Long projectId);

    Pair<List<Long>, Long> finalizeAccountDomainForList(Account caller, String accountName, Long domainId, Long projectId);

    Account getActiveAccountByName(String accountName, Long domainId);

    Account getActiveAccountById(Long accountId);

    Account getAccount(Long accountId);

    User getActiveUser(long userId);

    User getUserIncludingRemoved(long userId);

    boolean isRootAdmin(short accountType);

    User getActiveUserByRegistrationToken(String registrationToken);

    void markUserRegistered(long userId);

    public String[] createApiKeyAndSecretKey(RegisterCmd cmd);

    List<? extends Account> searchForAccounts(ListAccountsCmd cmd);

    List<? extends UserAccount> searchForUsers(ListUsersCmd cmd)
            throws PermissionDeniedException;

    UserAccount getUserByApiKey(String apiKey);
    
    void checkAccess(Account account, Domain domain) throws PermissionDeniedException;

    void checkAccess(Account account, AccessType accessType, boolean sameOwner, ControlledEntity... entities) throws PermissionDeniedException;

}
