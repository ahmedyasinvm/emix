package com.emicollect.app.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthManager @Inject constructor() {
    
    // Replace with your actual OAuth Client ID
    private val CLIENT_ID = "390398090935-cofso24v8ptf6mc6dtqbtro8epgfp0lm.apps.googleusercontent.com"

    fun getSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        
        val client = GoogleSignIn.getClient(context, gso)
        return client.signInIntent
    }

    fun handleSignInResult(data: Intent?): GoogleSignInAccount {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return task.getResult(com.google.android.gms.common.api.ApiException::class.java)
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        val client = GoogleSignIn.getClient(context, gso)
        client.signOut().addOnCompleteListener {
            onComplete()
        }
    }
}
