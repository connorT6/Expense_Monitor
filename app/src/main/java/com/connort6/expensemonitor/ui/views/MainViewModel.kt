package com.connort6.expensemonitor.ui.views

import android.app.Application
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.connort6.expensemonitor.config.collectionName
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

interface IMainViewModel {

    val status: StateFlow<Int>
    val loginAction: StateFlow<Int>

    fun startLogin()
    fun resetLoginAction()
}

private var mainCollection: CollectionReference? = null

fun mainCollection(): CollectionReference {
    if (mainCollection != null){
        return mainCollection!!
    } else {
        throw Exception("mainCollection is null")
    }
}

class MainViewModel(application: Application) : IMainViewModel,
    AndroidViewModel(application) {

    private val _status = MutableStateFlow(0) // loading: 0, authed: 1, not authed : -1
    override val status = _status.asStateFlow()

    private val _loginAction = MutableStateFlow(0)
    override val loginAction = _loginAction.asStateFlow()

    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager = CredentialManager.create(application)


    init {
        viewModelScope.launch {
            auth.addAuthStateListener {
                if (it.currentUser != null) {
                    mainCollection =
                        FirebaseFirestore.getInstance().collection(collectionName)
                            .document(it.currentUser!!.uid).collection("data")
                    _status.value = 1
                } else {
                    _status.value = -1
                }
            }
        }

        viewModelScope.launch {
            var prevState = 0
            status.collect { current ->
                if (prevState == current) {
                    return@collect
                }
                if (prevState == 1 && current == -1) {
                    _loginAction.value = -1
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
//                delay(5000)
//                signOut()
            } catch (e: GetCredentialException) {
                Log.e(
                    this.javaClass.name,
                    "Couldn't retrieve user's credentials: ${e.localizedMessage}"
                )
                _status.value = -1
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
            _status.value = -1
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, "Google sign in failed", e)
            _status.value = -1
        }

    }

    private fun signOut() {
        // Firebase sign out
        auth.signOut()

        // When a user signs out, clear the current user credential state from all credential providers.
        viewModelScope.launch {
            try {
                val clearRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearRequest)
            } catch (e: ClearCredentialException) {
                Log.e(
                    this.javaClass.name,
                    "Couldn't clear user's credentials: ${e.localizedMessage}"
                )
            }
        }
    }

    override fun resetLoginAction() {
        _loginAction.value == 0
    }
}


class MockMainViewModel : IMainViewModel {

    private val _status = MutableStateFlow(0) // loading: 0, authed: 1, not authed : -1
    override val status = _status.asStateFlow()

    private val _loginAction = MutableStateFlow(0)
    override val loginAction = _loginAction.asStateFlow()


    override fun startLogin() {

    }

    override fun resetLoginAction() {

    }
}