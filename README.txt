CallerAgentAndroid
===================

این پروژه معادل اندرویدی desktop_client.py است: شماره تماس ورودی را
می‌گیرد و به POST /call روی همان سرور (94.184.45.117) می‌فرستد.

باز کردن پروژه:
  Android Studio > File > Open... > پوشه CallerAgentAndroid را انتخاب کن.
  اگر پیغام Gradle wrapper پرسید، گزینه پیش‌فرض (Use default Gradle
  wrapper) را قبول کن.

فایل‌های اصلی:
  app/src/main/java/com/ivazplus/calleragent/MainActivity.kt
    - صفحه‌ی تنظیمات (آدرس سرور، پورت، کلید API، نام اپراتور)
  app/src/main/java/com/ivazplus/calleragent/CallRelayService.kt
    - سرویس همیشه‌روشن که به تماس ورودی گوش می‌دهد و POST می‌کند
  app/src/main/java/com/ivazplus/calleragent/BootReceiver.kt
    - بعد از ری‌استارت گوشی، سرویس را دوباره راه می‌اندازد

حداقل نسخه‌ی اندروید پشتیبانی‌شده: Android 8.0 (API 26)
