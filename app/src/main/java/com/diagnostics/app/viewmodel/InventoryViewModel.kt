package com.diagnostics.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diagnostics.app.data.models.DeviceScanData
import com.diagnostics.app.data.models.auth.DeviceStatus
import com.diagnostics.app.data.models.auth.InventoryDevice
import com.diagnostics.app.data.models.auth.ShopStats
import android.content.Context
import com.diagnostics.app.data.models.auth.ShopInfo
import com.diagnostics.app.data.repository.InventoryRepository
import com.diagnostics.app.utils.pdf.PdfReportGenerator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InventoryRepository()

    // countdown job — نحتاجه للإلغاء عند توليد QR جديد
    private var countdownJob: Job? = null

    private val _devices = MutableStateFlow<List<InventoryDevice>>(emptyList())
    val devices: StateFlow<List<InventoryDevice>> = _devices.asStateFlow()

    private val _stats = MutableStateFlow(ShopStats())
    val stats: StateFlow<ShopStats> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentShopId = MutableStateFlow<String?>(null)
    val currentShopId: StateFlow<String?> = _currentShopId.asStateFlow()

    // QR Token
    private val _qrToken = MutableStateFlow<String?>(null)
    val qrToken: StateFlow<String?> = _qrToken.asStateFlow()

    private val _qrExpiry = MutableStateFlow(0L)
    val qrExpiry: StateFlow<Long> = _qrExpiry.asStateFlow()

    private val _qrTimeLeft = MutableStateFlow(3600L)
    val qrTimeLeft: StateFlow<Long> = _qrTimeLeft.asStateFlow()

    private val _pdfState = MutableStateFlow<PdfState>(PdfState.Idle)
    val pdfState: StateFlow<PdfState> = _pdfState.asStateFlow()

    sealed class PdfState {
        object Idle : PdfState()
        object Loading : PdfState()
        data class Success(val filePath: String) : PdfState()
        data class Error(val message: String) : PdfState()
    }

    fun loadDashboard(shopId: String) {
        _currentShopId.value = shopId
        viewModelScope.launch {
            _isLoading.value = true
            repository.getDevices(shopId).collect { deviceList ->
                _devices.value = deviceList
                _stats.value = repository.getStats(shopId)
                _isLoading.value = false
            }
        }
    }

    fun loadStatsOnly(shopId: String) {
        viewModelScope.launch {
            _stats.value = repository.getStats(shopId)
        }
    }

    fun generateQrToken(shopId: String) {
        viewModelScope.launch {
            // 1. ألغِ الـ countdown القديم فوراً
            countdownJob?.cancel()

            // 2. ألغِ الـ token القديم في Firestore
            val oldToken = _qrToken.value
            if (!oldToken.isNullOrEmpty()) {
                repository.invalidateQrToken(oldToken, shopId)
            }

            // 3. أعد ضبط الـ state
            _qrToken.value = null
            _qrTimeLeft.value = 3600L
            _isLoading.value = true

            // 4. ولّد token جديد (batch يحذف القديم تلقائياً)
            val (newToken, expiry) = repository.generateQrToken(shopId)
            _qrToken.value = newToken
            _qrExpiry.value = expiry
            _isLoading.value = false

            // 5. ابدأ countdown جديد
            startQrCountdown(expiry)
        }
    }

    private fun startQrCountdown(expiresAt: Long) {
        countdownJob?.cancel()          // تأكيد الإلغاء
        countdownJob = viewModelScope.launch {
            while (isActive) {
                val remaining = (expiresAt - System.currentTimeMillis()) / 1000
                if (remaining <= 0) {
                    _qrTimeLeft.value = 0
                    _qrToken.value = null
                    break
                }
                _qrTimeLeft.value = remaining
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }

    suspend fun validateQrAndGetShopId(token: String, shopId: String): String? {
        _isLoading.value = true
        val validShopId = repository.validateQrToken(token, shopId)
        _isLoading.value = false
        if (validShopId != null) {
            _currentShopId.value = validShopId
            repository.invalidateQrToken(token, validShopId)
        }
        return validShopId
    }

    fun saveDeviceAfterScan(scanData: DeviceScanData, purchasePrice: Int, score: Int, shopId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val device = InventoryDevice(
                shopId = shopId,
                scanId = scanData.scanId,
                deviceBrand = scanData.deviceBrand,
                deviceModel = scanData.deviceModel,
                overallScore = score,
                scanDate = System.currentTimeMillis(),
                purchasePrice = purchasePrice,
                batteryHealth = scanData.batteryStressResult?.estimatedHealthPercent ?: 0,
                storageTotalGb = scanData.storageTotalGb,
                ramTotalMb = scanData.ramTotalMb,
                androidVersion = scanData.androidVersion
            )
            repository.saveDevice(shopId, device)
            _isLoading.value = false
        }
    }

    fun markAsSold(device: InventoryDevice, soldPrice: Int, buyerNote: String) {
        val shopId = _currentShopId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            repository.markAsSold(device, shopId, soldPrice, buyerNote)
                .onSuccess { loadDashboard(shopId) }
            _isLoading.value = false
        }
    }

    fun markAsInStock(device: InventoryDevice) {
        val shopId = _currentShopId.value ?: return
        viewModelScope.launch {
            repository.markAsInStock(device, shopId)
                .onSuccess { _stats.value = repository.getStats(shopId) }
        }
    }

    fun updatePrices(deviceId: String, purchase: Int, selling: Int, shopId: String) {
        viewModelScope.launch {
            repository.updatePrices(deviceId, shopId, purchase, selling)
        }
    }

    fun deleteDevice(deviceId: String, shopId: String) {
        viewModelScope.launch {
            repository.deleteDevice(deviceId, shopId)
        }
    }

    fun downloadReport(
        context: Context,
        device: InventoryDevice,
        shopInfo: ShopInfo?
    ) {
        viewModelScope.launch {
            _pdfState.value = PdfState.Loading
            try {
                // 1. جلب بيانات الفحص من Firestore
                val scanResult = repository.getScanData(device.scanId)
                if (scanResult.isFailure) {
                    _pdfState.value = PdfState.Error("لم يتم العثور على بيانات الفحص")
                    return@launch
                }
                val scanData = scanResult.getOrThrow()

                // 2. حساب الـ Score
                val score = device.overallScore

                // 3. توليد PDF
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val pdfGenerator = PdfReportGenerator(context)
                val filePath = pdfGenerator.generateReport(
                    scanData = scanData,
                    shopInfo = shopInfo,
                    shopId = uid
                ) ?: ""

                _pdfState.value = PdfState.Success(filePath)
            } catch (e: Exception) {
                _pdfState.value = PdfState.Error(e.message ?: "خطأ غير معروف")
            }
        }
    }

    fun resetPdfState() {
        _pdfState.value = PdfState.Idle
    }
}
