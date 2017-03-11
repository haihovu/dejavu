#!/bin/bash
rm keystore
keytool -genkeypair -keystore keystore -storetype JCEKS -storepass storepass -keypass keypass -alias dejavu -keyalg RSA -keysize 4096 -validity 3650
