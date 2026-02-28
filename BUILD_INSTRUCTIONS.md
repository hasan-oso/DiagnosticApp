# 🚀 بناء المشروع على الحاسوب الثاني

## ⚠️ المتطلبات
قبل البدء، تأكد من تثبيت:
1. **Android Studio** (أحدث إصدار - Hedgehog أو أحدث)
2. **JDK 17** (يأتي مع Android Studio عادة)

---

## 📦 خطوات البناء



### 2️⃣ **افتح المشروع في Android Studio**

#### الطريقة الأولى: من داخل Android Studio
```
1. افتح Android Studio
2. File → Open
3. اختر مجلد المشروع: DiagnosticApp
4. اضغط OK
```

#### ماذا يحدث تلقائياً؟
✅ Android Studio سيكتشف أن المشروع Gradle  
✅ سيقوم بتحميل Gradle wrapper تلقائياً  
✅ سيقوم بمزامنة dependencies  
✅ قد يستغرق 2-5 دقائق في المرة الأولى  

---

### 3️⃣ **مزامنة Gradle**

إذا لم تحدث المزامنة تلقائياً:
```
1. File → Sync Project with Gradle Files
   (أو اضغط على أيقونة الفيل 🐘 في الأعلى)
2. انتظر حتى تنتهي (شريط التقدم في الأسفل)
```

**إذا ظهرت أخطاء:**
- تأكد من اتصال الإنترنت (لتحميل dependencies)
- Tools → SDK Manager → تأكد من تثبيت:
  - Android SDK 34 (أو أحدث)
  - Build Tools 34.0.0

---

### 4️⃣ **تحديث Supabase Credentials**

**قبل البناء**، حدّث ملف Constants:
```
1. افتح: app/src/main/java/com/diagnostics/app/utils/Constants.kt
2. غيّر السطرين:
   const val SUPABASE_URL = "YOUR_SUPABASE_URL"
   const val SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY"

3. انظر supabase_schema.sql للتعليمات الكاملة
```

---

### 5️⃣ **بناء المشروع**

#### من Android Studio (الأسهل):
```
Build → Make Project
(أو Ctrl+F9)
```

#### من Terminal داخل Android Studio:
```
View → Tool Windows → Terminal

# ثم اكتب:
.\gradlew assembleDebug
```

---

### 6️⃣ **تشغيل على جهاز حقيقي**

#### تحضير الجهاز:
```
1. Settings → About Phone → اضغط 7 مرات على Build Number
2. Settings → Developer Options → فعّل USB Debugging
3. وصّل الجهاز بالـ USB
4. اقبل RSA fingerprint على الجهاز
```

#### من Android Studio:
```
1. اختر الجهاز من القائمة العلوية
2. اضغط Run (▶ أخضر) أو Shift+F10
```

---

## 📊 البناء الناجح

عند نجاح البناء، ستجد الـ APK في:
```
DiagnosticApp/app/build/outputs/apk/debug/app-debug.apk
```

**الحجم المتوقع:** ~15-25 MB

---

## 🐛 حل المشاكل الشائعة

### المشكلة 1: "SDK location not found"
```
الحل: File → Project Structure → SDK Location
اختر Android SDK path (عادة في C:\Users\YourName\AppData\Local\Android\Sdk)
```

### المشكلة 2: "Gradle sync failed"
```
الحل:
1. File → Invalidate Caches → Invalidate and Restart
2. انتظر حتى يعيد بناء الـ cache
```

### المشكلة 3: "Java version mismatch"
```
الحل: File → Settings → Build, Execution, Deployment → Build Tools → Gradle
Gradle JDK: اختر "17" أو "Embedded JDK"
```

### المشكلة 4: Dependencies لا تُحمّل
```
الحل:
1. تأكد من الإنترنت
2. File → Settings → Build Tools → Gradle
3. فعّل "Enable offline mode" ثم عطّله مرة أخرى
```

---

## ✅ التأكد من التثبيت الصحيح

بعد التثبيت على الجهاز:
```
1. افتح التطبيق
2. غيّر اللغة → يجب أن تتغير فوراً
3. اضغط "Start Scan"
4. امنح الأذونات المطلوبة
5. يجب أن ترى شاشة "Checking device safety..."
```

**إذا فشل preflight check:**
- شحن البطارية أقل من 50% → اشحن الجهاز
- المساحة أقل من 1GB → احذف ملفات
- الحرارة أكثر من 45°C → بردّ الجهاز

---

## 📝 ملاحظات مهمة

⚠️ **استخدم جهاز حقيقي فقط** - الـ Emulator غير مدعوم  
⚠️ **Android 8.0+** على الأقل (API 26+)  
⚠️ **لا تقطع الاتصال** أثناء stress tests (15 دقيقة)  
⚠️ **راقب الحرارة** - يجب ألا تتجاوز 70°C  

---

## 🎯 الخطوات التالية بعد البناء الناجح

1. ✅ نفّذ `supabase_schema.sql` في Supabase
2. ✅ حدّث `Constants.kt` بالـ credentials
3. ✅ اعمل rebuild للمشروع
4. ✅ اختبر على جهاز حقيقي
5. ✅ راجع البيانات في Supabase

---

**كل التوفيق! 🚀**
