#!/bin/bash
cd ~/W/Personal/MRX_BatteryGuard

echo "Cleaning..."
rm -rf build/obj build/classes.dex build/app-unsigned.apk build/MRX_BatteryGuard.apk build/tmp
mkdir -p build/obj

echo "Generating R.java..."
aapt package -f -m -J src -M src/main/AndroidManifest.xml -S res -I /usr/lib/android-sdk/platforms/android-34/android.jar

echo "Compiling..."
/usr/lib/jvm/java-8-openjdk-amd64/bin/javac -source 7 -target 7 \
  -cp /usr/lib/android-sdk/platforms/android-34/android.jar \
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
mkdir -p build/tmp
cp build/classes.dex build/tmp/
aapt package -f -M src/main/AndroidManifest.xml -S res \
  -I /usr/lib/android-sdk/platforms/android-34/android.jar \
  -F build/app-unsigned.apk \
  build/tmp

echo "Signing..."
jarsigner -keystore debug.keystore -storepass android -keypass android \
  build/app-unsigned.apk androiddebugkey

echo "Aligning..."
zipalign -f 4 build/app-unsigned.apk build/MRX_BatteryGuard.apk

echo "Verifying..."
aapt dump badging build/MRX_BatteryGuard.apk | head -5

echo "DONE! APK size: $(ls -la build/MRX_BatteryGuard.apk | awk '{print $5}') bytes"
