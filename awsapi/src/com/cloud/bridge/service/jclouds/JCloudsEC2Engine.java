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
package com.cloud.bridge.service.jclouds;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.vcloud.VCloudApiMetadata;
import org.jclouds.vcloud.VCloudClient;
import org.jclouds.vcloud.domain.Catalog;
import org.jclouds.vcloud.domain.CatalogItem;
import org.jclouds.vcloud.domain.Org;
import org.jclouds.vcloud.domain.ReferenceType;
import org.jclouds.vcloud.domain.network.FirewallService;
import org.jclouds.vcloud.domain.network.firewall.FirewallRule;

import com.cloud.bridge.service.EC2Engine;
import com.cloud.bridge.service.core.ec2.EC2Address;
import com.cloud.bridge.service.core.ec2.EC2AssociateAddress;
import com.cloud.bridge.service.core.ec2.EC2AuthorizeRevokeSecurityGroup;
import com.cloud.bridge.service.core.ec2.EC2AvailabilityZonesFilterSet;
import com.cloud.bridge.service.core.ec2.EC2CreateImage;
import com.cloud.bridge.service.core.ec2.EC2CreateImageResponse;
import com.cloud.bridge.service.core.ec2.EC2CreateKeyPair;
import com.cloud.bridge.service.core.ec2.EC2CreateVolume;
import com.cloud.bridge.service.core.ec2.EC2DeleteKeyPair;
import com.cloud.bridge.service.core.ec2.EC2DescribeAddresses;
import com.cloud.bridge.service.core.ec2.EC2DescribeAddressesResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeAvailabilityZones;
import com.cloud.bridge.service.core.ec2.EC2DescribeAvailabilityZonesResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeImageAttribute;
import com.cloud.bridge.service.core.ec2.EC2DescribeImages;
import com.cloud.bridge.service.core.ec2.EC2DescribeImagesResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeInstances;
import com.cloud.bridge.service.core.ec2.EC2DescribeInstancesResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeKeyPairs;
import com.cloud.bridge.service.core.ec2.EC2DescribeKeyPairsResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeSecurityGroups;
import com.cloud.bridge.service.core.ec2.EC2DescribeSecurityGroupsResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeSnapshots;
import com.cloud.bridge.service.core.ec2.EC2DescribeSnapshotsResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeTags;
import com.cloud.bridge.service.core.ec2.EC2DescribeTagsResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeVolumes;
import com.cloud.bridge.service.core.ec2.EC2DescribeVolumesResponse;
import com.cloud.bridge.service.core.ec2.EC2DisassociateAddress;
import com.cloud.bridge.service.core.ec2.EC2GroupFilterSet;
import com.cloud.bridge.service.core.ec2.EC2Image;
import com.cloud.bridge.service.core.ec2.EC2ImageAttributes;
import com.cloud.bridge.service.core.ec2.EC2ImportKeyPair;
import com.cloud.bridge.service.core.ec2.EC2InstanceFilterSet;
import com.cloud.bridge.service.core.ec2.EC2IpPermission;
import com.cloud.bridge.service.core.ec2.EC2ModifyImageAttribute;
import com.cloud.bridge.service.core.ec2.EC2PasswordData;
import com.cloud.bridge.service.core.ec2.EC2RebootInstances;
import com.cloud.bridge.service.core.ec2.EC2RegisterImage;
import com.cloud.bridge.service.core.ec2.EC2ReleaseAddress;
import com.cloud.bridge.service.core.ec2.EC2RunInstances;
import com.cloud.bridge.service.core.ec2.EC2RunInstancesResponse;
import com.cloud.bridge.service.core.ec2.EC2SSHKeyPair;
import com.cloud.bridge.service.core.ec2.EC2SecurityGroup;
import com.cloud.bridge.service.core.ec2.EC2Snapshot;
import com.cloud.bridge.service.core.ec2.EC2SnapshotFilterSet;
import com.cloud.bridge.service.core.ec2.EC2StartInstances;
import com.cloud.bridge.service.core.ec2.EC2StartInstancesResponse;
import com.cloud.bridge.service.core.ec2.EC2StopInstances;
import com.cloud.bridge.service.core.ec2.EC2StopInstancesResponse;
import com.cloud.bridge.service.core.ec2.EC2TagKeyValue;
import com.cloud.bridge.service.core.ec2.EC2Tags;
import com.cloud.bridge.service.core.ec2.EC2Volume;
import com.cloud.bridge.service.core.ec2.EC2VolumeFilterSet;
import com.cloud.bridge.service.exception.EC2ServiceException;
import com.cloud.bridge.service.exception.EC2ServiceException.ClientError;
import com.cloud.bridge.service.exception.EC2ServiceException.ServerError;
import com.cloud.bridge.util.ConfigurationHelper;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.inject.Module;

/**
 * EC2Engine processes the ec2 commands and calls their cloudstack analogs
 *
 */
public class JCloudsEC2Engine implements EC2Engine {
   protected final static Logger logger = Logger.getLogger(JCloudsEC2Engine.class);
   private String endpoint = null;
   private String useratorg = null;
   private String password = null;
   private ComputeServiceContext computeContext;
   private VCloudClient vcloudApi = null;

   Properties ec2properties = null;

   public JCloudsEC2Engine(){
      loadConfigValues();
   }

   /**
    * Which management server to we talk to?
    * Load a mapping form Amazon values for 'instanceType' to cloud defined
    * diskOfferingId and serviceOfferingId.
    *
    * @throws java.io.IOException
    */
   private void loadConfigValues() {
      ConfigurationHelper.preSetConfigPath(System.getProperty("user.home", "/etc") + "/.cinderella");
      File propertiesFile = ConfigurationHelper.findConfigurationFile("ec2-service.properties");
      ec2properties = new Properties();

      if (null != propertiesFile) {
         logger.info("Use EC2 properties file: " + propertiesFile.getAbsolutePath());
         try {
            ec2properties.load(new FileInputStream(propertiesFile));
         } catch (FileNotFoundException e) {
            logger.warn("Unable to open properties file: " + propertiesFile.getAbsolutePath(), e);
         } catch (IOException e) {
            logger.warn("Unable to read properties file: " + propertiesFile.getAbsolutePath(), e);
         }
         endpoint = ec2properties.getProperty("endpoint");
         useratorg = ec2properties.getProperty("useratorg");
         password = ec2properties.getProperty("password");

      } else logger.error( "ec2-service.properties not found" );
   }

   /**
    * Helper function to manage the api connection
    *
    * @return
    */
   private VCloudClient getApi() {
      if (vcloudApi == null) {
         Properties overrides = new Properties();
         overrides.setProperty(Constants.PROPERTY_TRUST_ALL_CERTS, "true");
         overrides.setProperty(Constants.PROPERTY_RELAX_HOSTNAME, "true");
         computeContext = ContextBuilder.newBuilder("vcloud")
                                        .endpoint(endpoint)
                                        .credentials(useratorg, password)
                                        .modules(ImmutableSet.<Module>builder()
                                          .add(new SLF4JLoggingModule())
                                          .add(new EnterpriseConfigurationModule()).build())
                                        .overrides(overrides)
                                        .buildView(ComputeServiceContext.class);

         computeContext.utils().injector().injectMembers(this);
         vcloudApi = computeContext.unwrap(VCloudApiMetadata.CONTEXT_TOKEN).getApi();
      }
      return ( vcloudApi != null ? vcloudApi : null );
   }


   /**
    * Verifies account can access CloudStack
    *
    * @param accessKey
    * @param secretKey
    * @return
    * @throws com.cloud.bridge.service.exception.EC2ServiceException
    */
/*
   public boolean validateAccount( String accessKey, String secretKey ) throws EC2ServiceException {
      String oldApiKey = null;
      String oldSecretKey = null;

      if (accessKey == null || secretKey == null) {
         return false;
      }

      // okay, instead of using the getApi() nonsense for validate, we are going to manage vcloudApi
      if (vcloudApi == null) {
         vcloudApi = new CloudStackApi(managementServer, cloudAPIPort, false);
      }

      try {
         oldApiKey = vcloudApi.getApiKey();
         oldSecretKey = vcloudApi.getSecretKey();
      } catch(Exception e) {
         // we really don't care, and expect this
      }
      try {
         vcloudApi.setApiKey(accessKey);
         vcloudApi.setSecretKey(secretKey);
         List<CloudStackAccount> accts = vcloudApi.listAccounts(null, null, null, null, null, null, null, null);
         if (oldApiKey != null && oldSecretKey != null) {
            vcloudApi.setApiKey(oldApiKey);
            vcloudApi.setSecretKey(oldSecretKey);
         }
         if (accts == null) {
            return false;
         }
         return true;
      } catch(Exception e) {
         logger.error("Validate account failed!");
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
   }
*/

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#createSecurityGroup(java.lang.String, java.lang.String)
    */
   @Override
   public Boolean createSecurityGroup(String groupName, String groupDesc) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         CloudStackSecurityGroup grp = getApi().createSecurityGroup(groupName, null, groupDesc, null);
         if (grp != null && grp.getId() != null) {
            return true;
         }
         return false;
      } catch( Exception e ) {
         logger.error( "EC2 CreateSecurityGroup - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#deleteSecurityGroup(java.lang.String)
    */
   @Override
   public boolean deleteSecurityGroup(String groupName) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*

      try {
         CloudStackInfoResponse resp = getApi().deleteSecurityGroup(null, null, null, groupName);
         if (resp != null) {
            return resp.getSuccess();
         }
         return false;
      } catch( Exception e ) {
         logger.error( "EC2 DeleteSecurityGroup - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#describeSecurityGroups(com.cloud.bridge.service.core.ec2.EC2DescribeSecurityGroups)
    */
   @Override
   public EC2DescribeSecurityGroupsResponse describeSecurityGroups(EC2DescribeSecurityGroups request)
   {
      try {
         EC2DescribeSecurityGroupsResponse response = listSecurityGroups( request.getGroupSet());
         EC2GroupFilterSet gfs = request.getFilterSet();

         if ( null == gfs )
            return response;
         else return gfs.evaluate( response );
      } catch( Exception e ) {
         logger.error( "EC2 DescribeSecurityGroups - ", e);
         throw new EC2ServiceException(ServerError.InternalError, "An unexpected error occurred.");
      }
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#revokeSecurityGroup(com.cloud.bridge.service.core.ec2.EC2AuthorizeRevokeSecurityGroup)
    */
   @Override
   public boolean revokeSecurityGroup( EC2AuthorizeRevokeSecurityGroup request )
   {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*

      if (null == request.getName()) throw new EC2ServiceException(ServerError.InternalError, "Name is a required parameter");
      try {
         String[] groupSet = new String[1];
         groupSet[0] = request.getName();
         String ruleId = null;

         EC2IpPermission[] items = request.getIpPermissionSet();

         EC2DescribeSecurityGroupsResponse response = listSecurityGroups( groupSet );
         EC2SecurityGroup[] groups = response.getGroupSet();

         for (EC2SecurityGroup group : groups) {
            EC2IpPermission[] perms = group.getIpPermissionSet();
            for (EC2IpPermission perm : perms) {
               ruleId = doesRuleMatch( items[0], perm );
               if (ruleId != null) break;
            }
         }

         if (null == ruleId)
            throw new EC2ServiceException(ClientError.InvalidGroup_NotFound, "Cannot find matching ruleid.");

         CloudStackInfoResponse resp = getApi().revokeSecurityGroupIngress(ruleId);
         if (resp != null && resp.getId() != null) {
            return resp.getSuccess();
         }
         return false;
      } catch( Exception e ) {
         logger.error( "EC2 revokeSecurityGroupIngress" + " - " + e.getMessage());
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#authorizeSecurityGroup(com.cloud.bridge.service.core.ec2.EC2AuthorizeRevokeSecurityGroup)
    */
   @Override
   public boolean authorizeSecurityGroup(EC2AuthorizeRevokeSecurityGroup request )
   {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*

      if (null == request.getName()) throw new EC2ServiceException(ServerError.InternalError, "Name is a required parameter");

      EC2IpPermission[] items = request.getIpPermissionSet();

      try {
         for (EC2IpPermission ipPerm : items) {
            EC2SecurityGroup[] groups = ipPerm.getUserSet();

            Multimap<String, String> secGroupList = new ArrayMultimap<String, String>();
            for (EC2SecurityGroup group : groups) {
               CloudStackKeyValue pair = new CloudStackKeyValue();
               pair.setKeyValue(group.getAccount(), group.getName());
               secGroupList.add(pair);
            }
            CloudStackSecurityGroupIngress resp = null;
            if (ipPerm.getProtocol().equalsIgnoreCase("icmp")) {
               resp = getApi().authorizeSecurityGroupIngress(null, constructList(ipPerm.getIpRangeSet()), null, null,
               ipPerm.getIcmpCode(), ipPerm.getIcmpType(), ipPerm.getProtocol(), null,
               request.getName(), null, secGroupList);
            } else {
               resp = getApi().authorizeSecurityGroupIngress(null, constructList(ipPerm.getIpRangeSet()), null,
               ipPerm.getToPort().longValue(), null, null, ipPerm.getProtocol(), null, request.getName(),
               ipPerm.getFromPort().longValue(), secGroupList);
            }
            if (resp != null && resp.getRuleId() != null) {
               return true;
            }
            return false;
         }
      } catch(Exception e) {
         logger.error( "EC2 AuthorizeSecurityGroupIngress - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
      return true;
*/
   }

   /**
    * Does the permission from the request (left) match the permission from the cloudStack query (right).
    * If the cloudStack rule matches then we return its ruleId.
    *
    * @param permLeft
    * @param permRight
    * @return ruleId of the cloudstack rule
    */
   private String doesRuleMatch(EC2IpPermission permLeft, EC2IpPermission permRight)
   {
      int matches = 0;

      if (null != permLeft.getIcmpType() && null != permLeft.getIcmpCode()) {
         if (null == permRight.getIcmpType() || null == permRight.getIcmpCode()) return null;

         if (!permLeft.getIcmpType().equalsIgnoreCase( permRight.getIcmpType())) return null;
         if (!permLeft.getIcmpCode().equalsIgnoreCase( permRight.getIcmpCode())) return null;
         matches++;
      }

      // -> "Valid Values for EC2 security groups: tcp | udp | icmp or the corresponding protocol number (6 | 17 | 1)."
      if (null != permLeft.getProtocol()) {
         if (null == permRight.getProtocol()) return null;

         String protocol = permLeft.getProtocol();
         if (protocol.equals( "6"  )) protocol = "tcp";
         else if (protocol.equals( "17" )) protocol = "udp";
         else if (protocol.equals( "1"  )) protocol = "icmp";

         if (!protocol.equalsIgnoreCase( permRight.getProtocol())) return null;
         matches++;
      }


      if (null != permLeft.getCIDR()) {
         if (null == permRight.getCIDR()) return null;

         if (!permLeft.getCIDR().equalsIgnoreCase( permRight.getCIDR())) return null;
         matches++;
      }

      // -> is the port(s) from the request (left) a match of the rule's port(s)
      if (0 != permLeft.getFromPort()) {
         // -> -1 means all ports match
         if (-1 != permLeft.getFromPort()) {
            if (permLeft.getFromPort().compareTo(permRight.getFromPort()) != 0 ||
            permLeft.getToPort().compareTo(permRight.getToPort()) != 0)
               return null;
         }
         matches++;
      }


      // -> was permLeft set up properly with at least one property to match?
      if ( 0 == matches )
         return null;
      else return permRight.getRuleId();
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#handleRequest(com.cloud.bridge.service.core.ec2.EC2DescribeSnapshots)
    */
   @Override
   public EC2DescribeSnapshotsResponse handleRequest( EC2DescribeSnapshots request )
   {
      EC2DescribeVolumesResponse volumes = new EC2DescribeVolumesResponse();
      EC2SnapshotFilterSet sfs = request.getFilterSet();
      EC2TagKeyValue[] tagKeyValueSet = request.getResourceTagSet();

      try {
         // -> query to get the volume size for each snapshot
//         EC2DescribeSnapshotsResponse response = listSnapshots( request.getSnapshotSet(),
//         getResourceTags(tagKeyValueSet));
//         if (response == null) {
//            return new EC2DescribeSnapshotsResponse();
//         }
//         EC2Snapshot[] snapshots = response.getSnapshotSet();
//         for (EC2Snapshot snap : snapshots) {
//            volumes = listVolumes(snap.getVolumeId(), null, volumes, null);
//            EC2Volume[] volSet = volumes.getVolumeSet();
//            if (0 < volSet.length) snap.setVolumeSize(volSet[0].getSize());
//            volumes.reset();
//         }
//
//         if ( null == sfs )
//            return response;
//         else return sfs.evaluate( response );
           return null;
      } catch( EC2ServiceException error ) {
         logger.error( "EC2 DescribeSnapshots - ", error);
         throw error;

      } catch( Exception e ) {
         logger.error( "EC2 DescribeSnapshots - ", e);
         throw new EC2ServiceException(ServerError.InternalError, "An unexpected error occurred.");
      }
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#createSnapshot(java.lang.String)
    */
   @Override
   public EC2Snapshot createSnapshot( String volumeId ) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*

      try {

         CloudStackSnapshot snap = getApi().createSnapshot(volumeId, null, null, null);
         if (snap == null) {
            throw new EC2ServiceException(ServerError.InternalError, "Unable to create snapshot!");
         }
         EC2Snapshot ec2Snapshot = new EC2Snapshot();

         ec2Snapshot.setId(snap.getId());
         ec2Snapshot.setName(snap.getName());
         ec2Snapshot.setType(snap.getSnapshotType());
         ec2Snapshot.setAccountName(snap.getAccountName());
         ec2Snapshot.setDomainId(snap.getDomainId());
         ec2Snapshot.setCreated(snap.getCreated());
         ec2Snapshot.setVolumeId(snap.getVolumeId());

         List<CloudStackVolume> vols = getApi().listVolumes(null, null, null, snap.getVolumeId(), null, null, null, null, null, null, null, null);

         if(vols.size() > 0) {
            assert(vols.get(0).getSize() != null);
            Long sizeInGB = vols.get(0).getSize().longValue()/1073741824;
            ec2Snapshot.setVolumeSize(sizeInGB);
         }

         return ec2Snapshot;
      } catch( Exception e ) {
         logger.error( "EC2 CreateSnapshot - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#deleteSnapshot(java.lang.String)
    */
   @Override
   public boolean deleteSnapshot(String snapshotId) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*

      try {

         CloudStackInfoResponse resp = getApi().deleteSnapshot(snapshotId);
         if(resp != null) {
            return resp.getSuccess();
         }

         return false;
      } catch(Exception e) {
         logger.error( "EC2 DeleteSnapshot - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
*/
   }


   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#modifyImageAttribute(com.cloud.bridge.service.core.ec2.EC2Image)
    */
   @Override
   public boolean modifyImageAttribute( EC2Image request )
   {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*

      // TODO: This is incomplete
      EC2DescribeImagesResponse images = new EC2DescribeImagesResponse();

      try {
         images = listTemplates( request.getId(), images );
         EC2Image[] imageSet = images.getImageSet();

         CloudStackTemplate resp = getApi().updateTemplate(request.getId(), null, request.getDescription(), null, imageSet[0].getName(), null, null);
         if (resp != null) {
            return true;
         }
         return false;
      } catch( Exception e ) {
         logger.error( "EC2 ModifyImage - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
*/
   }


   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#modifyImageAttribute(com.cloud.bridge.service.core.ec2.EC2ModifyImageAttribute)
    */
   @Override
   public boolean modifyImageAttribute( EC2ModifyImageAttribute request )
   {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         if(request.getAttribute().equals(ImageAttribute.launchPermission)){

            String accounts = "";
            Boolean isPublic = null;
            EC2ModifyImageAttribute.Operation operation = request.getLaunchPermOperation();

            List<String> accountOrGroupList = request.getLaunchPermissionAccountsList();
            if(accountOrGroupList != null && !accountOrGroupList.isEmpty()){
               boolean first = true;
               for(String accountOrGroup : accountOrGroupList){
                  if("all".equalsIgnoreCase(accountOrGroup)){
                     if(operation.equals(EC2ModifyImageAttribute.Operation.add)){
                        isPublic = true;
                     }else{
                        isPublic = false;
                     }
                  }else{
                     if(!first){
                        accounts = accounts + ",";
                     }
                     accounts = accounts + accountOrGroup;
                     first = false;
                  }
               }
            }
            CloudStackInfoResponse resp = getApi().updateTemplatePermissions(request.getImageId(), accounts, null, null, isPublic, operation.toString());
            return resp.getSuccess();
         }else if(request.getAttribute().equals(ImageAttribute.description)){
            CloudStackTemplate resp = getApi().updateTemplate(request.getImageId(), null, request.getDescription(), null, null, null, null);
            if (resp != null) {
               return true;
            }
            return false;
         }

      } catch (Exception e) {
         logger.error( "EC2 modifyImageAttribute - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }

      return false;


*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#describeImageAttribute(com.cloud.bridge.service.core.ec2.EC2DescribeImageAttribute)
    */
   @Override
   public EC2ImageAttributes describeImageAttribute(EC2DescribeImageAttribute request) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*

      EC2ImageAttributes imageAtts = new EC2ImageAttributes();

      try {
         imageAtts.setImageId(request.getImageId());
         if(request.getAttribute().equals(ImageAttribute.launchPermission)){
            CloudStackTemplatePermission tempPerm = getApi().listTemplatePermissions(request.getImageId(), null, null);
            if(tempPerm != null){
               imageAtts.setDomainId(tempPerm.getDomainId());

               List<String> accntList = tempPerm.getAccounts();
               imageAtts.setAccountNamesWithLaunchPermission(accntList);

               imageAtts.setIsPublic(tempPerm.getIsPublic());
            }
         }else if(request.getAttribute().equals(ImageAttribute.description)){
            EC2DescribeImagesResponse descriptionResp = new EC2DescribeImagesResponse();
            listTemplates(request.getImageId(), descriptionResp);
            if(descriptionResp.getImageSet() != null){
               EC2Image[] images = descriptionResp.getImageSet();
               imageAtts.setDescription(images[0].getDescription());
            }
         }

      } catch (Exception e) {
         logger.error( "EC2 describeImageAttribute - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }

      return imageAtts;
*/
   }



   // handlers
   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#getPasswordData(java.lang.String)
    */
   @Override
   public EC2PasswordData getPasswordData(String instanceId) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         CloudStackPasswordData resp = getApi().getVMPassword(instanceId);
         EC2PasswordData passwdData = new EC2PasswordData();
         if (resp != null) {
            passwdData.setInstanceId(instanceId);
            passwdData.setEncryptedPassword(resp.getEncryptedpassword());
         }
         return passwdData;
      } catch(Exception e) {
         logger.error("EC2 GetPasswordData - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
*/
   }
   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#describeKeyPairs(com.cloud.bridge.service.core.ec2.EC2DescribeKeyPairs)
    */
   @Override
   public EC2DescribeKeyPairsResponse describeKeyPairs( EC2DescribeKeyPairs request ) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         EC2KeyPairFilterSet filterSet = request.getKeyFilterSet();
         String[] keyNames = request.getKeyNames();
         List<CloudStackKeyPair> keyPairs = getApi().listSSHKeyPairs(null, null, null);
         List<EC2SSHKeyPair> keyPairsList = new ArrayList<EC2SSHKeyPair>();

         if (keyPairs != null) {
            // Let's trim the list of keypairs to only the ones listed in keyNames
            List<CloudStackKeyPair> matchedKeyPairs = new ArrayList<CloudStackKeyPair>();
            if (keyNames != null && keyNames.length > 0) {
               for (CloudStackKeyPair keyPair : keyPairs) {
                  boolean matched = false;
                  for (String keyName : keyNames) {
                     if (keyPair.getName().equalsIgnoreCase(keyName)) {
                        matched = true;
                        break;
                     }
                  }
                  if (matched) {
                     matchedKeyPairs.add(keyPair);
                  }
               }
               if (matchedKeyPairs.isEmpty()) {
                  throw new EC2ServiceException(ServerError.InternalError, "No matching keypairs found");
               }
            }else{
               matchedKeyPairs = keyPairs;
            }


            // this should be reworked... converting from CloudStackKeyPairResponse to EC2SSHKeyPair is dumb
            for (CloudStackKeyPair respKeyPair: matchedKeyPairs) {
               EC2SSHKeyPair ec2KeyPair = new EC2SSHKeyPair();
               ec2KeyPair.setFingerprint(respKeyPair.getFingerprint());
               ec2KeyPair.setKeyName(respKeyPair.getName());
               ec2KeyPair.setPrivateKey(respKeyPair.getPrivatekey());
               keyPairsList.add(ec2KeyPair);
            }
         }
         return filterSet.evaluate(keyPairsList);
      } catch(Exception e) {
         logger.error("EC2 DescribeKeyPairs - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#deleteKeyPair(com.cloud.bridge.service.core.ec2.EC2DeleteKeyPair)
    */
   @Override
   public boolean deleteKeyPair( EC2DeleteKeyPair request ) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         CloudStackInfoResponse resp = getApi().deleteSSHKeyPair(request.getKeyName(), null, null);
         if (resp == null) {
            throw new Exception("Ivalid CloudStack API response");
         }

         return resp.getSuccess();
      } catch(Exception e) {
         logger.error("EC2 DeleteKeyPair - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#createKeyPair(com.cloud.bridge.service.core.ec2.EC2CreateKeyPair)
    */
   @Override
   public EC2SSHKeyPair createKeyPair(EC2CreateKeyPair request) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         CloudStackKeyPair resp = getApi().createSSHKeyPair(request.getKeyName(), null, null);
         if (resp == null) {
            throw new Exception("Ivalid CloudStack API response");
         }

         EC2SSHKeyPair response = new EC2SSHKeyPair();
         response.setFingerprint(resp.getFingerprint());
         response.setKeyName(resp.getName());
         response.setPrivateKey(resp.getPrivatekey());

         return response;
      } catch (Exception e) {
         logger.error("EC2 CreateKeyPair - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#importKeyPair(com.cloud.bridge.service.core.ec2.EC2ImportKeyPair)
    */
   @Override
   public EC2SSHKeyPair importKeyPair( EC2ImportKeyPair request ) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         CloudStackKeyPair resp = getApi().registerSSHKeyPair(request.getKeyName(), request.getPublicKeyMaterial());
         if (resp == null) {
            throw new Exception("Ivalid CloudStack API response");
         }

         EC2SSHKeyPair response = new EC2SSHKeyPair();
         response.setFingerprint(resp.getFingerprint());
         response.setKeyName(resp.getName());
         response.setPrivateKey(resp.getPrivatekey());

         return response;
      } catch (Exception e) {
         logger.error("EC2 ImportKeyPair - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#describeAddresses(com.cloud.bridge.service.core.ec2.EC2DescribeAddresses)
    */
   @Override
   public EC2DescribeAddressesResponse describeAddresses( EC2DescribeAddresses request ) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         List<CloudStackIpAddress> addrList = getApi().listPublicIpAddresses(null, null, null, null, null, null, null, null, null);

         EC2AddressFilterSet filterSet = request.getFilterSet();
         List<EC2Address> addressList = new ArrayList<EC2Address>();
         if (addrList != null && addrList.size() > 0) {
            for (CloudStackIpAddress addr: addrList) {
               // remember, if no filters are set, request.inPublicIpSet always returns true
               if (request.inPublicIpSet(addr.getIpAddress())) {
                  EC2Address ec2Address = new EC2Address();
                  ec2Address.setIpAddress(addr.getIpAddress());
                  if (addr.getVirtualMachineId() != null)
                     ec2Address.setAssociatedInstanceId(addr.getVirtualMachineId().toString());
                  addressList.add(ec2Address);
               }
            }
         }

         return filterSet.evaluate(addressList);
      } catch(Exception e) {
         logger.error("EC2 DescribeAddresses - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#releaseAddress(com.cloud.bridge.service.core.ec2.EC2ReleaseAddress)
    */
   @Override
   public boolean releaseAddress(EC2ReleaseAddress request) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         CloudStackIpAddress cloudIp = getApi().listPublicIpAddresses(null, null, null, null, null, request.getPublicIp(), null, null, null).get(0);
         CloudStackInfoResponse resp = getApi().disassociateIpAddress(cloudIp.getId());
         if (resp != null) {
            return resp.getSuccess();
         }
      } catch(Exception e) {
         logger.error("EC2 ReleaseAddress - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
      return false;
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#associateAddress(com.cloud.bridge.service.core.ec2.EC2AssociateAddress)
    */
   @Override
   public boolean associateAddress( EC2AssociateAddress request ) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         CloudStackIpAddress cloudIp = getApi().listPublicIpAddresses(null, null, null, null, null, request.getPublicIp(), null, null, null).get(0);
         CloudStackUserVm cloudVm = getApi().listVirtualMachines(null, null, true, null, null, null, null, request.getInstanceId(), null, null, null, null, null, null, null, null, null).get(0);

         CloudStackInfoResponse resp = getApi().enableStaticNat(cloudIp.getId(), cloudVm.getId());
         if (resp != null) {
            return resp.getSuccess();
         }
      } catch(Exception e) {
         logger.error( "EC2 AssociateAddress - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
      return false;
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#disassociateAddress(com.cloud.bridge.service.core.ec2.EC2DisassociateAddress)
    */
   @Override
   public boolean disassociateAddress( EC2DisassociateAddress request ) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         CloudStackIpAddress cloudIp = getApi().listPublicIpAddresses(null, null, null, null, null, request.getPublicIp(), null, null, null).get(0);
         CloudStackInfoResponse resp = getApi().disableStaticNat(cloudIp.getId());
         if (resp != null) {
            return resp.getSuccess();
         }
      } catch(Exception e) {
         logger.error( "EC2 DisassociateAddress - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
      return false;
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#allocateAddress()
    */
   @Override
   public EC2Address allocateAddress()
   {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         EC2Address ec2Address = new EC2Address();
         // this gets our networkId
         CloudStackAccount caller = getCurrentAccount();

         CloudStackZone zone = findZone();
         CloudStackNetwork net = findNetwork(zone);
//			CloudStackIpAddress resp = getApi().associateIpAddress(null, null, null, "0036952d-48df-4422-9fd0-94b0885e18cb");
         CloudStackIpAddress resp = getApi().associateIpAddress(zone.getId(), caller.getName(), caller.getDomainId(), net != null ? net.getId():null);
         ec2Address.setAssociatedInstanceId(resp.getId());

         if (resp.getIpAddress() == null) {
            List<CloudStackIpAddress> addrList = getApi().listPublicIpAddresses(null, null, null, null, null, null, null, null, null);
            if (addrList != null && addrList.size() > 0) {
               for (CloudStackIpAddress addr: addrList) {
                  if (addr.getId().equalsIgnoreCase(resp.getId())) {
                     ec2Address.setIpAddress(addr.getIpAddress());
                  }
               }
            }
         } else {
            ec2Address.setIpAddress(resp.getIpAddress());
         }

         return ec2Address;
      } catch(Exception e) {
         logger.error( "EC2 AllocateAddress - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#describeImages(com.cloud.bridge.service.core.ec2.EC2DescribeImages)
    */
   @Override
   public EC2DescribeImagesResponse describeImages(EC2DescribeImages request)
   {
//      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
      EC2DescribeImagesResponse images = new EC2DescribeImagesResponse();

      try {
         String[] templateIds = request.getImageSet();

         if ( 0 == templateIds.length ) {
            return listTemplates(null, images);
         }
         for (String s : templateIds) {
            images = listTemplates(s, images);
         }
         return images;

      } catch( Exception e ) {
         logger.error( "EC2 DescribeImages - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#createImage(com.cloud.bridge.service.core.ec2.EC2CreateImage)
    */
   @Override
   public EC2CreateImageResponse createImage(EC2CreateImage request)
   {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      EC2CreateImageResponse response = null;
      boolean needsRestart = false;
      String volumeId      = null;

      try {
         // [A] Creating a template from a VM volume should be from the ROOT volume
         //     Also for this to work the VM must be in a Stopped state so we 'reboot' it if its not
         EC2DescribeVolumesResponse volumes = new EC2DescribeVolumesResponse();
         volumes = listVolumes( null, request.getInstanceId(), volumes, null );
         EC2Volume[] volSet = volumes.getVolumeSet();
         for (EC2Volume vol : volSet) {
            if (vol.getType().equalsIgnoreCase( "ROOT" )) {
               String vmState = vol.getVMState();
               if (vmState.equalsIgnoreCase( "running" ) || vmState.equalsIgnoreCase( "starting" )) {
                  needsRestart = true;
                  if (!stopVirtualMachine( request.getInstanceId() ))
                     throw new EC2ServiceException(ClientError.IncorrectState, "CreateImage - instance must be in a stopped state");
               }
               volumeId = vol.getId();
               break;
            }
         }

         // [B] The parameters must be in sorted order for proper signature generation
         EC2DescribeInstancesResponse instances = new EC2DescribeInstancesResponse();
         instances = lookupInstances( request.getInstanceId(), instances, null );
         EC2Instance[] instanceSet = instances.getInstanceSet();
         String templateId = instanceSet[0].getTemplateId();

         EC2DescribeImagesResponse images = new EC2DescribeImagesResponse();
         images = listTemplates( templateId, images );
         EC2Image[] imageSet = images.getImageSet();
         String osTypeId = imageSet[0].getOsTypeId();

         CloudStackTemplate resp = getApi().createTemplate((request.getDescription() == null ? "" : request.getDescription()), request.getName(),
         osTypeId, null, null, null, null, null, null, volumeId);
         if (resp == null || resp.getId() == null) {
            throw new EC2ServiceException(ServerError.InternalError, "An upexpected error occurred.");
         }

         //if template was created succesfully, create the new image response
         response = new EC2CreateImageResponse();
         response.setId(resp.getId());

         // [C] If we stopped the virtual machine now we need to restart it
         if (needsRestart) {
            if (!startVirtualMachine( request.getInstanceId() ))
               throw new EC2ServiceException(ServerError.InternalError,
               "CreateImage - restarting instance " + request.getInstanceId() + " failed");
         }
         return response;

      } catch( Exception e ) {
         logger.error( "EC2 CreateImage - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#registerImage(com.cloud.bridge.service.core.ec2.EC2RegisterImage)
    */
   @Override
   public EC2CreateImageResponse registerImage(EC2RegisterImage request)
   {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         CloudStackAccount caller = getCurrentAccount();
         if (null == request.getName())
            throw new EC2ServiceException(ClientError.Unsupported, "Missing parameter - name");

         List<CloudStackTemplate> templates = getApi().registerTemplate((request.getDescription() == null ? request.getName() : request.getDescription()),
         request.getFormat(), request.getHypervisor(), request.getName(), toOSTypeId(request.getOsTypeName()), request.getLocation(),
         toZoneId(request.getZoneName(), null), null, null, null, null, null, null, null, null, null);
         if (templates != null) {
            // technically we will only ever register a single template...
            for (CloudStackTemplate template : templates) {
               if (template != null && template.getId() != null) {
                  EC2CreateImageResponse image = new EC2CreateImageResponse();
                  image.setId(template.getId().toString());
                  return image;
               }
            }
         }
         return null;
      } catch( Exception e ) {
         logger.error( "EC2 RegisterImage - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#deregisterImage(com.cloud.bridge.service.core.ec2.EC2Image)
    */
   @Override
   public boolean deregisterImage( EC2Image image )
   {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         CloudStackInfoResponse resp = getApi().deleteTemplate(image.getId(), null);
         return resp.getSuccess();
      } catch( Exception e ) {
         logger.error( "EC2 DeregisterImage - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#describeInstances(com.cloud.bridge.service.core.ec2.EC2DescribeInstances)
    */
   @Override
   public EC2DescribeInstancesResponse describeInstances(EC2DescribeInstances request ) {
      try {
         EC2TagKeyValue[] tagKeyValueSet = request.getResourceTagSet();
         return listVirtualMachines( request.getInstancesSet(), request.getFilterSet(),
         getResourceTags(tagKeyValueSet));
      } catch( Exception e ) {
         logger.error( "EC2 DescribeInstances - " ,e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#handleRequest(com.cloud.bridge.service.core.ec2.EC2DescribeAvailabilityZones)
    */
   @Override
   public EC2DescribeAvailabilityZonesResponse handleRequest(EC2DescribeAvailabilityZones request) {
      try {
         EC2DescribeAvailabilityZonesResponse availableZones = listZones(request.getZoneSet(), null);
         EC2AvailabilityZonesFilterSet azfs = request.getFilterSet();
         if ( null == azfs )
            return availableZones;
         else {
            List<String> matchedAvailableZones = azfs.evaluate(availableZones);
            if (matchedAvailableZones.isEmpty())
               return new EC2DescribeAvailabilityZonesResponse();
            return listZones(matchedAvailableZones.toArray(new String[0]), null);
         }
      } catch( EC2ServiceException error ) {
         logger.error( "EC2 DescribeAvailabilityZones - ", error);
         throw error;

      } catch( Exception e ) {
         logger.error( "EC2 DescribeAvailabilityZones - " ,e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#handleRequest(com.cloud.bridge.service.core.ec2.EC2DescribeVolumes)
    */
   @Override
   public EC2DescribeVolumesResponse handleRequest( EC2DescribeVolumes request ) {
      EC2DescribeVolumesResponse volumes = new EC2DescribeVolumesResponse();
      EC2VolumeFilterSet vfs = request.getFilterSet();
      EC2TagKeyValue[] tagKeyValueSet = request.getResourceTagSet();
      try {
         String[] volumeIds = request.getVolumeSet();
         if ( 0 == volumeIds.length ){
            volumes = listVolumes( null, null, volumes, getResourceTags(tagKeyValueSet) );
         } else {
            for (String s : volumeIds)
               volumes = listVolumes(s, null, volumes, getResourceTags(tagKeyValueSet) );
         }

         if ( null == vfs )
            return volumes;
         else return vfs.evaluate( volumes );
      }  catch( Exception e ) {
         logger.error( "EC2 DescribeVolumes - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#attachVolume(com.cloud.bridge.service.core.ec2.EC2Volume)
    */
   @Override
   public EC2Volume attachVolume( EC2Volume request ) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         request.setDeviceId(mapDeviceToCloudDeviceId(request.getDevice()));
         EC2Volume resp = new EC2Volume();

         CloudStackVolume vol = getApi().attachVolume(request.getId(), request.getInstanceId(), request.getDeviceId());
         if(vol != null) {
            resp.setAttached(vol.getAttached());
            resp.setCreated(vol.getCreated());
            resp.setDevice(request.getDevice());
            resp.setDeviceId(vol.getDeviceId());
            resp.setHypervisor(vol.getHypervisor());
            resp.setId(vol.getId());
            resp.setInstanceId(vol.getVirtualMachineId());
            resp.setSize(vol.getSize());
            resp.setSnapshotId(vol.getSnapshotId());
            resp.setState(vol.getState());
            resp.setType(vol.getVolumeType());
            resp.setVMState(vol.getVirtualMachineState());
            resp.setZoneName(vol.getZoneName());
            return resp;
         }
         throw new EC2ServiceException( ServerError.InternalError, "An unexpected error occurred." );
      } catch( Exception e ) {
         logger.error( "EC2 AttachVolume 2 - ", e);
         throw new EC2ServiceException( ServerError.InternalError, e.getMessage() != null ? e.getMessage() : e.toString());
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#detachVolume(com.cloud.bridge.service.core.ec2.EC2Volume)
    */
   @Override
   public EC2Volume detachVolume(EC2Volume request) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         CloudStackVolume vol = getApi().detachVolume(null, request.getId(), null);
         EC2Volume resp = new EC2Volume();

         if(vol != null) {
            resp.setAttached(vol.getAttached());
            resp.setCreated(vol.getCreated());
            resp.setDevice(request.getDevice());
            resp.setDeviceId(vol.getDeviceId());
            resp.setHypervisor(vol.getHypervisor());
            resp.setId(vol.getId());
            resp.setInstanceId(vol.getVirtualMachineId());
            resp.setSize(vol.getSize());
            resp.setSnapshotId(vol.getSnapshotId());
            resp.setState(vol.getState());
            resp.setType(vol.getVolumeType());
            resp.setVMState(vol.getVirtualMachineState());
            resp.setZoneName(vol.getZoneName());
            return resp;
         }

         throw new EC2ServiceException( ServerError.InternalError, "An unexpected error occurred." );
      } catch( Exception e ) {
         logger.error( "EC2 DetachVolume - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
*/
      }

      /* (non-Javadoc)
       * @see com.cloud.bridge.service.core.ec2.EC2Engine1#createVolume(com.cloud.bridge.service.core.ec2.EC2CreateVolume)
       */
   @Override
   public EC2Volume createVolume( EC2CreateVolume request ) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {

         CloudStackAccount caller = getCurrentAccount();
         // -> put either snapshotid or diskofferingid on the request
         String snapshotId = request.getSnapshotId();
         Long size = request.getSize();
         String diskOfferingId = null;

         if (snapshotId == null) {
            List<CloudStackDiskOffering> disks = getApi().listDiskOfferings(null, null, null, null);
            for (CloudStackDiskOffering offer : disks) {
               if (offer.isCustomized()) {
                  diskOfferingId = offer.getId();
               }
            }
            if (diskOfferingId == null) throw new EC2ServiceException(ServerError.InternalError, "No Customize Disk Offering Found");
         }

//			// -> no volume name is given in the Amazon request but is required in the cloud API
         CloudStackVolume vol = getApi().createVolume(UUID.randomUUID().toString(), null, diskOfferingId, null, size, snapshotId, toZoneId(request.getZoneName(), null));
         if (vol != null) {
            EC2Volume resp = new EC2Volume();
            resp.setAttached(vol.getAttached());
            resp.setCreated(vol.getCreated());
//				resp.setDevice();
            resp.setDeviceId(vol.getDeviceId());
            resp.setHypervisor(vol.getHypervisor());
            resp.setId(vol.getId());
            resp.setInstanceId(vol.getVirtualMachineId());
            resp.setSize(vol.getSize());
            resp.setSnapshotId(vol.getSnapshotId());
            resp.setState(vol.getState());
            resp.setType(vol.getVolumeType());
            resp.setVMState(vol.getVirtualMachineState());
            resp.setZoneName(vol.getZoneName());
            return resp;
         }
         return null;
      } catch( Exception e ) {
         logger.error( "EC2 CreateVolume - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#deleteVolume(com.cloud.bridge.service.core.ec2.EC2Volume)
    */
   @Override
   public EC2Volume deleteVolume( EC2Volume request ) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         CloudStackInfoResponse resp = getApi().deleteVolume(request.getId());
         if(resp != null) {
            request.setState("deleted");
            return request;
         }

         throw new EC2ServiceException(ServerError.InternalError, "An unexpected error occurred.");
      } catch( Exception e ) {
         logger.error( "EC2 DeleteVolume 2 - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#modifyTags(com.cloud.bridge.service.core.ec2.EC2Tags, java.lang.String)
    */
   @Override
   public boolean modifyTags( EC2Tags request, String operation) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         Multimap<String, String> resourceTagList = new ArrayMultimap<String, String>();
         for ( EC2TagKeyValue resourceTag : request.getResourceTags()){
            CloudStackKeyValue pair = new CloudStackKeyValue();
            pair.setKeyValue(resourceTag.getKey(), resourceTag.getValue());
            resourceTagList.add(pair);
         }
         EC2TagTypeId[] resourceTypeSet = request.getResourceTypeSet();
         for (EC2TagTypeId resourceType : resourceTypeSet) {
            String cloudStackResourceType = mapToCloudStackResourceType(resourceType.getResourceType());
            List<String> resourceIdList = new ArrayList<String>();
            for ( String resourceId : resourceType.getResourceIds())
               resourceIdList.add(resourceId);
            CloudStackInfoResponse resp = new CloudStackInfoResponse();
            if (operation.equalsIgnoreCase("create"))
               resp = getApi().createTags(cloudStackResourceType, resourceIdList, resourceTagList);
            else if(operation.equalsIgnoreCase("delete"))
               resp = getApi().deleteTags(cloudStackResourceType, resourceIdList, resourceTagList);
            else
               throw new EC2ServiceException( ServerError.InternalError, "Unknown operation." );
            if (resp.getSuccess() == false)
               return false;
         }
         return true;
      } catch (Exception e){
         logger.error( "EC2 Create/Delete Tags - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ?
         e.getMessage() : "An unexpected error occurred.");
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#describeTags(com.cloud.bridge.service.core.ec2.EC2DescribeTags)
    */
   @Override
   public EC2DescribeTagsResponse describeTags (EC2DescribeTags request) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         EC2DescribeTagsResponse tagResponse = new EC2DescribeTagsResponse();
         List<CloudStackResourceTag> resourceTagList = getApi().listTags(null, null, null, true, null);

         List<EC2ResourceTag> tagList = new ArrayList<EC2ResourceTag>();
         if (resourceTagList != null && resourceTagList.size() > 0) {
            for (CloudStackResourceTag resourceTag: resourceTagList) {
               EC2ResourceTag tag = new EC2ResourceTag();
               tag.setResourceId(resourceTag.getResourceId());
               tag.setResourceType(mapToAmazonResourceType(resourceTag.getResourceType()));
               tag.setKey(resourceTag.getKey());
               if (resourceTag.getValue() != null)
                  tag.setValue(resourceTag.getValue());
               tagResponse.addTags(tag);
            }
         }

         EC2TagsFilterSet tfs = request.getFilterSet();
         if (tfs == null)
            return tagResponse;
         else
            return tfs.evaluate(tagResponse);
      } catch(Exception e) {
         logger.error("EC2 DescribeTags - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#rebootInstances(com.cloud.bridge.service.core.ec2.EC2RebootInstances)
    */
   @Override
   public boolean rebootInstances(EC2RebootInstances request)
   {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*

      EC2Instance[] vms = null;

      // -> reboot is not allowed on destroyed (i.e., terminated) instances
      try {
         String[] instanceSet = request.getInstancesSet();
         EC2DescribeInstancesResponse previousState = listVirtualMachines( instanceSet, null, null );
         vms = previousState.getInstanceSet();

         // -> send reboot requests for each found VM
         for (EC2Instance vm : vms) {
            if (vm.getState().equalsIgnoreCase( "Destroyed" )) continue;

            CloudStackUserVm resp = getApi().rebootVirtualMachine(vm.getId());
            if (logger.isDebugEnabled())
               logger.debug("Rebooting VM " + resp.getId() + " job " + resp.getJobId());
         }

         // -> if some specified VMs where not found we have to tell the caller
         if (instanceSet.length != vms.length)
            throw new EC2ServiceException(ClientError.InvalidAMIID_NotFound, "One or more instanceIds do not exist, other instances rebooted.");

         return true;
      } catch( Exception e ) {
         logger.error( "EC2 RebootInstances - ", e );
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#runInstances(com.cloud.bridge.service.core.ec2.EC2RunInstances)
    */
   @Override
   public EC2RunInstancesResponse runInstances(EC2RunInstances request) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      EC2RunInstancesResponse instances = new EC2RunInstancesResponse();
      int createInstances    = 0;
      int canCreateInstances = -1;
      int countCreated       = 0;

      try {
         CloudStackAccount caller = getCurrentAccount();

         // ugly...
         canCreateInstances = calculateAllowedInstances();
         if (-1 == canCreateInstances) canCreateInstances = request.getMaxCount();

         if (canCreateInstances < request.getMinCount()) {
            logger.info( "EC2 RunInstances - min count too big (" + request.getMinCount() + "), " + canCreateInstances + " left to allocate");
            throw new EC2ServiceException(ClientError.InstanceLimitExceeded ,"Only " + canCreateInstances + " instance(s) left to allocate");
         }

         if ( canCreateInstances < request.getMaxCount())
            createInstances = request.getMinCount();
         else
            createInstances = request.getMaxCount();

         //find CS service Offering ID
         String instanceType = "m1.small";
         if(request.getInstanceType() != null){
            instanceType = request.getInstanceType();
         }
         CloudStackServiceOffering svcOffering = null; //getCSServiceOfferingId(instanceType);
         if(svcOffering == null){
            logger.info("No ServiceOffering found to be defined by name, please contact the administrator "+instanceType );
            throw new EC2ServiceException(ClientError.Unsupported, "instanceType: [" + instanceType + "] not found!");
         }

         // zone stuff
         String zoneId = toZoneId(request.getZoneName(), null);

         List<CloudStackZone> zones = getApi().listZones(null, null, zoneId, null);
         if (zones == null || zones.size() == 0) {
            logger.info("EC2 RunInstances - zone [" + request.getZoneName() + "] not found!");
            throw new EC2ServiceException(ClientError.InvalidZone_NotFound, "ZoneId [" + request.getZoneName() + "] not found!");
         }
         // we choose first zone?
         CloudStackZone zone = zones.get(0);

         // network
         CloudStackNetwork network = findNetwork(zone);

         // now actually deploy the vms
         for( int i=0; i < createInstances; i++ ) {
            CloudStackUserVm resp = getApi().deployVirtualMachine(svcOffering.getId(),
            request.getTemplateId(), zoneId, null, null, null, null,
            null, null, null, request.getKeyName(), null, (network != null ? network.getId() : null),
            null, constructList(request.getGroupSet()), request.getSize().longValue(), request.getUserData());
            EC2Instance vm = new EC2Instance();
            vm.setId(resp.getId().toString());
            vm.setName(resp.getName());
            vm.setZoneName(resp.getZoneName());
            vm.setTemplateId(resp.getTemplateId().toString());
            if (resp.getSecurityGroupList() != null && resp.getSecurityGroupList().size() > 0) {
               // TODO, we have a list of security groups, just return the first one?
               List<CloudStackSecurityGroup> securityGroupList = resp.getSecurityGroupList();
               for (CloudStackSecurityGroup securityGroup : securityGroupList) {
                  vm.addGroupName(securityGroup.getName());
               }
            }
            vm.setState(resp.getState());
            vm.setCreated(resp.getCreated());
            List <CloudStackNic> nicList = resp.getNics();
            for (CloudStackNic nic : nicList) {
               if (nic.getIsDefault()) {
                  vm.setPrivateIpAddress(nic.getIpaddress());
                  break;
               }
            }
            vm.setIpAddress(resp.getIpAddress());
            vm.setAccountName(resp.getAccountName());
            vm.setDomainId(resp.getDomainId());
            vm.setHypervisor(resp.getHypervisor());
            vm.setServiceOffering( svcOffering.getName());
            instances.addInstance(vm);
            countCreated++;
         }

         if (0 == countCreated) {
            // TODO, we actually need to destroy left-over VMs when the exception is thrown
            throw new EC2ServiceException(ServerError.InsufficientInstanceCapacity, "Insufficient Instance Capacity" );
         }

         return instances;
      } catch( Exception e ) {
         logger.error( "EC2 RunInstances - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#startInstances(com.cloud.bridge.service.core.ec2.EC2StartInstances)
    */
   @Override
   public EC2StartInstancesResponse startInstances(EC2StartInstances request) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      EC2StartInstancesResponse instances = new EC2StartInstancesResponse();
      EC2Instance[] vms = null;

      // -> first determine the current state of each VM (becomes it previous state)
      try {
         EC2DescribeInstancesResponse previousState = listVirtualMachines( request.getInstancesSet(), null, null );
         vms = previousState.getInstanceSet();

         // -> send start requests for each item
         for (EC2Instance vm : vms) {
            vm.setPreviousState(vm.getState());

            // -> if its already running then we don't care
            if (vm.getState().equalsIgnoreCase( "Running" ) || vm.getState().equalsIgnoreCase( "Destroyed" )) {
               instances.addInstance(vm);
               continue;
            }

            CloudStackUserVm resp = getApi().startVirtualMachine(vm.getId());
            if(resp != null){
               vm.setState(resp.getState());
               if(logger.isDebugEnabled())
                  logger.debug("Starting VM " + vm.getId() + " job " + resp.getJobId());
            }
            instances.addInstance(vm);
         }
         return instances;
      } catch( Exception e ) {
         logger.error( "EC2 StartInstances - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
*/
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#stopInstances(com.cloud.bridge.service.core.ec2.EC2StopInstances)
    */
   @Override
   public EC2StopInstancesResponse stopInstances(EC2StopInstances request) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      EC2StopInstancesResponse instances = new EC2StopInstancesResponse();
      EC2Instance[] virtualMachines = null;

      // -> first determine the current state of each VM (becomes it previous state)
      try {
         String[] instanceSet = request.getInstancesSet();

         EC2DescribeInstancesResponse previousState = listVirtualMachines( instanceSet, null, null );
         virtualMachines = previousState.getInstanceSet();

         // -> send stop requests for each item
         for (EC2Instance vm : virtualMachines) {
            vm.setPreviousState( vm.getState());
            CloudStackUserVm resp = null;
            if (request.getDestroyInstances()) {
               if (vm.getState().equalsIgnoreCase( "Destroyed" )) {
                  instances.addInstance(vm);
                  continue;
               }
               resp = getApi().destroyVirtualMachine(vm.getId());
               if(logger.isDebugEnabled())
                  logger.debug("Destroying VM " + vm.getId() + " job " + resp.getJobId());
            } else {
               if (vm.getState().equalsIgnoreCase("Stopped") || vm.getState().equalsIgnoreCase("Destroyed")) {
                  instances.addInstance(vm);
                  continue;
               }
               resp = getApi().stopVirtualMachine(vm.getId(), false);
               if(logger.isDebugEnabled())
                  logger.debug("Stopping VM " + vm.getId() + " job " + resp.getJobId());
            }
            if (resp != null) {
               vm.setState(resp.getState());
               instances.addInstance(vm);
            }
         }
         return instances;
      } catch( Exception e ) {
         logger.error( "EC2 StopInstances - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() + ", might already be destroyed" : "An unexpected error occurred.");
      }
*/
   }

   /**
    * RunInstances includes a min and max count of requested instances to create.
    * We have to be able to create the min number for the user or none at all.  So
    * here we determine what the user has left to create.
    *
    * @return -1 means no limit exists, other positive numbers give max number left that
    *         the user can create.
    */
   private int calculateAllowedInstances() throws Exception {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      int maxAllowed = -1;

      CloudStackAccount ourAccount = getCurrentAccount();

      if (ourAccount == null) {
         // This should never happen, but
         // we will return -99999 if this happens...
         return -99999;
      }

      // if accountType is Admin == 1, then let's return -1
      if (ourAccount.getAccountType() == 1) return -1;

      // -> get the user limits on instances
      // "0" represents instances:
      // http://download.cloud.com/releases/2.2.0/api_2.2.8/user/listResourceLimits.html
      List<CloudStackResourceLimit> limits = getApi().listResourceLimits(null, null, null, null, "0");
      if (limits != null && limits.size() > 0) {
         maxAllowed = (int)limits.get(0).getMax().longValue();
         if (maxAllowed == -1)
            return -1;   // no limit

         EC2DescribeInstancesResponse existingVMS = listVirtualMachines( null, null, null );
         EC2Instance[] vmsList = existingVMS.getInstanceSet();
         return (maxAllowed - vmsList.length);
      } else {
         return 0;
      }
*/
   }

   /**
    * Performs the cloud API listVirtualMachines one or more times.
    *
    * @param virtualMachineIds - an array of instances we are interested in getting information on
    * @param ifs - filter out unwanted instances
    */
   private EC2DescribeInstancesResponse listVirtualMachines( String[] virtualMachineIds, EC2InstanceFilterSet ifs,
                                                             Multimap<String, String> resourceTags ) throws Exception
   {
      EC2DescribeInstancesResponse instances = new EC2DescribeInstancesResponse();

      if (null == virtualMachineIds || 0 == virtualMachineIds.length) {
         instances = lookupInstances( null, instances, resourceTags );
      } else {
         for( int i=0; i <  virtualMachineIds.length; i++ ) {
            instances = lookupInstances( virtualMachineIds[i], instances, resourceTags );
         }
      }

      if ( null == ifs )
         return instances;
      else return ifs.evaluate( instances );
   }

   /**
    * Get one or more templates depending on the volumeId parameter.
    *
    * @param volumeId   - if interested in one specific volume, null if want to list all volumes
    * @param instanceId - if interested in volumes for a specific instance, null if instance is not important
    */
   private EC2DescribeVolumesResponse listVolumes(String volumeId, String instanceId, EC2DescribeVolumesResponse volumes,
                                                  Multimap<String, String> resourceTagSet)throws Exception {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*

      List<CloudStackVolume> vols = getApi().listVolumes(null, null, null, volumeId, null, null, null, null, null,
      instanceId, null, resourceTagSet);
      if(vols != null && vols.size() > 0) {
         for(CloudStackVolume vol : vols) {
            EC2Volume ec2Vol = new EC2Volume();
            ec2Vol.setId(vol.getId());
            if(vol.getAttached() != null)
               ec2Vol.setAttached(vol.getAttached());
            ec2Vol.setCreated(vol.getCreated());

            if(vol.getDeviceId() != null)
               ec2Vol.setDeviceId(vol.getDeviceId());
            ec2Vol.setHypervisor(vol.getHypervisor());

            if(vol.getSnapshotId() != null)
               ec2Vol.setSnapshotId(vol.getSnapshotId());
            ec2Vol.setState(mapToAmazonVolState(vol.getState()));
            ec2Vol.setSize(vol.getSize());
            ec2Vol.setType(vol.getVolumeType());

            if(vol.getVirtualMachineId() != null)
               ec2Vol.setInstanceId(vol.getVirtualMachineId());

            if(vol.getVirtualMachineState() != null)
               ec2Vol.setVMState(vol.getVirtualMachineState());
            ec2Vol.setZoneName(vol.getZoneName());

            Multimap<String, String> resourceTags = vol.getTags();
            for(CloudStackKeyValue resourceTag : resourceTags) {
               EC2TagKeyValue param = new EC2TagKeyValue();
               param.setKey(resourceTag.getKey());
               if (resourceTag.getValue() != null)
                  param.setValue(resourceTag.getValue());
               ec2Vol.addResourceTag(param);
            }

            volumes.addVolume(ec2Vol);
         }
      }

      return volumes;
*/
   }

   /**
    * Translate the given zone name into the required zoneId.  Query for
    * a list of all zones and match the zone name given.   Amazon uses zone
    * names while the Cloud API often requires the zoneId.
    *
    * @param zoneName - (e.g., 'AH'), if null return the first zone in the available list
    *
    * @return the zoneId that matches the given zone name
    */
   private String toZoneId(String zoneName, String domainId) throws Exception	{
      EC2DescribeAvailabilityZonesResponse zones = null;
      String[] interestedZones = null;

      if ( null != zoneName) {
         interestedZones = new String[1];
         interestedZones[0] = zoneName;
      }else {
         //TODO: default zone
      }

      zones = listZones(interestedZones, domainId);

      if (zones == null || zones.getZoneIdAt( 0 ) == null)
         throw new EC2ServiceException(ClientError.InvalidParameterValue, "Unknown zoneName value - " + zoneName);
      return zones.getZoneIdAt(0);
   }

   /**
    * More than one place we need to access the defined list of zones.  If given a specific
    * list of zones of interest, then only values from those zones are returned.
    *
    * @param interestedZones - can be null, should be a subset of all zones
    *
    * @return EC2DescribeAvailabilityZonesResponse
    */
   private EC2DescribeAvailabilityZonesResponse listZones(String[] interestedZones, String domainId) throws Exception
   {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      EC2DescribeAvailabilityZonesResponse zones = new EC2DescribeAvailabilityZonesResponse();

      List<CloudStackZone> cloudZones = getApi().listZones(true, domainId, null, null);

      if(cloudZones != null) {
         for(CloudStackZone cloudZone : cloudZones) {
            if ( null != interestedZones && 0 < interestedZones.length ) {
               for( int j=0; j < interestedZones.length; j++ ) {
                  if (interestedZones[j].equalsIgnoreCase( cloudZone.getName())) {
                     zones.addZone(cloudZone.getId().toString(), cloudZone.getName());
                     break;
                  }
               }
            } else {
               zones.addZone(cloudZone.getId().toString(), cloudZone.getName());
            }
         }
      }
      return zones;
*/
   }


   /**
    * Get information on one or more virtual machines depending on the instanceId parameter.
    *
    * @param instanceId - if null then return information on all existing instances, otherwise
    *                     just return information on the matching instance.
    * @param instances  - a container object to fill with one or more EC2Instance objects
    *
    * @return the same object passed in as the "instances" parameter modified with one or more
    *         EC2Instance objects loaded.
    */
   private EC2DescribeInstancesResponse lookupInstances( String instanceId, EC2DescribeInstancesResponse instances,
                                                         Multimap<String, String> resourceTagSet )
   throws Exception {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*

      String instId = instanceId != null ? instanceId : null;
      List<CloudStackUserVm> vms = getApi().listVirtualMachines(null, null, true, null, null, null, null,
      instId, null, null, null, null, null, null, null, null, resourceTagSet);

      if(vms != null && vms.size() > 0) {
         for(CloudStackUserVm cloudVm : vms) {
            EC2Instance ec2Vm = new EC2Instance();

            ec2Vm.setId(cloudVm.getId().toString());
            ec2Vm.setName(cloudVm.getName());
            ec2Vm.setZoneName(cloudVm.getZoneName());
            ec2Vm.setTemplateId(cloudVm.getTemplateId().toString());
            ec2Vm.setGroup(cloudVm.getGroup());
            ec2Vm.setState(cloudVm.getState());
            ec2Vm.setCreated(cloudVm.getCreated());
            ec2Vm.setIpAddress(cloudVm.getIpAddress());
            ec2Vm.setAccountName(cloudVm.getAccountName());
            ec2Vm.setDomainId(cloudVm.getDomainId());
            ec2Vm.setHypervisor(cloudVm.getHypervisor());
            ec2Vm.setRootDeviceType(cloudVm.getRootDeviceType());
            ec2Vm.setRootDeviceId(cloudVm.getRootDeviceId());
//    			ec2Vm.setServiceOffering(serviceOfferingIdToInstanceType(cloudVm.getServiceOfferingId().toString()));

            List<CloudStackNic> nics = cloudVm.getNics();
            for(CloudStackNic nic : nics) {
               if(nic.getIsDefault()) {
                  ec2Vm.setPrivateIpAddress(nic.getIpaddress());
                  break;
               }
            }

            Multimap<String, String> resourceTags = cloudVm.getTags();
            for(CloudStackKeyValue resourceTag : resourceTags) {
               EC2TagKeyValue param = new EC2TagKeyValue();
               param.setKey(resourceTag.getKey());
               if (resourceTag.getValue() != null)
                  param.setValue(resourceTag.getValue());
               ec2Vm.addResourceTag(param);
            }

            if (cloudVm.getSecurityGroupList() != null && cloudVm.getSecurityGroupList().size() > 0) {
               // TODO, we have a list of security groups, just return the first one?
               List<CloudStackSecurityGroup> securityGroupList = cloudVm.getSecurityGroupList();
               for (CloudStackSecurityGroup securityGroup : securityGroupList) {
                  ec2Vm.addGroupName(securityGroup.getName());
               }
            }

            instances.addInstance(ec2Vm);
         }
      }else{
         if(instanceId != null){
            //no such instance found
            throw new EC2ServiceException(ServerError.InternalError, "Instance:" + instanceId + " not found");
         }
      }
      return instances;
*/
   }


   /**
    * Get one or more templates depending on the templateId parameter.
    *
    * @param templateId - if null then return information on all existing templates, otherwise
    *                     just return information on the matching template.
    * @param images     - a container object to fill with one or more EC2Image objects
    *
    * @return the same object passed in as the "images" parameter modified with one or more
    *         EC2Image objects loaded.
    */
   private EC2DescribeImagesResponse listTemplates( String templateId, EC2DescribeImagesResponse images ) throws EC2ServiceException {
//      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
      Org org = getApi().getOrgClient().findOrgNamed(null);

      for (ReferenceType catref : org.getCatalogs().values()) {
         Catalog catalog = vcloudApi.getCatalogClient().getCatalog(catref.getHref());
         for (ReferenceType itemref : catalog.values()) {
            CatalogItem item = vcloudApi.getCatalogClient().getCatalogItem(itemref.getHref());
            EC2Image ec2Image = new EC2Image();
            ec2Image.setId(item.getName());
            ec2Image.setAccountName(org.getHref().toString());
            ec2Image.setName(item.getName());
            ec2Image.setDescription(item.getDescription());
//            ec2Image.setOsTypeId(temp.getOsTypeId().toString());
//            ec2Image.setIsPublic(temp.getIsPublic());
//            ec2Image.setIsReady(temp.getIsReady());
//            ec2Image.setDomainId(temp.getDomainId());
//            Multimap<String, String> resourceTags = temp.getTags();
            for(Map.Entry<String, String> resourceTag : item.getProperties().entrySet()) {
               EC2TagKeyValue param = new EC2TagKeyValue();
               param.setKey(resourceTag.getKey());
               if (resourceTag.getValue() != null)
                  param.setValue(resourceTag.getValue());
               ec2Image.addResourceTag(param);
            }
            images.addImage(ec2Image);

         }

      }

      return images;
   }

   /* (non-Javadoc)
    * @see com.cloud.bridge.service.core.ec2.EC2Engine1#listSecurityGroups(java.lang.String[])
    */
   @Override
   public EC2DescribeSecurityGroupsResponse listSecurityGroups( String[] interestedGroups ) {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         EC2DescribeSecurityGroupsResponse groupSet = new EC2DescribeSecurityGroupsResponse();

         List<CloudStackSecurityGroup> groups = getApi().listSecurityGroups(null, null, null, true, null, null, null);
         if (groups != null && groups.size() > 0)
            for (CloudStackSecurityGroup group : groups) {
               boolean matched = false;
               if (interestedGroups.length > 0) {
                  for (String groupName :interestedGroups) {
                     if (groupName.equalsIgnoreCase(group.getName())) {
                        matched = true;
                        break;
                     }
                  }
               } else {
                  matched = true;
               }
               if (!matched) continue;
               EC2SecurityGroup ec2Group = new EC2SecurityGroup();
               // not sure if we should set both account and account name to accountname
               ec2Group.setAccount(group.getAccountName());
               ec2Group.setAccountName(group.getAccountName());
               ec2Group.setName(group.getName());
               ec2Group.setDescription(group.getDescription());
               ec2Group.setDomainId(group.getDomainId());
               ec2Group.setId(group.getId().toString());
               toPermission(ec2Group, group);

               groupSet.addGroup(ec2Group);
            }
         return groupSet;
      } catch(Exception e) {
         logger.error( "List Security Groups - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage());
      }
*/
   }

   /**
    * Convert ingress rule to EC2IpPermission records
    *
    * @param response
    * @param group
    * @return
    */
   private boolean toPermission(EC2SecurityGroup response, FirewallService fw ) {
      List<FirewallRule> rules = fw.getFirewallRules();

      if (rules == null || rules.isEmpty()) return false;

      for (FirewallRule rule : rules) {
         EC2IpPermission perm = new EC2IpPermission();
//         perm.setProtocol(rule.getProtocol());
//         perm.setFromPort(rule.getStartPort());
//         perm.setToPort(rule.getEndPort());
//         perm.setRuleId(rule.getRuleId() != null ? rule.getRuleId().toString() : new String());
//         perm.setIcmpCode(rule.getIcmpCode() != null ? rule.getIcmpCode().toString() : new String());
//         perm.setIcmpType(rule.getIcmpType() != null ? rule.getIcmpType().toString() : new String());
//         perm.setCIDR(rule.getCidr());
//         perm.addIpRange(rule.getCidr());
//
//         if (rule.getAccountName() != null && rule.getSecurityGroupName() != null) {
//            EC2SecurityGroup newGroup = new EC2SecurityGroup();
//            newGroup.setAccount(rule.getAccountName());
//            newGroup.setName(rule.getSecurityGroupName());
//            perm.addUser(newGroup);
//         }
         response.addIpPermission(perm);
      }
      return true;
   }

   /**
    * Windows has its own device strings.
    *
    * @param hypervisor
    * @param deviceId
    * @return
    */
   public static String cloudDeviceIdToDevicePath( String hypervisor, String deviceId )
   {
      Integer devId = new Integer(deviceId);
      if (null != hypervisor && hypervisor.toLowerCase().contains( "windows" )) {
         switch( devId ) {
            case 1:  return "xvdb";
            case 2:  return "xvdc";
            case 3:  return "xvdd";
            case 4:  return "xvde";
            case 5:  return "xvdf";
            case 6:  return "xvdg";
            case 7:  return "xvdh";
            case 8:  return "xvdi";
            case 9:  return "xvdj";
            default: return new String( "" + deviceId );
         }
      } else {    // -> assume its unix
         switch( devId ) {
            case 1:  return "/dev/sdb";
            case 2:  return "/dev/sdc";
            case 3:  return "/dev/sdd";
            case 4:  return "/dev/sde";
            case 5:  return "/dev/sdf";
            case 6:  return "/dev/sdg";
            case 7:  return "/dev/sdh";
            case 8:  return "/dev/sdi";
            case 9:  return "/dev/sdj";
            default: return new String( "" + deviceId );
         }
      }
   }


   /**
    * Translate the device name string into a Cloud Stack deviceId.
    * deviceId 3 is reserved for CDROM and 0 for the ROOT disk
    *
    * @param device string
    * @return deviceId value
    */
   private String mapDeviceToCloudDeviceId( String device )
   {
      if (device.equalsIgnoreCase( "/dev/sdb"  )) return "1";
      else if (device.equalsIgnoreCase( "/dev/sdc"  )) return "2";
      else if (device.equalsIgnoreCase( "/dev/sde"  )) return "4";
      else if (device.equalsIgnoreCase( "/dev/sdf"  )) return "5";
      else if (device.equalsIgnoreCase( "/dev/sdg"  )) return "6";
      else if (device.equalsIgnoreCase( "/dev/sdh"  )) return "7";
      else if (device.equalsIgnoreCase( "/dev/sdi"  )) return "8";
      else if (device.equalsIgnoreCase( "/dev/sdj"  )) return "9";

      else if (device.equalsIgnoreCase( "/dev/xvdb" )) return "1";
      else if (device.equalsIgnoreCase( "/dev/xvdc" )) return "2";
      else if (device.equalsIgnoreCase( "/dev/xvde" )) return "4";
      else if (device.equalsIgnoreCase( "/dev/xvdf" )) return "5";
      else if (device.equalsIgnoreCase( "/dev/xvdg" )) return "6";
      else if (device.equalsIgnoreCase( "/dev/xvdh" )) return "7";
      else if (device.equalsIgnoreCase( "/dev/xvdi" )) return "8";
      else if (device.equalsIgnoreCase( "/dev/xvdj" )) return "9";

      else if (device.equalsIgnoreCase( "xvdb"      )) return "1";
      else if (device.equalsIgnoreCase( "xvdc"      )) return "2";
      else if (device.equalsIgnoreCase( "xvde"      )) return "4";
      else if (device.equalsIgnoreCase( "xvdf"      )) return "5";
      else if (device.equalsIgnoreCase( "xvdg"      )) return "6";
      else if (device.equalsIgnoreCase( "xvdh"      )) return "7";
      else if (device.equalsIgnoreCase( "xvdi"      )) return "8";
      else if (device.equalsIgnoreCase( "xvdj"      )) return "9";

      else throw new EC2ServiceException( ClientError.Unsupported, device + " is not supported" );
   }

   /**
    * Map CloudStack instance state to Amazon state strings
    *
    * @param state
    * @return
    */
   private String mapToAmazonVolState( String state )
   {
      if (state.equalsIgnoreCase( "Allocated" ) ||
      state.equalsIgnoreCase( "Creating"  ) ||
      state.equalsIgnoreCase( "Ready"     )) return "available";

      if (state.equalsIgnoreCase( "Destroy"   )) return "deleting";

      return "error";
   }

   /**
    * Map Amazon resourceType to CloudStack resourceType
    *
    * @param Amazon resourceType
    * @return CloudStack resourceType
    */
   private String mapToCloudStackResourceType( String resourceType) {
      if (resourceType.equalsIgnoreCase("image"))
         return("template");
      else if(resourceType.equalsIgnoreCase("instance"))
         return("userVm");
      else
         return resourceType;
   }

   /**
    * Map Amazon resourceType to CloudStack resourceType
    *
    * @param CloudStack resourceType
    * @return Amazon resourceType
    */
   private String mapToAmazonResourceType( String resourceType) {
      if (resourceType.equalsIgnoreCase("template"))
         return("image");
      else if(resourceType.equalsIgnoreCase("userVm"))
         return("instance");
      else
         return (resourceType.toLowerCase());
   }

   /**
    * Stop an instance
    * Wait until one specific VM has stopped
    *
    * @param instanceId
    * @return
    * @throws Exception
    */
   private boolean stopVirtualMachine( String instanceId) throws Exception {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         CloudStackUserVm resp = getApi().stopVirtualMachine(instanceId, false);
         if (logger.isDebugEnabled())
            logger.debug("Stopping VM " + instanceId );
         return resp != null;
      } catch(Exception e) {
         logger.error( "StopVirtualMachine - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
*/
   }

   /**
    * Start an existing stopped instance(VM)
    *
    * @param instanceId
    * @return
    * @throws Exception
    */
   private boolean startVirtualMachine( String instanceId ) throws Exception {
      throw new EC2ServiceException(ServerError.InternalError, "Not implemented in Cinderella");
/*
      try {
         CloudStackUserVm resp = getApi().startVirtualMachine(instanceId);
         if (logger.isDebugEnabled())
            logger.debug("Starting VM " + instanceId );
         return resp != null;
      } catch(Exception e) {
         logger.error("StartVirtualMachine - ", e);
         throw new EC2ServiceException(ServerError.InternalError, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.");
      }
*/
   }

   private Multimap<String, String> getResourceTags(EC2TagKeyValue[] tagKeyValueSet) {
      Builder<String, String> builder = ImmutableMultimap.<String, String> builder();
      for (EC2TagKeyValue tagKeyValue : tagKeyValueSet) {
         builder.put(tagKeyValue.getKey(), tagKeyValue.getValue());
      }
      return builder.build();
   }

}
