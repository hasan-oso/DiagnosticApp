package com.diagnostics.app.utils.language

import com.diagnostics.app.utils.LanguageManager

data class Strings(
    // ─── Auth / Welcome ───
    val welcomeTitle: String,
    val welcomeSubtitle: String,
    val tagline: String,
    val loginButton: String,
    val registerButton: String,
    val loginTitle: String,
    val registerTitle: String,
    val emailHint: String,
    val passwordHint: String,
    val passConfirmHint: String,
    val forgotPassword: String,
    val noAccount: String,
    val hasAccount: String,
    val selectLanguage: String,

    // ─── Registration Steps ───
    val step: String,
    val accountInfo: String,
    val ownerInfo: String,
    val shopInfo: String,
    val businessType: String,
    val shopLogo: String,
    val shopNameHint: String,
    val ownerNameHint: String,
    val phoneHint: String,
    val cityHint: String,
    val districtHint: String,
    val addressHint: String,
    val uploadingLogo: String,
    val continueButton: String,
    val backButton: String,
    val finishButton: String,

    // ─── Dashboard ───
    val dashboard: String,
    val inventory: String,
    val totalDevices: String,
    val inStock: String,
    val sold: String,
    val totalProfit: String,
    val purchasePrice: String,
    val sellingPrice: String,
    val markAsSold: String,
    val generateQr: String,
    val logout: String,
    val scanCount: String,

    // ─── QR ───
    val scanQrCode: String,
    val qrExpiry: String,
    val generateNewQr: String,
    val scanShopQr: String,
    val skipScan: String,
    val qrInvalid: String,
    val qrExpired: String,
    val qrValidForOneDevice: String,
    val shareQr: String,

    // ─── Report ───
    val reportTitle: String,
    val downloadPdf: String,
    val sharePdf: String,
    val overallScore: String,
    val battery: String,
    val performance: String,
    val storage: String,
    val security: String,

    // ─── Scan ───
    val startScan: String,
    val scanning: String,
    val scanComplete: String,

    // ─── Common ───
    val save: String,
    val cancel: String,
    val back: String,
    val loading: String,
    val error: String,
    val success: String,
    val delete: String,
    val edit: String,
    val share: String,
    val close: String,

    // ─── SellDeviceDialog ───
    val noteOptional: String,
    val profit: String,
    val currencySymbol: String,

    // ─── ReportScreen ───
    val yes: String,
    val no: String,
    val didYouBuyDevice: String,
    val voltageDropChart: String,
    val cpuTempChart: String,
    val cpuFreqChart: String,
    val noVoltageReadings: String,
    val noTempReadings: String,
    val noFreqReadings: String,

    // ─── QrScanScreen ───
    val noQrCodeQuestion: String,
    val qrScanError: String,
    val cameraPermissionRequired: String,

    // ─── PdfSuccessDialog ───
    val reportSaved: String,
    val reportReadyIn: String,
    val openReport: String,

    // ─── GenerateQrScreen ───
    val generateQrScreenTitle: String,

    // ─── DashboardScreen ───
    val generateQrForCustomer: String,

    // ─── WelcomeScreen ───
    val orText: String,
    val scanWithoutAccount: String,

    // ─── QR & Purchase ───
    val purchasePriceQuestion: String,
    val saveToInventory: String,
    val skip: String,

    // ─── GenerateQrScreen ───
    val scanQrToStart: String,
    val qrExpiredLabel: String,
    val qrExpiresIn: String,
    val shareQrCode: String,
    val qrShareTextPrefix: String,

    // ─── QrScanScreen ───
    val scanQrSubtitle: String,

    // ─── ReportScreen ───
    val addToStockSubtitle: String,
    val savedToInventoryToast: String,

    // ─── Security Warning ───
    val securityWarning: String,
    val deviceRooted: String,
    val developerOptionsOn: String,
    val adbWarning: String,
    
    // ─── PDF ───
    val errorOpeningPdf: String,
    val errorSharingPdf: String,
    val noPdfApp: String,
    val shareReport: String,
    val notAvailable: String
)

object AppStrings {

    val turkish = Strings(
        // Auth
        welcomeTitle = "NeuralCheck",
        welcomeSubtitle = "Yapay Zeka Destekli Cihaz Teşhisi",
        tagline = "Yapay Sinir Ağları İnceler, Bilim Karar Verir",
        loginButton = "Giriş Yap",
        registerButton = "Hesap Oluştur",
        loginTitle = "Tekrar Hoşgeldiniz",
        registerTitle = "Hesap Oluştur",
        emailHint = "E-posta Adresi",
        passwordHint = "Şifre",
        passConfirmHint = "Şifreyi Onayla",
        forgotPassword = "Şifremi Unuttum?",
        noAccount = "Hesabınız yok mu? Kayıt Olun",
        hasAccount = "Zaten hesabınız var mı? Giriş Yapın",
        selectLanguage = "Dil Seçin",
        // Registration
        step = "Adım",
        accountInfo = "Hesap Bilgileri",
        ownerInfo = "Sahip Bilgileri",
        shopInfo = "Mağaza Bilgileri",
        businessType = "İşletme Türü",
        shopLogo = "Mağaza Logosu",
        shopNameHint = "Mağaza Adı",
        ownerNameHint = "Sahip Adı",
        phoneHint = "Telefon Numarası",
        cityHint = "Şehir",
        districtHint = "İlçe",
        addressHint = "Adres",
        uploadingLogo = "Logo Yükleniyor...",
        continueButton = "Devam Et",
        backButton = "Geri Dön",
        finishButton = "Tamamla",
        // Dashboard
        dashboard = "Kontrol Paneli",
        inventory = "Envanter",
        totalDevices = "Toplam Cihaz",
        inStock = "Stokta",
        sold = "Satıldı",
        totalProfit = "Toplam Kâr",
        purchasePrice = "Alış Fiyatı",
        sellingPrice = "Satış Fiyatı",
        markAsSold = "Satıldı Olarak İşaretle",
        generateQr = "QR Kodu Oluştur",
        logout = "Çıkış Yap",
        scanCount = "Tarama Sayısı",
        // QR
        scanQrCode = "QR Kodu Tara",
        qrExpiry = "Süre dolacak: ",
        generateNewQr = "Yeni QR Oluştur",
        scanShopQr = "Mağaza QR Kodunu Tara",
        skipScan = "Hesapsız Devam Et",
        qrInvalid = "Geçersiz veya kullanılmış QR kodu",
        qrExpired = "QR kodun süresi doldu. Yeni kod talep edin ⏰",
        qrValidForOneDevice = "Yalnızca bir cihaz için geçerlidir",
        shareQr = "QR Kodunu Paylaş",
        // Report
        reportTitle = "Teknik Rapor",
        downloadPdf = "PDF İndir",
        sharePdf = "Raporu Paylaş",
        overallScore = "Genel Puan",
        battery = "Batarya",
        performance = "Performans",
        storage = "Depolama",
        security = "Güvenlik",
        // Scan
        startScan = "Taramayı Başlat",
        scanning = "Taranıyor...",
        scanComplete = "Tarama Tamamlandı",
        // Common
        save = "Kaydet",
        cancel = "İptal",
        back = "Geri",
        loading = "Yükleniyor...",
        error = "Hata",
        success = "Başarılı",
        delete = "Sil",
        edit = "Düzenle",
        share = "Paylaş",
        close = "Kapat",
        // SellDeviceDialog
        noteOptional = "Not (isteğe bağlı)",
        profit = "Kâr:",
        currencySymbol = "TL",
        // ReportScreen
        yes = "Evet",
        no = "Hayır",
        didYouBuyDevice = "Bu cihazı satın aldınız mı?",
        voltageDropChart = "Gerilim Düşüşü",
        cpuTempChart = "CPU Sıcaklık Eğrisi",
        cpuFreqChart = "CPU Frekansı & Kısıtlama",
        noVoltageReadings = "Gerilim verisi yok.",
        noTempReadings = "Sıcaklık verisi yok.",
        noFreqReadings = "Frekans verisi yok.",
        // QrScanScreen
        noQrCodeQuestion = "QR kodu yok mu?",
        qrScanError = "Kod okuma hatası",
        cameraPermissionRequired = "⚠️ QR taramak için kamera izni gerekli",
        // PdfSuccessDialog
        reportSaved = "Rapor Kaydedildi",
        reportReadyIn = "raporu Downloads klasöründe hazır",
        openReport = "Raporu Aç",
        // GenerateQrScreen
        generateQrScreenTitle = "Tarama QR Kodu Oluştur",
        // Dashboard
        generateQrForCustomer = "Müşteri için QR Oluştur",
        // WelcomeScreen
        orText = "veya",
        scanWithoutAccount = "Hesapsız Tarama",
        // QR & Purchase (new keys only — scanQrCode/scanShopQr/qrValidForOneDevice already defined above)
        purchasePriceQuestion = "Bu cihazı kaça satın aldınız?",
        saveToInventory = "Envantere Kaydet",
        skip = "Atla",
        // GenerateQrScreen
        scanQrToStart = "Taraması başlatmak için bu kodu tarayın",
        qrExpiredLabel = "QR kodu süresi doldu",
        qrExpiresIn = "⏱️ Süre doluyor:",
        shareQrCode = "QR Kodunu Paylaş",
        qrShareTextPrefix = "Tarama kodu:",
        // QrScanScreen
        scanQrSubtitle = "Dükkan sahibinden QR kodunu isteyin\ndükkan hesabına bağlamak için",
        // ReportScreen
        addToStockSubtitle = "Stok listenize ekleyin ve kârınızı takip edin",
        savedToInventoryToast = "✅ Envantere kaydedildi",
        // Security Warning
        securityWarning = "Güvenlik Uyardı",
        deviceRooted = "Cihaz rootlanmış",
        developerOptionsOn = "Geliştirici seçenekleri açık",
        adbWarning = "USB hata ayıklama açık",

        // PDF
        errorOpeningPdf = "Dosya açılamadı",
        errorSharingPdf = "Paylaşım başarısız",
        noPdfApp = "PDF uygulaması bulunamadı — Adobe Acrobat yükleyin",
        shareReport = "Raporu Paylaş",
        notAvailable = "Mevcut Değil"
    )

    val arabic = Strings(
        // Auth
        welcomeTitle = "نيورال تشيك",
        welcomeSubtitle = "تشخيص الأجهزة مدعوم بالذكاء الاصطناعي",
        tagline = "الشبكات العصبية تفحص، العلم يقرر",
        loginButton = "تسجيل الدخول",
        registerButton = "إنشاء حساب",
        loginTitle = "مرحباً بعودتك",
        registerTitle = "إنشاء حساب",
        emailHint = "البريد الإلكتروني",
        passwordHint = "كلمة المرور",
        passConfirmHint = "تأكيد كلمة المرور",
        forgotPassword = "نسيت كلمة المرور؟",
        noAccount = "ليس لديك حساب؟ سجل الآن",
        hasAccount = "لديك حساب بالفعل؟ سجل دخول",
        selectLanguage = "اختر اللغة",
        // Registration
        step = "الخطوة",
        accountInfo = "معلومات الحساب",
        ownerInfo = "معلومات المالك",
        shopInfo = "معلومات المتجر",
        businessType = "نوع العمل",
        shopLogo = "شعار المحل",
        shopNameHint = "اسم المتجر",
        ownerNameHint = "اسم المالك",
        phoneHint = "رقم الهاتف",
        cityHint = "المدينة",
        districtHint = "المنطقة",
        addressHint = "العنوان",
        uploadingLogo = "جارٍ رفع الشعار...",
        continueButton = "المتابعة",
        backButton = "رجوع",
        finishButton = "إنهاء",
        // Dashboard
        dashboard = "لوحة التحكم",
        inventory = "المخزون",
        totalDevices = "إجمالي الأجهزة",
        inStock = "في المخزون",
        sold = "مباع",
        totalProfit = "صافي الربح",
        purchasePrice = "سعر الشراء",
        sellingPrice = "سعر البيع",
        markAsSold = "تعليم كمباع",
        generateQr = "توليد QR",
        logout = "تسجيل الخروج",
        scanCount = "عدد الفحوصات",
        // QR
        scanQrCode = "مسح رمز QR",
        qrExpiry = "ينتهي خلال: ",
        generateNewQr = "توليد رمز جديد",
        scanShopQr = "امسح رمز المحل",
        skipScan = "فحص بدون حساب",
        qrInvalid = "رمز غير صالح أو تم استخدامه مسبقاً",
        qrExpired = "انتهت صلاحية الرمز ⏰ اطلب رمزاً جديداً",
        qrValidForOneDevice = "صالح لجهاز واحد فقط",
        shareQr = "مشاركة الرمز",
        // Report
        reportTitle = "التقرير التقني",
        downloadPdf = "تحميل PDF",
        sharePdf = "مشاركة التقرير",
        overallScore = "التقييم الإجمالي",
        battery = "البطارية",
        performance = "الأداء",
        storage = "التخزين",
        security = "الأمان",
        // Scan
        startScan = "بدء الفحص",
        scanning = "جارٍ الفحص...",
        scanComplete = "اكتمل الفحص",
        // Common
        save = "حفظ",
        cancel = "إلغاء",
        back = "رجوع",
        loading = "جارٍ التحميل...",
        error = "خطأ",
        success = "تم بنجاح",
        delete = "حذف",
        edit = "تعديل",
        share = "مشاركة",
        close = "إغلاق",
        // SellDeviceDialog
        noteOptional = "ملاحظة (اختياري)",
        profit = "الربح:",
        currencySymbol = "TL",
        // ReportScreen
        yes = "نعم",
        no = "لا",
        didYouBuyDevice = "هل اشتريت هذا الجهاز؟",
        voltageDropChart = "انخفاض الفولطية تحت الضغط",
        cpuTempChart = "منحنى درجة حرارة المعالج",
        cpuFreqChart = "تردد المعالج والكبح الحراري",
        noVoltageReadings = "لا توجد قراءات فولطية.",
        noTempReadings = "لا توجد قراءات حرارية.",
        noFreqReadings = "لا توجد قراءات تردد.",
        // QrScanScreen
        noQrCodeQuestion = "لا يوجد رمز؟",
        qrScanError = "خطأ في قراءة الرمز",
        cameraPermissionRequired = "⚠️ يجب السماح بالوصول للكاميرا لمسح QR",
        // PdfSuccessDialog
        reportSaved = "تم حفظ التقرير",
        reportReadyIn = "جاهز في Downloads",
        openReport = "فتح التقرير",
        // GenerateQrScreen
        generateQrScreenTitle = "توليد رمز الفحص",
        // Dashboard
        generateQrForCustomer = "📲 توليد رمز QR للعميل",
        // WelcomeScreen
        orText = "أو",
        scanWithoutAccount = "فحص بدون حساب",
        // QR & Purchase (new keys only — scanQrCode/scanShopQr/qrValidForOneDevice already defined above)
        purchasePriceQuestion = "💰 بكم اشتريت هذا الجهاز؟",
        saveToInventory = "حفظ في المخزون",
        skip = "تخطي",
        // GenerateQrScreen
        scanQrToStart = "امسح هذا الرمز لبدء الفحص",
        qrExpiredLabel = "انتهت صلاحية الرمز",
        qrExpiresIn = "⏱️ ينتهي خلال:",
        shareQrCode = "مشاركة رمز QR",
        qrShareTextPrefix = "رمز الفحص:",
        // QrScanScreen
        scanQrSubtitle = "اطلب من صاحب المحل رمز QR\nلربط الفحص بحسابه",
        // ReportScreen
        addToStockSubtitle = "أضفه لمخزون محلك وتابع أرباحك",
        savedToInventoryToast = "✅ تم الحفظ في المخزون",
        // Security Warning
        securityWarning = "تحذير أمني",
        deviceRooted = "الجهاز مفتوح الصلاحيات (Rooted)",
        developerOptionsOn = "خيارات المطور مفعلة",
        adbWarning = "تصحيح USB مفعّل",

        // PDF
        errorOpeningPdf = "تعذر فتح الملف",
        errorSharingPdf = "تعذرت المشاركة",
        noPdfApp = "لا يوجد تطبيق لفتح PDF — قم بتثبيت Adobe Acrobat",
        shareReport = "مشاركة التقرير",
        notAvailable = "غير متوفر"
    )

    val english = Strings(
        // Auth
        welcomeTitle = "NeuralCheck",
        welcomeSubtitle = "AI-Powered Device Diagnostics",
        tagline = "Neural Networks Inspect, Science Decides",
        loginButton = "Login",
        registerButton = "Create Account",
        loginTitle = "Welcome Back",
        registerTitle = "Create Account",
        emailHint = "Email Address",
        passwordHint = "Password",
        passConfirmHint = "Confirm Password",
        forgotPassword = "Forgot Password?",
        noAccount = "Don't have an account? Register",
        hasAccount = "Already have an account? Login",
        selectLanguage = "Select Language",
        // Registration
        step = "Step",
        accountInfo = "Account Information",
        ownerInfo = "Owner Information",
        shopInfo = "Shop Information",
        businessType = "Business Type",
        shopLogo = "Shop Logo",
        shopNameHint = "Shop Name",
        ownerNameHint = "Owner Name",
        phoneHint = "Phone Number",
        cityHint = "City",
        districtHint = "District",
        addressHint = "Address",
        uploadingLogo = "Uploading Logo...",
        continueButton = "Continue",
        backButton = "Back",
        finishButton = "Finish",
        // Dashboard
        dashboard = "Dashboard",
        inventory = "Inventory",
        totalDevices = "Total Devices",
        inStock = "In Stock",
        sold = "Sold",
        totalProfit = "Net Profit",
        purchasePrice = "Purchase Price",
        sellingPrice = "Selling Price",
        markAsSold = "Mark as Sold",
        generateQr = "Generate QR",
        logout = "Logout",
        scanCount = "Scan Count",
        // QR
        scanQrCode = "Scan QR Code",
        qrExpiry = "Expires in: ",
        generateNewQr = "Generate New QR",
        scanShopQr = "Scan Shop QR Code",
        skipScan = "Continue Without Account",
        qrInvalid = "Invalid or already used QR code",
        qrExpired = "QR code expired. Request a new one ⏰",
        qrValidForOneDevice = "Valid for one device only",
        shareQr = "Share QR Code",
        // Report
        reportTitle = "Technical Report",
        downloadPdf = "Download PDF",
        sharePdf = "Share Report",
        overallScore = "Overall Score",
        battery = "Battery",
        performance = "Performance",
        storage = "Storage",
        security = "Security",
        // Scan
        startScan = "Start Scan",
        scanning = "Scanning...",
        scanComplete = "Scan Complete",
        // Common
        save = "Save",
        cancel = "Cancel",
        back = "Back",
        loading = "Loading...",
        error = "Error",
        success = "Success",
        delete = "Delete",
        edit = "Edit",
        share = "Share",
        close = "Close",
        // SellDeviceDialog
        noteOptional = "Note (optional)",
        profit = "Profit:",
        currencySymbol = "TL",
        // ReportScreen
        yes = "Yes",
        no = "No",
        didYouBuyDevice = "Did you buy this device?",
        voltageDropChart = "Voltage Drop During Stress Test",
        cpuTempChart = "CPU Temperature Curve",
        cpuFreqChart = "CPU Frequency & Throttling",
        noVoltageReadings = "No voltage readings available.",
        noTempReadings = "No temp readings available.",
        noFreqReadings = "No frequency readings available.",
        // QrScanScreen
        noQrCodeQuestion = "No QR code?",
        qrScanError = "Error reading QR code",
        cameraPermissionRequired = "⚠️ Camera permission required for QR scan",
        // PdfSuccessDialog
        reportSaved = "Report Saved",
        reportReadyIn = "ready in Downloads",
        openReport = "Open Report",
        // GenerateQrScreen
        generateQrScreenTitle = "Generate Scan QR Code",
        // Dashboard
        generateQrForCustomer = "📲 Generate QR for Customer",
        // WelcomeScreen
        orText = "or",
        scanWithoutAccount = "Scan Without Account",
        // QR & Purchase (new keys only — scanQrCode/scanShopQr/qrValidForOneDevice already defined above)
        purchasePriceQuestion = "💰 What is the purchase price?",
        saveToInventory = "Save to Inventory",
        skip = "Skip",
        // GenerateQrScreen
        scanQrToStart = "Scan this code to start scanning",
        qrExpiredLabel = "QR code expired",
        qrExpiresIn = "⏱️ Expires in:",
        shareQrCode = "Share QR Code",
        qrShareTextPrefix = "Scan code:",
        // QrScanScreen
        scanQrSubtitle = "Ask the shop owner for the QR code\nto link the scan to their account",
        // ReportScreen
        addToStockSubtitle = "Add it to your store stock and track profits",
        savedToInventoryToast = "✅ Saved to inventory",
        // Security Warning
        securityWarning = "Security Warning",
        deviceRooted = "Device is Rooted",
        developerOptionsOn = "Developer Options Enabled",
        adbWarning = "USB Debugging Enabled",

        // PDF
        errorOpeningPdf = "Could not open file",
        errorSharingPdf = "Sharing failed",
        noPdfApp = "No PDF app found — install Adobe Acrobat",
        shareReport = "Share Report",
        notAvailable = "N/A"
    )

    fun get(language: LanguageManager.Language): Strings = when (language) {
        LanguageManager.Language.TURKISH -> turkish
        LanguageManager.Language.ARABIC  -> arabic
        LanguageManager.Language.ENGLISH -> english
    }
}
