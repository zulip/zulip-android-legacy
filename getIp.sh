#!/usr/bin/env bash
set -e

# get ip address of host
OUTPUT="$(ip a | grep inet | sed -n '2p')"
arr=(${OUTPUT})
echo ${arr[1]} | cut -f1 -d'/'
ipAddress="$(echo ${arr[1]} | cut -f1 -d'/')"

# set ipaddress append with port 9991 as server url in BaseTest and LoginDevAuthTest
sed -i "51s/.*/config.vm.network “forwarded_port”, guest: 9991, host: host_port, host_ip: \"0.0.0.0\"/" zulip/Vagrantfile
sed -i "42s/.*/    private static String SERVER_URL = \"http:\/\/${ipAddress}:9991\";/" app/src/androidTest/java/com/zulip/android/activities/LoginDevAuthTest.java
sed -i "40s/.*/    public static final String SERVER_URL = \"http:\/\/${ipAddress}:9991\";/" app/src/androidTest/java/com/zulip/android/activities/BaseTest.java
