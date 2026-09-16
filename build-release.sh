#!/bin/bash
cd ~/W/Personal/MRX_BatteryGuard

echo "Cleaning..."
rm -rf build/obj build/classes.dex build/app-release-unsigned.apk build/app-release-aligned.apk build/MRX_BatteryGuard-release.apk
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

if [ $? -ne 0 ]; then echo "COMPILE FAILED!"; exit 1; fi

echo "Dexing..."
dalvik-exchange --dex --output=build/classes.dex build/obj

echo "Packaging..."
mkdir -p build/tmp
cp build/classes.dex build/tmp/
aapt package -f -M src/main/AndroidManifest.xml -S res \
  -I /usr/lib/android-sdk/platforms/android-34/android.jar \
  -F build/app-release-unsigned.apk \
  build/tmp

echo "Aligning (before signing)..."
zipalign -f 4 build/app-release-unsigned.apk build/app-release-aligned.apk

echo "Signing with APK Signature Scheme v2..."
apksigner sign \
  --ks mrx-release.keystore \
  --ks-pass pass:mrx2024 \
  --key-pass pass:mrx2024 \
  --ks-key-alias mrx \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out build/MRX_BatteryGuard-release.apk \
  build/app-release-aligned.apk

echo "Verifying signature..."
apksigner verify --verbose build/MRX_BatteryGuard-release.apk

echo ""
echo "DONE! Release APK: build/MRX_BatteryGuard-release.apk"
ls -la build/MRX_BatteryGuard-release.apk
