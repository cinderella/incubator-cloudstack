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

 

# $Id: managesnapshot.sh 11601 2010-08-11 17:26:15Z kris $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.refactor/java/scripts/storage/qcow2/managesnapshot.sh $
# managesnapshot.sh -- manage snapshots for a single disk (create, destroy, rollback)

usage() {
  printf "Usage: %s: -c <path to disk> -n <snapshot name>\n" $(basename $0) >&2
  printf "Usage: %s: -d <path to disk> -n <snapshot name>\n" $(basename $0) >&2
  printf "Usage: %s: -r <path to disk> -n <snapshot name>\n" $(basename $0) >&2
  printf "Usage: %s: -b <path to disk> -n <snapshot name> -p <dest name>\n" $(basename $0) >&2
  exit 2
}

qemu_img="cloud-qemu-img"
which $qemu_img >& /dev/null
if [ $? -gt 0 ]
then
   which qemu-img >& /dev/null
   if [ $? -eq 0 ]
   then
       qemu_img="qemu-img"
   fi
fi

create_snapshot() {
  local disk=$1
  local snapshotname="$2"
  local failed=0

  if [ -f "${disk}" ]; then
     $qemu_img snapshot -c "$snapshotname" $disk

  
     if [ $? -gt 0 ]
     then
       failed=2
       printf "***Failed to create snapshot $snapshotname for path $disk\n" >&2
       $qemu_img snapshot -d "$snapshotname" $disk
    
       if [ $? -gt 0 ]
       then
          printf "***Failed to delete snapshot $snapshotname for path $disk\n" >&2
       fi
     fi
 else
    failed=3
    printf "***Failed to create snapshot $snapshotname, undefined type $disk\n" >&2
 fi

  return $failed 
}

destroy_snapshot() {
  local disk=$1
  local snapshotname=$2
  local failed=0

  if [ -f $disk ]; then
     $qemu_img snapshot -d "$snapshotname" $disk
     if [ $? -gt 0 ]
     then
       failed=2
       printf "Failed to delete snapshot $snapshotname for path $disk\n" >&2
     fi	
  else
     failed=3
     printf "***Failed to delete snapshot $snapshotname, undefined type $disk\n" >&2
  fi
  return $failed 
}

rollback_snapshot() {
  local disk=$1
  local snapshotname="$2"
  local failed=0

  $qemu_img snapshot -a $snapshotname $disk
  
  if [ $? -gt 0 ]
  then
    printf "***Failed to apply snapshot $snapshotname for path $disk\n" >&2
    failed=1
  fi
  
  return $failed 
}
backup_snapshot() {
  local disk=$1
  local snapshotname=$2
  local destPath=$3
  local destName=$4

  if [ ! -d $destPath ]
  then
     mkdir -p $destPath >& /dev/null
     if [ $? -gt 0 ]
     then
        printf "Failed to create $destPath" >&2
        return 3
     fi
  fi

  if [ -f ${disk} ]; then
    # Does the snapshot exist? 
    $qemu_img snapshot -l $disk|grep -w "$snapshotname" >& /dev/null
    if [ $? -gt 0 ]
    then
      printf "there is no $snapshotname on disk $disk" >&2
      return 1
    fi

    $qemu_img convert -f qcow2 -O qcow2 -s $snapshotname $disk $destPath/$destName >& /dev/null
    if [ $? -gt 0 ]
    then
      printf "Failed to backup $snapshotname for disk $disk to $destPath" >&2
      return 2
    fi
  else
    printf "***Failed to backup snapshot $snapshotname, undefined type $disk\n" >&2
    return 3
  fi
  return 0
}
#set -x

cflag=
dflag=
rflag=
bflag=
nflag=
pathval=
snapshot=
tmplName=
deleteDir=

while getopts 'c:d:r:n:b:p:t:f' OPTION
do
  case $OPTION in
  c)	cflag=1
	pathval="$OPTARG"
	;;
  d)    dflag=1
        pathval="$OPTARG"
        ;;
  r)    rflag=1
        pathval="$OPTARG"
        ;;
  b)    bflag=1
        pathval="$OPTARG"
        ;;
  n)	nflag=1
	snapshot="$OPTARG"
	;;
  p)    destPath="$OPTARG"
        ;;
  t)    tmplName="$OPTARG"
	;;
  f)    deleteDir=1
	;;
  ?)	usage
	;;
  esac
done

[ -b "$pathval" ] && snapshot=`echo "${snapshot}" | md5sum -t | awk '{ print $1 }'`

if [ "$cflag" == "1" ]
then
  create_snapshot $pathval "$snapshot"
  exit $?
elif [ "$dflag" == "1" ]
then
  destroy_snapshot $pathval "$snapshot" $deleteDir
  exit $?
elif [ "$bflag" == "1" ]
then
  backup_snapshot $pathval $snapshot $destPath $tmplName
  exit $?
elif [ "$rflag" == "1" ]
then
  rollback_snapshot $pathval "$snapshot" $destPath
  exit $?
fi


exit 0
