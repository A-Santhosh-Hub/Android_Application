# SanBar release build (R8).
# Widgets, the listener service and the activity are referenced from AndroidManifest.xml, so R8
# keeps them automatically. Reflection in SanBar only targets Android's own classes
# (BluetoothDevice.getBatteryLevel / isConnected, RemoteViews tint methods), which R8 never touches.

-keepattributes SourceFile,LineNumberTable
