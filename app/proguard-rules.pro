# Navigation's type-safe routes are @Serializable classes; keep their serializers.
-keepclassmembers @kotlinx.serialization.Serializable class com.vmleroy.tutora.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
