# Mantem nomes das classes anotadas com @AppFunction* / @Serializable.
-keep class br.com.agendaai.capacitor.appfunctions.** { *; }
-keepattributes *Annotation*
# Kotlinx serialization
-keepclasseswithmembers,allowobfuscation class * {
    @kotlinx.serialization.Serializable <fields>;
}
-dontnote kotlinx.serialization.**
