package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.GoldAccent

enum class AuthMode {
    LOGIN,
    REGISTER,
    VERIFICATION
}

@Composable
fun AuthScreen(
    pending2FA: UserProfile?,
    onRegisterFirebase: (email: String, password: String, name: String, onVerificationSent: (String) -> Unit, onError: (String) -> Unit) -> Unit,
    onLoginFirebase: (email: String, password: String, onSuccess: (UserProfile) -> Unit, onUnverified: (String) -> Unit, onError: (String) -> Unit) -> Unit,
    onResendVerification: (email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onLoginPhone: (phone: String, name: String) -> Unit = { _, _ -> },
    onLoginGoogle: (email: String, name: String) -> Unit = { _, _ -> },
    onLoginApple: (email: String, name: String) -> Unit = { _, _ -> },
    onLoginMicrosoft: (email: String, name: String) -> Unit = { _, _ -> },
    onForgotPassword: (email: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _ -> },
    onVerify2FA: (code: String) -> Boolean = { true },
    onCancel2FA: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var verificationEmail by remember { mutableStateOf("") }

    // Input States
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Forgot Password States
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var forgotPasswordLoading by remember { mutableStateOf(false) }

    // UI Loading & Feedback States
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successNotice by remember { mutableStateOf<String?>(null) }

    // 2FA Dialog Code state
    var twoFactorCodeInput by remember { mutableStateOf("") }

    // Resend Dialog state
    var showResendDialog by remember { mutableStateOf(false) }
    var resendPasswordInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EmeraldGreenDark,
                        Color(0xFF0D1B2A),
                        Color(0xFF050B14)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo & Branding
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(GoldAccent, EmeraldGreenPrimary)
                        )
                    )
                    .border(3.dp, GoldAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "الخزنة الذكية",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "الخزنة الذكية 🔐",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GoldAccent
            )
            Text(
                text = "إدارة ميزانيتك وأصولك بأمان وموثوقية",
                fontSize = 12.sp,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    when (currentMode) {
                        AuthMode.VERIFICATION -> {
                            // ==========================================
                            // 📧 VERIFICATION SCREEN
                            // ==========================================
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .background(EmeraldGreenPrimary.copy(alpha = 0.15f), CircleShape)
                                        .border(2.dp, EmeraldGreenPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MarkEmailRead,
                                        contentDescription = "Email Verification",
                                        tint = EmeraldGreenPrimary,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "تأكيد البريد الإلكتروني",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Email Verification Required",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // EXACT USER-REQUESTED MESSAGE BOX
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = EmeraldGreenDark.copy(alpha = 0.08f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreenPrimary.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "“We have sent you a verification email to $verificationEmail. Please verify it and log in.”",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 20.sp
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = "“لقد أرسلنا رسالة تأكيد إلى $verificationEmail. يرجى تأكيد بريدك الإلكتروني ثم تسجيل الدخول.”",
                                            fontSize = 13.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 19.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x15FFA000), RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "بعد فتح الرابط في بريدك، اضغط زر تسجيل الدخول أدناه للمتابعة.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (successNotice != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = successNotice!!,
                                        fontSize = 12.sp,
                                        color = EmeraldGreenPrimary,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                if (errorMessage != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = errorMessage!!,
                                        fontSize = 12.sp,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // MANDATORY LOGIN BUTTON ON VERIFICATION SCREEN
                                Button(
                                    onClick = {
                                        errorMessage = null
                                        successNotice = null
                                        emailInput = verificationEmail
                                        currentMode = AuthMode.LOGIN
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = "تسجيل الدخول / Login",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Resend Verification Email Button
                                OutlinedButton(
                                    onClick = {
                                        errorMessage = null
                                        successNotice = null
                                        if (passwordInput.isNotBlank()) {
                                            isLoading = true
                                            onResendVerification(
                                                verificationEmail,
                                                passwordInput,
                                                {
                                                    isLoading = false
                                                    successNotice = "تمت إعادة إرسال رسالة التأكيد بنجاح! / Verification email resent!"
                                                },
                                                { error ->
                                                    isLoading = false
                                                    errorMessage = error
                                                }
                                            )
                                        } else {
                                            showResendDialog = true
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "إعادة إرسال رسالة التأكيد / Resend Email",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                TextButton(
                                    onClick = {
                                        errorMessage = null
                                        successNotice = null
                                        currentMode = AuthMode.REGISTER
                                    }
                                ) {
                                    Text(
                                        text = "تغيير البريد الإلكتروني / Change Email",
                                        fontSize = 12.sp,
                                        color = GoldAccent
                                    )
                                }
                            }
                        }

                        AuthMode.LOGIN -> {
                            // ==========================================
                            // 🔑 LOGIN SCREEN (EMAIL / PASSWORD)
                            // ==========================================
                            Text(
                                text = "تسجيل الدخول إلى حسابك",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sign in with your email & password",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = {
                                    emailInput = it
                                    errorMessage = null
                                },
                                label = { Text("البريد الإلكتروني / Email") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = {
                                    passwordInput = it
                                    errorMessage = null
                                },
                                label = { Text("كلمة المرور / Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Forgot Password Trigger
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        forgotPasswordEmail = emailInput.trim()
                                        showForgotPasswordDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "نسيت كلمة المرور؟ / Forgot Password?",
                                        fontSize = 11.5.sp,
                                        color = EmeraldGreenPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = errorMessage!!,
                                    fontSize = 12.sp,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    if (emailInput.isBlank() || passwordInput.isBlank()) {
                                        errorMessage = "يرجى إدخال البريد الإلكتروني وكلمة المرور"
                                        return@Button
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    onLoginFirebase(
                                        emailInput.trim(),
                                        passwordInput,
                                        { _ ->
                                            isLoading = false
                                            Toast.makeText(context, "تم تسجيل الدخول بنجاح! مرحباً بك", Toast.LENGTH_SHORT).show()
                                        },
                                        { unverifiedEmailAddress ->
                                            // RULE: If user logs in and email is not verified, block access and show verification screen
                                            isLoading = false
                                            verificationEmail = unverifiedEmailAddress
                                            currentMode = AuthMode.VERIFICATION
                                        },
                                        { error ->
                                            isLoading = false
                                            errorMessage = error
                                        }
                                    )
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("تسجيل الدخول / Log In", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ليس لديك حساب؟", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "إنشاء حساب جديد",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                    modifier = Modifier.clickable {
                                        errorMessage = null
                                        currentMode = AuthMode.REGISTER
                                    }
                                )
                            }
                        }

                        AuthMode.REGISTER -> {
                            // ==========================================
                            // 📝 REGISTER / SIGN UP SCREEN
                            // ==========================================
                            Text(
                                text = "إنشاء حساب جديد",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Create a New Account",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("الاسم الكامل / Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = {
                                    emailInput = it
                                    errorMessage = null
                                },
                                label = { Text("البريد الإلكتروني / Email") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = {
                                    passwordInput = it
                                    errorMessage = null
                                },
                                label = { Text("كلمة المرور (6 أحرف على الأقل)") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = confirmPasswordInput,
                                onValueChange = {
                                    confirmPasswordInput = it
                                    errorMessage = null
                                },
                                label = { Text("تأكيد كلمة المرور / Confirm Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = errorMessage!!,
                                    fontSize = 12.sp,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    val trimmedEmail = emailInput.trim()
                                    if (trimmedEmail.isBlank() || passwordInput.isBlank()) {
                                        errorMessage = "يرجى ملء جميع الحقول المطلوبة / Please fill all required fields"
                                        return@Button
                                    }
                                    if (passwordInput != confirmPasswordInput) {
                                        errorMessage = "كلمتا المرور غير متطابقتين / Passwords do not match"
                                        return@Button
                                    }
                                    if (passwordInput.length < 6) {
                                        errorMessage = "كلمة المرور يجب أن تكون 6 أحرف على الأقل / Password must be at least 6 characters"
                                        return@Button
                                    }

                                    isLoading = true
                                    errorMessage = null
                                    onRegisterFirebase(
                                        trimmedEmail,
                                        passwordInput,
                                        nameInput,
                                        { sentEmail ->
                                            // RULE: When a user registers with email/password, do not sign them in automatically.
                                            // Send a verification email and show a verification screen
                                            isLoading = false
                                            verificationEmail = sentEmail
                                            currentMode = AuthMode.VERIFICATION
                                        },
                                        { error ->
                                            isLoading = false
                                            errorMessage = error
                                        }
                                    )
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("إنشاء حساب / Register", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("لديك حساب بالفعل؟", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تسجيل الدخول",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreenPrimary,
                                    modifier = Modifier.clickable {
                                        errorMessage = null
                                        currentMode = AuthMode.LOGIN
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // RESEND VERIFICATION PASSWORD DIALOG
        if (showResendDialog) {
            AlertDialog(
                onDismissRequest = { showResendDialog = false },
                title = {
                    Text("إعادة إرسال رسالة التأكيد", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            text = "أدخل كلمة المرور الخاصة بحسابك لإعادة إرسال رابط التحقق إلى:\n$verificationEmail",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = resendPasswordInput,
                            onValueChange = { resendPasswordInput = it },
                            label = { Text("كلمة المرور") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (resendPasswordInput.isNotBlank()) {
                                showResendDialog = false
                                isLoading = true
                                onResendVerification(
                                    verificationEmail,
                                    resendPasswordInput,
                                    {
                                        isLoading = false
                                        successNotice = "تم إرسال رسالة التأكيد مرة أخرى إلى بريدك الإلكتروني!"
                                    },
                                    { err ->
                                        isLoading = false
                                        errorMessage = err
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                    ) {
                        Text("إرسال الآن")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showResendDialog = false }) {
                        Text("إلغاء")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // 2FA VERIFICATION STEP OVERLAY
        if (pending2FA != null) {
            AlertDialog(
                onDismissRequest = onCancel2FA,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "التحقق بخطوتين",
                            tint = GoldAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "التحقق بخطوتين (2FA)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "تم تفعيل حماية الخطوتين لحسابك. يرجى إدخال رمز الأمان المكون من 6 أرقام للتحقق والتسجيل:",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = twoFactorCodeInput,
                            onValueChange = { twoFactorCodeInput = it },
                            label = { Text("رمز التحقق (مثال: 123456)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val success = onVerify2FA(twoFactorCodeInput)
                            if (success) {
                                Toast.makeText(context, "تم التحقق بنجاح! مرحباً بك", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "رمز التحقق غير صحيح", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تحقق وتسجيل", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = onCancel2FA,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء / الدخول المباشر")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Forgot Password Dialog
        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!forgotPasswordLoading) showForgotPasswordDialog = false
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldGreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إعادة تعيين كلمة المرور", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "أدخل بريدك الإلكتروني وسنرسل لك رابطاً لإعادة تعيين كلمة المرور بأمان عبر Firebase Authentication:",
                            fontSize = 12.5.sp,
                            color = Color.Gray,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = forgotPasswordEmail,
                            onValueChange = { forgotPasswordEmail = it },
                            label = { Text("البريد الإلكتروني / Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (forgotPasswordEmail.isBlank()) {
                                Toast.makeText(context, "يرجى إدخال البريد الإلكتروني", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            forgotPasswordLoading = true
                            onForgotPassword(
                                forgotPasswordEmail.trim(),
                                {
                                    forgotPasswordLoading = false
                                    showForgotPasswordDialog = false
                                    Toast.makeText(
                                        context,
                                        "تم إرسال رابط إعادة التعيين إلى بريدك الإلكتروني بنجاح!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                { error ->
                                    forgotPasswordLoading = false
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        enabled = !forgotPasswordLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (forgotPasswordLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("إرسال الرابط", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showForgotPasswordDialog = false },
                        enabled = !forgotPasswordLoading
                    ) {
                        Text("إلغاء")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
