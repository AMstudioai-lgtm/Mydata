package com.example.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.di.AppContainer
import com.example.viewmodel.AuthViewModel
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FirebaseAuthHandlingTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `firebaseAuthService ne crashe pas si FirebaseApp est absent`() = runBlocking {
        val service = FirebaseAuthService(
            context = context,
            authProvider = { throw IllegalStateException("Default FirebaseApp is not initialized") },
            configuredCheck = { false }
        )

        assertFalse(service.isConfigured)
        assertNull(service.currentUserId())
        assertNull(service.currentEmail())
        assertNull(service.getCachedSession())
        assertNull(service.observeAuthSession().first())

        val signInResult = service.signInWithGoogleIdToken("fake-token")
        assertTrue(signInResult.isFailure)

        service.signOut()
    }

    @Test
    fun `googleSignInHelper gere un contexte non Activity sans crasher`() = runBlocking {
        val webClientId = GoogleSignInHelper.resolveWebClientId(context)
        if (webClientId == null) {
            assertNull(GoogleSignInHelper.attemptAutoSignIn(context))
        } else {
            assertTrue(webClientId.isNotBlank())
        }

        val result = GoogleSignInHelper.requestInteractiveSignIn(context)
        assertTrue(result is GoogleSignInResult.Error)
    }

    @Test
    fun `authViewModel bascule en LoggedOut et signale l erreur sans crasher`() = runTest(testDispatcher) {
        val service = FirebaseAuthService(
            context = context,
            authProvider = { throw IllegalStateException("Default FirebaseApp is not initialized") },
            configuredCheck = { false }
        )
        val viewModel = AuthViewModel(service)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.LoggedOut, viewModel.authState.value)
        assertFalse(viewModel.isAuthConfigured)

        viewModel.attemptSilentSignIn(context)
        viewModel.signInWithGoogle(context)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isSubmitting.value)

        viewModel.signOut(context)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AuthState.LoggedOut, viewModel.authState.value)
    }

    @Test
    fun `appContainer reste sain avec ou sans google-services json`() {
        val container = AppContainer(context)
        val hasFirebaseApp = FirebaseApp.getApps(context).isNotEmpty()
        val hasWebClientId = GoogleSignInHelper.resolveWebClientId(context) != null

        assertEquals(hasFirebaseApp && hasWebClientId, container.authService.isConfigured)
        if (!hasFirebaseApp) {
            assertNull(container.syncRepository)
        } else {
            container.syncRepository
        }
    }
}
