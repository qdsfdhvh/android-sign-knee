# keep sign jni methods
-keepclasseswithmembers class com.seiko.example.sign.** {
    public static <fields>;
    native <methods>;
}
