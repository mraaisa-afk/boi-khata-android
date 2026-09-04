# P8: Keep Room schema and generated DAO contracts used through reflection.
-keep class com.boikhata.core.database.entity.** { *; }
-keep interface com.boikhata.core.database.dao.** { *; }
-keep class com.boikhata.core.database.**Database { *; }

# P8: Keep Hilt entry points and WorkManager worker constructors.
-keep class com.boikhata.**Worker { *; }
-keep class dagger.hilt.** { *; }
-keep class androidx.hilt.work.** { *; }

# P8: Firestore field-by-field mapping uses declared model members and enum names.
-keep class com.boikhata.core.cloud.** { *; }
-keepclassmembers class com.boikhata.** { @com.google.firebase.firestore.PropertyName <fields>; }

# Compose and Android generated metadata are retained by their upstream consumer rules.
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
