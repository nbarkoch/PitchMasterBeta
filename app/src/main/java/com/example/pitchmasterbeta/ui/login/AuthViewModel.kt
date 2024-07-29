package com.example.pitchmasterbeta.ui.login

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.ForgotPasswordHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.VerificationHandler
import com.amazonaws.services.cognitoidentityprovider.model.PasswordResetRequiredException
import com.amazonaws.services.cognitoidentityprovider.model.SignUpResult
import com.amazonaws.services.cognitoidentityprovider.model.UsernameExistsException
import com.example.pitchmasterbeta.MainActivity.Companion.appContext
import com.example.pitchmasterbeta.services.AWSKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern


class AuthViewModel : ViewModel() {

    private var userPool: CognitoUserPool = CognitoUserPool(
        appContext,
        AWSKeys.USERPool.POOL_ID,
        AWSKeys.USERPool.CLIENT_ID,
        "",
        AWSKeys.MY_REGIONS
    )

    private var userSession: CognitoUserSession? = null

    fun login(
        username: String,
        password: String,
        onCompletion: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (username.isEmpty() || password.isEmpty()) {
            return
        }
        try {
            val user = userPool.getUser(username)
            user.getSessionInBackground(object : AuthenticationHandler {
                override fun onSuccess(userSession: CognitoUserSession, newDevice: CognitoDevice?) {
                    userSession.apply {
                        Log.d("login", userSession.toString())
                    }
                    this@AuthViewModel.userSession = userSession
                    onCompletion(userSession.idToken.jwtToken)
                }

                override fun getAuthenticationDetails(
                    authenticationContinuation: AuthenticationContinuation,
                    userId: String
                ) {

                    // Provide authentication details
                    val authenticationDetails = AuthenticationDetails(userId, password, null)
                    authenticationContinuation.setAuthenticationDetails(authenticationDetails)
                    authenticationContinuation.continueTask()
                }

                override fun getMFACode(continuation: MultiFactorAuthenticationContinuation) {
                    // Handle MFA if needed
                }

                override fun authenticationChallenge(continuation: ChallengeContinuation) {
                    continuation.apply {
                        Log.d("login", challengeName)
                    }

                }

                override fun onFailure(exception: Exception?) {
                    exception?.printStackTrace()

                    onFailure("Incorrect username or password.")

                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            onFailure("Something went wrong")
        }
    }


    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    fun checkLoginStatus(): Boolean {
        _isLoggedIn.value = userSession?.isValid ?: false
        return _isLoggedIn.value
    }

    fun logout(username: String, onCompletion: () -> Unit, onFailure: () -> Unit) {
        val user = userPool.getUser(username)
        user.globalSignOutInBackground(object : GenericHandler {
            override fun onSuccess() {
                onCompletion()
            }

            override fun onFailure(exception: Exception?) {
                exception?.printStackTrace()
                onFailure()
            }
        })
    }

    enum class PasswordReasoning {
        NOT_LONG_ENOUGH,
        MISSING_NUMBERS,
        MISSING_UPPER_CASE,
        MISSING_LOWER_CASE,
        PASSWORDS_DOES_NOT_MATCH
    }

//    enum class UsernameReasoning {
//        NOT_LONG_ENOUGH,
//    }
//
//    enum class EmailReasoning {
//        NO_VALID_FORMAT,
//        NOT_EXISTS,
//        IS_BLOCKED
//    }

    //    fun isValidPasswordFormat(password: String): Boolean {
//        val passwordREGEX = Pattern.compile("^" +
//                "(?=.*[0-9])" +         //at least 1 digit
//                "(?=.*[a-z])" +         //at least 1 lower case letter
//                "(?=.*[A-Z])" +         //at least 1 upper case letter
//                "(?=.*[a-zA-Z])" +      //any letter
//                "(?=\\S+$)" +           //no white spaces
//                ".{8,}" +               //at least 8 characters
//                "$")
//        return passwordREGEX.matcher(password).matches()
//    }
    fun isUsernameValid(username: String): String {
        var newValidation = ""
        if (username.isEmpty()) {
            return newValidation
        }
        if (username.length < 4) {
            newValidation = "user name is too short"
        }
        return newValidation
    }

    fun isPasswordValid(password: String, confirmedPassword: String): List<String> {
        val passwordValidationStackDraft = ArrayList<PasswordReasoning>()
        if (password.isEmpty()) {
            return emptyList()
        }
        //at least 8 characters
        if (!Pattern.compile("^.{8,}$").matcher(password).matches()) {
            passwordValidationStackDraft.add(PasswordReasoning.NOT_LONG_ENOUGH)
        }
        //at least 1 digit
        if (!Pattern.compile("^(?=.*[0-9]).+\$").matcher(password).matches()) {
            passwordValidationStackDraft.add(PasswordReasoning.MISSING_NUMBERS)
        }
        //at least 1 upper case letter
        if (!Pattern.compile("^(?=.*[A-Z]).+\$").matcher(password).matches()) {
            passwordValidationStackDraft.add(PasswordReasoning.MISSING_UPPER_CASE)
        }
        //at least 1 lower case letter
        if (!Pattern.compile("^(?=.*[a-z]).+\$").matcher(password).matches()) {
            passwordValidationStackDraft.add(PasswordReasoning.MISSING_LOWER_CASE)
        }
        if (confirmedPassword.isNotEmpty() && password != confirmedPassword) {
            passwordValidationStackDraft.add(PasswordReasoning.PASSWORDS_DOES_NOT_MATCH)
        }

        val passwordErrorStackDraft = passwordValidationStackDraft.map {
            when (it) {
                PasswordReasoning.NOT_LONG_ENOUGH -> "Password should be at least 8 long"
                PasswordReasoning.MISSING_NUMBERS -> "Password should have at least 1 digit"
                PasswordReasoning.MISSING_UPPER_CASE -> "Password should have at least 1 upper case letter"
                PasswordReasoning.MISSING_LOWER_CASE -> "Password should have at least 1 lower case letter"
                PasswordReasoning.PASSWORDS_DOES_NOT_MATCH -> "Passwords doesn't match"
            }
        }

        return passwordErrorStackDraft
    }

    fun isEmailValid(email: String): String {
        var newValidation = ""
        if (email.isEmpty()) {
            return newValidation
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            newValidation = "Invalid format of email address"
        }
        return newValidation
    }

    fun signUp(
        username: String,
        password: String,
        email: String,
        onCompletion: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            return
        }
        val userAttributes = CognitoUserAttributes()
        userAttributes.addAttribute("email", email)

        val signUpHandler = object : SignUpHandler {
            override fun onSuccess(user: CognitoUser, signUpResult: SignUpResult) {
                onCompletion()
            }

            override fun onFailure(exception: Exception?) {
                exception?.printStackTrace()
                var registrationMessage = "Couldn't register\n${exception?.message}"
                if (exception is UsernameExistsException) {
                    registrationMessage = "Couldn't register\nThe name is already in use"
                }
                if (exception is PasswordResetRequiredException) {
                    registrationMessage = "Couldn't register\nPassword reset required"
                }
                onFailure(registrationMessage)
            }
        }

        userPool.signUpInBackground(username, password, userAttributes, null, signUpHandler)
    }

    fun confirmVerificationCode(
        username: String,
        code: String,
        onCompletion: () -> Unit,
        onFailure: () -> Unit
    ) {
        val user = userPool.getUser(username)
        val confirmationHandler = object : GenericHandler {
            override fun onSuccess() {
                onCompletion()
            }

            override fun onFailure(exception: Exception?) {
                exception?.printStackTrace()
                onFailure()
            }
        }
        user.confirmSignUpInBackground(code, true, confirmationHandler)
    }

    fun resendVerificationToEmail(
        username: String,
        onCompletion: () -> Unit,
        onFailure: () -> Unit
    ) {
        val user = userPool.getUser(username)
        val resendConfirmationHandler = object : VerificationHandler {
            override fun onSuccess(verificationCodeDeliveryMedium: CognitoUserCodeDeliveryDetails?) {
                onCompletion()
            }

            override fun onFailure(exception: Exception?) {
                exception?.printStackTrace()
                onFailure()
            }
        }
        user.resendConfirmationCodeInBackground(resendConfirmationHandler)
    }

    private fun startForgotPassword(username: String) {
        val user = userPool.getUser(username)
        user.forgotPasswordInBackground(object : ForgotPasswordHandler {
            override fun onSuccess() {
                // Handle success, the verification code was sent successfully
                Log.d("ForgotPassword", "Verification code sent successfully")
                // Proceed to UI to allow user to enter verification code and new password
            }

            override fun getResetCode(continuation: ForgotPasswordContinuation?) {
                //
            }

            override fun onFailure(e: Exception?) {
                // Handle failure
                Log.e("ForgotPassword", "Error initiating forgot password", e)
            }
        })
    }

    private fun confirmNewPassword(
        username: String,
        verificationCode: String,
        newPassword: String
    ) {
        val user = userPool.getUser(username)
        user.confirmPassword(verificationCode, newPassword, object : ForgotPasswordHandler {
            override fun onSuccess() {
                // Handle success, the password was reset successfully
                Log.d("ForgotPassword", "Password reset successfully")
                // Proceed to login or other screen
            }

            override fun getResetCode(continuation: ForgotPasswordContinuation?) {
                //
            }

            override fun onFailure(e: Exception?) {
                // Handle failure
                Log.e("ForgotPassword", "Error resetting password", e)
            }
        })
    }

}