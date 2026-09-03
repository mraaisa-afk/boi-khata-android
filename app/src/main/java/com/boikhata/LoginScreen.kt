package com.boikhata

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.boikhata.core.designsystem.theme.BoiKhataTheme
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

/**
 * D40: LoginScreen — Phone OTP entry + verification.
 * NOTE: Actual OTP sending requires a real device + Firebase App Check + SHA-1.
 * The sandbox cannot verify runtime OTP behavior.
 */
@Composable
fun LoginScreen(
    onOtpVerified: () -> Unit,
) {
    var phone by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val otpSentMsg = stringResource(R.string.otp_sent)
    val verifyFailedMsg = stringResource(R.string.verify_failed)

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (verificationId == null) {
            // Phone entry phase
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.enter_phone)) },
                placeholder = { Text(stringResource(R.string.phone_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (phone.isBlank()) return@Button
                    isLoading = true
                    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                            isLoading = false
                            auth.signInWithCredential(credential)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) onOtpVerified()
                                }
                        }
                        override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                            isLoading = false
                            message = e.localizedMessage
                        }
                        override fun onCodeSent(vId: String, token: PhoneAuthProvider.ForceResendingToken) {
                            isLoading = false
                            verificationId = vId
                            message = otpSentMsg
                        }
                    }
                    val options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(context as Activity)
                        .setCallbacks(callbacks)
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)
                },
                enabled = !isLoading && phone.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.send_otp))
            }
        } else {
            // OTP entry phase
            OutlinedTextField(
                value = otpCode,
                onValueChange = { otpCode = it },
                label = { Text(stringResource(R.string.enter_otp)) },
                placeholder = { Text(stringResource(R.string.otp_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    isLoading = true
                    val credential = PhoneAuthProvider.getCredential(verificationId!!, otpCode)
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                onOtpVerified()
                            } else {
                                message = verifyFailedMsg
                            }
                        }
                },
                enabled = !isLoading && otpCode.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.verify_otp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { verificationId = null }) {
                Text(stringResource(R.string.resend_otp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) CircularProgressIndicator()
        message?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
