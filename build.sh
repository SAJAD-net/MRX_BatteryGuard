#!/bin/bash
cd ~/W/Personal/MRX_BatteryGuard

echo "Cleaning..."
rm -rf build/obj build/classes.dex build/app-final.apk build/MRX_BatteryGuard.apk
mkdir -p build/obj

echo "Generating R.java..."
aapt package -f -m -J src -M src/main/AndroidManifest.xml -S res -I /usr/lib/android-sdk/platforms/android-23/android.jar

echo "Compiling..."
/usr/lib/jvm/java-8-openjdk-amd64/bin/javac -source 7 -target 7 \
  -cp /usr/lib/android-sdk/platforms/android-23/android.jar \
  -d build/obj \
  src/main/java/com/mrx/batteryguard/MainActivity.java \
  src/com/mrx/batteryguard/R.java \
  -Xlint:-options

if [ $? -ne 0 ]; then
  echo "COMPILE FAILED!"
  exit 1
fi

echo "Dexing..."
dalvik-exchange --dex --output=build/classes.dex build/obj

echo "Packaging..."
aapt package -f -M src/main/AndroidManifest.xml -S res \
  -I /usr/lib/android-sdk/platforms/android-23/android.jar \
  -F build/app-final.apk

echo "Adding dex..."
aapt add build/app-final.apk build/classes.dex

echo "Signing..."
jarsigner -keystore debug.keystore -storepass android -keypass android \
  build/app-final.apk androiddebugkey

echo "Aligning..."
zipalign -f 4 build/app-final.apk build/MRX_BatteryGuard.apk

echo "DONE!"
ls -la build/MRX_BatteryGuard.apk
