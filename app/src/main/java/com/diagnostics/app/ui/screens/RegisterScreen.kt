/*
 * Copyright (c) 2025 Hasan Oso. All Rights Reserved.
 * 
 * PROPRIETARY SOFTWARE - DO NOT DISTRIBUTE
 * 
 * This file is part of DiagnosticApp.
 * Unauthorized use is strictly prohibited.
 * 
 * Owner: Hasan Oso
 * Student ID: 230212925
 * Institution: OSTIM Technical University
 */
package com.diagnostics.app.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.diagnostics.app.data.models.auth.RegistrationData
import com.diagnostics.app.utils.LanguageManager
import com.diagnostics.app.utils.language.AppStrings
import com.diagnostics.app.utils.language.Strings
import com.diagnostics.app.viewmodel.AuthViewModel
import com.diagnostics.app.viewmodel.RegisterState
import androidx.compose.ui.res.stringResource
import com.diagnostics.app.R

/**
 * RegisterScreen — Multi-section registration form for shop accounts.
 * Collects account info, shop details, and branding options with a 4-step process.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    // Current Step
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 4

    // Form fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var instagram by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("") }

    val registerState by authViewModel.registerState.collectAsState()
    val logoUri by authViewModel.logoUri.collectAsState()
    val logoUrl by authViewModel.logoUrl.collectAsState()
    
    val context = LocalContext.current
    val currentLanguage = remember { LanguageManager.getLanguage(context) }
    val strings = AppStrings.get(currentLanguage)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { authViewModel.setLogoUri(it) }
    }

    val layoutDirection = if (currentLanguage == LanguageManager.Language.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr

    LaunchedEffect(registerState) {
        if (registerState is RegisterState.Success) {
            onRegisterSuccess()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text(strings.registerTitle, color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (currentStep > 1) currentStep-- else {
                                    authViewModel.resetRegisterState()
                                    onBackClick()
                                }
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0A0F))
                    )
                    LinearProgressIndicator(
                        progress = currentStep / 4f,
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0xFF1A1A2E),
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                    )
                }
            },
            containerColor = Color(0xFF0A0A0F)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${strings.step} $currentStep",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (currentStep) {
                    1 -> AccountStep(strings, email, { email = it }, password, { password = it }, passwordConfirm, { passwordConfirm = it })
                    2 -> OwnerInfoStep(strings, shopName, { shopName = it }, ownerName, { ownerName = it }, phoneNumber, { phoneNumber = it }, city, { city = it }, district, { district = it }, address, { address = it })
                    3 -> BusinessStep(strings, businessType, { businessType = it }, instagram, { instagram = it }, whatsapp, { whatsapp = it })
                    4 -> LogoStep(strings, logoUri, logoUrl) {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (registerState is RegisterState.Error) {
                    Text(
                        text = (registerState as RegisterState.Error).message,
                        color = Color(0xFFFF1744),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        if (currentStep < totalSteps) {
                            currentStep++
                        } else {
                            if (password == passwordConfirm) {
                                authViewModel.register(
                                    RegistrationData(
                                        email = email,
                                        password = password,
                                        ownerName = ownerName,
                                        phoneNumber = phoneNumber,
                                        shopNameEn = shopName,
                                        shopNameAr = shopName,
                                        shopNameTr = shopName,
                                        address = address,
                                        city = city,
                                        district = district,
                                        instagram = instagram,
                                        whatsapp = whatsapp
                                    )
                                )
                                // رفع الشعار بعد التسجيل إن وجد
                                logoUri?.let { uri ->
                                    val currentUser = authViewModel.authState.value
                                    // سيتم الرفع في LaunchedEffect بعد نجاح التسجيل
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = registerState !is RegisterState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF6200EA), Color(0xFF00E5FF))), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (registerState is RegisterState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            val buttonText = if (currentStep == totalSteps) strings.finishButton else strings.continueButton
                            Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StyledTextField(value: String, onValueChange: (String) -> Unit, label: String, isPassword: Boolean = false, isPhone: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp)),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else if (isPhone) KeyboardType.Phone else KeyboardType.Text,
            imeAction = ImeAction.Next
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = Color(0xFF2D2D44)
        )
    )
}

@Composable
fun AccountStep(strings: Strings, email: String, onEmail: (String) -> Unit, pass: String, onPass: (String) -> Unit, passConf: String, onPassConf: (String) -> Unit) {
    Text(strings.accountInfo, fontSize = 20.sp, color = Color.White, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(16.dp))
    StyledTextField(email, onEmail, strings.emailHint)
    Spacer(modifier = Modifier.height(16.dp))
    StyledTextField(pass, onPass, strings.passwordHint, isPassword = true)
    Spacer(modifier = Modifier.height(16.dp))
    StyledTextField(passConf, onPassConf, strings.passConfirmHint, isPassword = true)
}

@Composable
fun OwnerInfoStep(strings: Strings, shop: String, onShop: (String) -> Unit, owner: String, onOwner: (String) -> Unit, phone: String, onPhone: (String) -> Unit, city: String, onCity: (String) -> Unit, district: String, onDist: (String) -> Unit, add: String, onAdd: (String) -> Unit) {
    Text(strings.ownerInfo, fontSize = 20.sp, color = Color.White, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(16.dp))
    StyledTextField(shop, onShop, strings.shopNameHint)
    Spacer(modifier = Modifier.height(8.dp))
    StyledTextField(owner, onOwner, strings.ownerNameHint)
    Spacer(modifier = Modifier.height(8.dp))
    StyledTextField(phone, onPhone, strings.phoneHint, isPhone = true)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f)) { StyledTextField(city, onCity, strings.cityHint) }
        Box(modifier = Modifier.weight(1f)) { StyledTextField(district, onDist, strings.districtHint) }
    }
    Spacer(modifier = Modifier.height(8.dp))
    StyledTextField(add, onAdd, strings.addressHint)
}

@Composable
fun BusinessStep(strings: Strings, type: String, onType: (String) -> Unit, insta: String, onInsta: (String) -> Unit, wa: String, onWa: (String) -> Unit) {
    Text(strings.businessType, fontSize = 20.sp, color = Color.White, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(16.dp))
    StyledTextField(type, onType, stringResource(R.string.shop_type_hint))
    Spacer(modifier = Modifier.height(8.dp))
    StyledTextField(insta, onInsta, "Instagram (@handle)")
    Spacer(modifier = Modifier.height(8.dp))
    StyledTextField(wa, onWa, "WhatsApp Number", isPhone = true)
}

@Composable
fun LogoStep(strings: Strings, logoUri: Uri?, logoUrl: String? = null, onClick: () -> Unit) {
    Text(strings.shopLogo, fontSize = 20.sp, color = Color.White, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFF1A1A2E), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            // معاينة قبل الرفع — URI محلي
            logoUri != null -> {
                AsyncImage(
                    model = logoUri,
                    contentDescription = "Shop Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFF00E5FF), CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            // بعد الرفع — Base64
            logoUrl != null && logoUrl.startsWith("data:image") -> {
                val imageBytes = Base64.decode(
                    logoUrl.removePrefix("data:image/jpeg;base64,"),
                    Base64.DEFAULT
                )
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Shop Logo",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFF00E5FF), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            // لا يوجد شعار
            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2D2D44))
                            .border(2.dp, Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            tint = Color(0xFF00E5FF),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.upload_logo_hint), color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
