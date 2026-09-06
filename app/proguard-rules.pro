# JGit 需要保留反射相关的类
-keep class org.eclipse.jgit.** { *; }
-keep class org.eclipse.jgit.internal.storage.file.** { *; }
-keep class com.jcraft.jsch.** { *; }
-dontwarn org.eclipse.jgit.**
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }