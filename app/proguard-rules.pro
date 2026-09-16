# Atrybuty potrzebne bibliotekom korzystającym z refleksji/generyków w runtime
# (Room, Gemini SDK, Billing, Play Core) - bez nich R8 potrafi je po cichu zepsuć.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Billing
-keep class com.android.billingclient.** { *; }

# Gemini (com.google.ai.client.generativeai) - SDK serializuje żądania/odpowiedzi
# przez refleksję, więc jego klasy modeli muszą przejść przez R8 bez zmian nazw.
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# AdMob (Rewarded Ads)
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
# Wewnętrzny kod play-services-ads odwołuje się do klas z nowszego SDK (Android 15) niż
# nasz compileSdk - nie ma ich w android.jar, ale to martwe gałęzie kodu na starszych
# wersjach systemu, więc bezpiecznie je wyciszamy zamiast podnosić compileSdk.
-dontwarn android.media.LoudnessCodecController
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener

# Google Play In-App Review
-keep class com.google.android.play.core.review.** { *; }

# Keep data models used for JSON / Room mapping (DTO dla cards.json + encje Room)
-keep class com.mazur.tarot.data.model.** { *; }
-keep class com.mazur.tarot.data.local.db.** { *; }
