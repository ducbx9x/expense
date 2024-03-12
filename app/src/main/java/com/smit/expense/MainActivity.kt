package com.smit.expense
import android.Manifest.permission_group.CALENDAR
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.smit.expense.R
import kotlinx.coroutines.tasks.await


class MainActivity : AppCompatActivity() {
//    private var binding: ActivityMainBinding? = null
    private var showOneTapUI = true
    private var oneTapClient: SignInClient? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var signInRequest: BeginSignInRequest
    private var mGoogleSignInClient: GoogleSignInClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = Firebase.auth
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId("956595930282-913q0ani52qv65jick3vipkkctjctgqm.apps.googleusercontent.com")
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(CALENDAR))
            .requestServerAuthCode("956595930282-913q0ani52qv65jick3vipkkctjctgqm.apps.googleusercontent.com")
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        findViewById<SignInButton>(R.id.signGG)?.setOnClickListener {
            auth.signOut()
            mGoogleSignInClient?.signOut()
            mGoogleSignInClient?.signInIntent?.let {
                Log.w("klnflkadsnflaks", "startActivityForResult")
                startActivityForResult(it, 112)
            }
            /*if (showOneTapUI)
                lifecycleScope.launch(Dispatchers.Main) {
                    launchSignInIntent()
                }*/
        }
        auth.signOut()
        updateUI(null)

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 112) {
            Log.w("klnflkadsnflaks", "resultCode $requestCode")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {

            val account = completedTask.getResult(ApiException::class.java)
            val authCode = account.serverAuthCode
            Log.w("klnflkadsnflaks", "authCode=" + authCode)
//            getRefreshToken(authCode)
            Toast.makeText(this@MainActivity,"authCode=" + authCode, Toast.LENGTH_SHORT).show()
            // Signed in successfully, show authenticated UI.
            //updateUI(account)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("klnflkadsnflaks", "signInResult:failed code=" + e.statusCode)
            Toast.makeText(this@MainActivity,"false", Toast.LENGTH_SHORT).show()
            updateUI(null)
        }
    }

    private suspend fun launchSignInIntent() {
        showOneTapUI = false
        oneTapClient?.beginSignIn(signInRequest)?.await()?.let { it->
            val intentSenderRequest = IntentSenderRequest.Builder(it.pendingIntent).build()
            activityResultLauncher.launch(intentSenderRequest)
        }
    }

    private val activityResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            showOneTapUI = true
            if (result.resultCode == RESULT_OK) {
                try {
                    val credential = oneTapClient?.getSignInCredentialFromIntent(result.data)
                    val idToken = credential?.googleIdToken
                    when {
                        idToken != null -> {
                            // with Firebase.
                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                            auth.signInWithCredential(firebaseCredential)
                                .addOnCompleteListener(this) { task ->
                                    if (task.isSuccessful) {
                                        // Sign in success, update UI with the signed-in user's information
                                        Log.d("sdfsdfsf", "signInWithCredential:success")
                                        val user = auth.currentUser
                                        updateUI(user)
                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Log.w(
                                            "sdfsdfsf",
                                            "signInWithCredential:failure",
                                            task.exception
                                        )
                                        updateUI(null)
                                    }
                                }
                            Log.d("sdfsdfsf", "Got ID token.")
                        }

                        else -> {
                            // Shouldn't happen.
                            Log.d("sdfsdfsf", "No ID token!")
                        }
                    }
                } catch (e: ApiException) {

                }
            }
        }

    private fun updateUI(user: FirebaseUser?) {

    }

}
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var  mGoogleSignInClient: GoogleSignInClient
//    private val RC_SIGN_IN = 1
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//
//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestIdToken("AIzaSyCzWN9bGUcH2UG6UwFtyd4y4U0R9hw50LQ")
//            .requestEmail()
//            .build()
//        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
//        val btnLoginGG = findViewById<SignInButton>(R.id.signGG)
//
//        btnLoginGG.setOnClickListener {
//            val signInIntent = mGoogleSignInClient.signInIntent
//            startActivityForResult(signInIntent, RC_SIGN_IN)
//        }
//
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == RC_SIGN_IN) {
//            // The Task returned from this call is always completed, no need to attach
//            // a listener.
//            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
//            handleSignInResult(task)
//        }
//    }
//
//    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
//        try {
//            val account = completedTask.getResult(ApiException::class.java)
//            val intent = Intent(this@MainActivity, MainActivity2::class.java)
//            intent.putExtra("acc", account)
//            startActivity(intent)
//            // Signed in successfully, show authenticated UI.
//        } catch (e: ApiException) {
//            // The ApiException status code indicates the detailed failure reason.
//            // Please refer to the GoogleSignInStatusCodes class reference for more information.
//            Toast.makeText(this, "Lỗi đăng nhập: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun onStart() {
//        super.onStart()
//        val account = GoogleSignIn.getLastSignedInAccount(this)
//        if (account?.email!=null){
//            val intent = Intent(this@MainActivity, MainActivity2::class.java)
//            intent.putExtra("acc", account)
//            startActivity(intent)
//        }
//    }
//}