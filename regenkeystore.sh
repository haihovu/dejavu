#!/bin/bash
rm keystore
keytool -genkeypair -keystore keystore -storetype JCEKS -storepass storepass -keypass keypass -alias midas -keyalg RSA -keysize 4096 -validity 365