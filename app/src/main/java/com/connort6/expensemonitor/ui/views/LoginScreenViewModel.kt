package com.connort6.expensemonitor.ui.views

import android.app.Application
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

interface ILoginScreenViewModel {

    val status: StateFlow<Int>

    fun startLogin()
}


class LoginScreenViewModel(application: Application) : ILoginScreenViewModel,
    AndroidViewModel(application) {

    private val _status = MutableStateFlow(0) // loading: 0, authed: 1, not authed : -1
    override val status = _status.asStateFlow()


    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager = CredentialManager.create(application)


    init {
        viewModelScope.launch {
            auth.addAuthStateListener {
                if (it.currentUser != null) {
                    _status.value = 1
                } else {
                    _status.value = -1
                }
            }
        }
    }


    override fun startLogin() {
        _status.value = 0
        viewModelScope.launch {
            val googleIdOption = GetGoogleIdOption.Builder()
                // Your server's client ID, not your Android client ID.
                .setServerClientId("634072896448-j2035nol1k134gqn0l49put5nm5a8bn6.apps.googleusercontent.com")
                // Only show accounts previously used to sign in.
                .setFilterByAuthorizedAccounts(false)
                .build()


            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                // Launch Credential Manager UI
                val result = credentialManager.getCredential(
                    context = application,
                    request = request
                )

                // Extract credential from the result returned by Credential Manager
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(
                    this.javaClass.name,
                    "Couldn't retrieve user's credentials: ${e.localizedMessage}"
                )
            }

        }
    }

    private suspend fun handleSignIn(credential: Credential) {
        // Check if credential is of type Google ID
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            // Create Google ID Token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            // Sign in to Firebase with using the token
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.w(this.javaClass.name, "Credential is not of type Google ID!")
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, "Google sign in failed", e)
        }

    }
}


class MockLoginViewModel : ILoginScreenViewModel {

    private val _status = MutableStateFlow(0) // loading: 0, authed: 1, not authed : -1
    override val status = _status.asStateFlow()


    override fun startLogin() {

    }
}