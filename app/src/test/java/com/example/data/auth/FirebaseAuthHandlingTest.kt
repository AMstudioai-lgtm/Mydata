package com.example.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.di.AppContainer
import com.example.viewmodel.AuthViewModel
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
    fun `googleSignInHelper renvoie Error au lieu de crasher sans configuration`() = runBlocking {
        assertNull(GoogleSignInHelper.resolveWebClientId(context))

        val result = GoogleSignInHelper.requestInteractiveSignIn(context)
        assertTrue(result is GoogleSignInResult.Error)
        assertNull(GoogleSignInHelper.attemptAutoSignIn(context))
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
    fun `appContainer reste sain sans google-services json`() {
        val container = AppContainer(context)
        assertFalse(container.authService.isConfigured)
        assertNull(container.syncRepository)
    }
}
