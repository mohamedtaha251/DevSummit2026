# Standard Baseline R8 / ProGuard Configuration

# Preserve annotations and type signatures
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Model and serialization keep rules are now handled type-safely via Jetpack Keep (@UnconditionallyKeep)

# Ignore optional debug dependencies
-dontwarn com.google.firebase.appcheck.debug.**
