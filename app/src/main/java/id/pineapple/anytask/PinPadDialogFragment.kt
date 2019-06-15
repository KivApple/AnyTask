package id.pineapple.anytask

import android.Manifest
import android.app.Dialog
import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.v4.app.ActivityCompat
import android.support.v4.app.DialogFragment
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.support.v4.os.CancellationSignal
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import id.pineapple.pinpadview.PinPadView
import kotlinx.android.synthetic.main.fragment_pin_pad.*
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

class PinPadDialogFragment : DialogFragment(), PinPadView.OnPinCodeEnteredListener {
	private lateinit var sharedPreferences: SharedPreferences
	val action: Action by lazy {
		Action.values()[arguments!!.getInt(ARG_ACTION)]
	}
	private lateinit var currentState: State
	private lateinit var newPinHash: String
	private var fingerprintManager: FingerprintManagerCompat? = null
	private var cryptoObject: FingerprintManagerCompat.CryptoObject? = null
	private var fingerprintHandler: FingerprintHandler? = null
	private var fingerprintCancellationSignal: CancellationSignal? = null
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_Light_PinPadDialog)
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
		val pinEnabled = pinEnabled(sharedPreferences)
		if (savedInstanceState == null) {
			currentState = when (action) {
				Action.CHECK -> if (pinEnabled) State.CHECK else State.FINISH
				Action.SET -> if (pinEnabled) State.CHECK else State.SET1
				Action.CLEAR -> if (pinEnabled) State.CHECK else State.FINISH
			}
			newPinHash = ""
		} else {
			currentState = State.values()[savedInstanceState.getInt(STATE_CURRENT)]
			newPinHash = savedInstanceState.getString(STATE_NEW_PIN_HASH) ?: ""
		}
		if (currentState == State.FINISH) {
			(context as MainActivity).showTaskList()
			return
		}
		if (sharedPreferences.getBoolean("enable_biometrics", true) &&
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.USE_FINGERPRINT) ==
					PackageManager.PERMISSION_GRANTED) {
				val manager = FingerprintManagerCompat.from(context!!)
				if (manager.isHardwareDetected) {
					if (manager.hasEnrolledFingerprints()) {
						val keyguardManager = context!!
								.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
						if (keyguardManager.isKeyguardSecure) {
							fingerprintManager = manager
						}
					}
				}
			}
		}
		isCancelable = false
	}
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putInt(STATE_CURRENT, currentState.ordinal)
		outState.putString(STATE_NEW_PIN_HASH, newPinHash)
	}
	
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
			super.onCreateDialog(savedInstanceState).apply {
				setOnKeyListener { _, keyCode, _ ->
					when (keyCode) {
						KeyEvent.KEYCODE_BACK -> if (action == Action.CHECK) {
							activity?.finish()
						} else {
							dismiss()
						}
						else -> return@setOnKeyListener false
					}
					true
				}
				if (action == Action.CHECK) {
					window?.setWindowAnimations(R.style.Animation_PinPadWindow)
				}
			}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
			inflater.inflate(R.layout.fragment_pin_pad, container, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		pin_pad_view.isShuffleButtons =
				sharedPreferences.getBoolean("shuffle_pin_pad_keys", false)
		pin_pad_view.setOnPinCodeEnteredListener(this)
		updateUiForState()
	}
	
	override fun onResume() {
		super.onResume()
		if (currentState == State.CHECK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
				&& fingerprintManager != null) {
			try {
				val cipher = Cipher.getInstance(arrayOf(
						KeyProperties.KEY_ALGORITHM_AES,
						KeyProperties.BLOCK_MODE_CBC,
						KeyProperties.ENCRYPTION_PADDING_PKCS7
				).joinToString("/"))
				val keyStore = KeyStore.getInstance("AndroidKeyStore")
				keyStore.load(null)
				if (!keyStore.containsAlias(KEY_NAME)) {
					val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
							"AndroidKeyStore")
					keyGenerator.init(KeyGenParameterSpec.Builder(
							KEY_NAME,
							KeyProperties.PURPOSE_ENCRYPT or
									KeyProperties.PURPOSE_DECRYPT
					)
							.setBlockModes(KeyProperties.BLOCK_MODE_CBC)
							.setUserAuthenticationRequired(true)
							.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
							.build())
					keyGenerator.generateKey()
				}
				val key = try {
					keyStore.getKey(KEY_NAME, null)
				} catch (e: GeneralSecurityException) {
					return
				}
				cipher.init(Cipher.ENCRYPT_MODE, key)
				cryptoObject = FingerprintManagerCompat.CryptoObject(cipher)
				fingerprintHandler = FingerprintHandler()
				fingerprintHandler!!.startAuth()
			} catch (e: Throwable) {
				Log.e(this::class.java.name, "Failed to initialize cipher", e)
				fingerprintManager = null
				fingerprintCancellationSignal = null
				cryptoObject = null
				fingerprintHandler = null
			}
		}
		updateUiForState()
	}
	
	override fun onPause() {
		fingerprintCancellationSignal?.cancel()
		fingerprintCancellationSignal = null
		fingerprintHandler = null
		super.onPause()
	}
	
	override fun onPinCodeEntered(pinCode: String?) {
		when (currentState) {
			State.CHECK -> if (pinCode == null || hashPin(pinCode) ==
					sharedPreferences.getString("pin_hash", "")!!) {
				pin_pad_view?.showFilledDots()
				pin_pad_view?.postDelayed({
					when (action) {
						Action.CHECK -> {
							(context as MainActivity).showTaskList()
							dismiss()
						}
						Action.SET -> {
							currentState = State.SET1
							updateUiForState()
						}
						Action.CLEAR -> {
							sharedPreferences.edit()
									.remove("pin_hash")
									.apply()
							dismiss()
						}
					}
				}, AUTH_DELAY)
			} else {
				currentState = State.CHECK_ERROR
				updateUiForState()
			}
			State.SET1 -> {
				newPinHash = hashPin(pinCode!!)
				pin_pad_view.showFilledDots()
				pin_pad_view.postDelayed({
					currentState = State.SET2
					updateUiForState()
				}, AUTH_DELAY)
			}
			State.SET2 -> {
				if (hashPin(pinCode!!) == newPinHash) {
					sharedPreferences.edit()
							.putString("pin_hash", newPinHash)
							.apply()
					pin_pad_view.showFilledDots()
					pin_pad_view.postDelayed({
						dismiss()
					}, AUTH_DELAY)
				} else {
					newPinHash = ""
					currentState = State.SET_ERROR
					updateUiForState()
				}
			}
			State.CHECK_ERROR, State.SET_ERROR, State.FINISH -> throw IllegalStateException()
		}
	}
	
	override fun onPinCodeCancel() {
		if (action == Action.CHECK) {
			activity?.finish()
		} else {
			dismiss()
		}
	}
	
	private fun updateUiForState() {
		when (currentState) {
			State.CHECK -> when (action) {
				Action.SET, Action.CLEAR -> {
					hint_label.text = getString(R.string.enter_old_pin)
				}
				Action.CHECK -> {
					hint_label.text = null
				}
			}
			State.CHECK_ERROR -> {
				hint_label.text = getString(R.string.wrong_pin)
				pin_pad_view.isEnabled = false
				pin_pad_view.isErrorState = true
				pin_pad_view.postDelayed({
					currentState = State.CHECK
					updateUiForState()
				}, ERROR_DELAY)
				return
			}
			State.SET1 -> if (action == Action.SET) {
				hint_label.text = getString(R.string.enter_new_pin)
			} else {
				hint_label.text = null
			}
			State.SET2 -> if (action == Action.SET) {
				hint_label.text = getString(R.string.enter_new_pin_again)
			} else {
				hint_label.text = null
			}
			State.SET_ERROR -> {
				hint_label.text = getString(R.string.pin_does_not_match)
				pin_pad_view.isEnabled = false
				pin_pad_view.isErrorState = true
				pin_pad_view.postDelayed({
					currentState = State.SET1
					updateUiForState()
				}, ERROR_DELAY)
				return
			}
			State.FINISH -> {
			}
		}
		fingerprint_icon.visibility = if (currentState == State.CHECK &&
				fingerprintHandler != null) View.VISIBLE else View.GONE
		pin_pad_view.clearPinCode()
		pin_pad_view.isErrorState = false
		pin_pad_view.isEnabled = true
	}
	
	enum class Action {
		CHECK,
		SET,
		CLEAR
	}
	
	private enum class State {
		CHECK,
		CHECK_ERROR,
		SET1,
		SET2,
		SET_ERROR,
		FINISH
	}
	
	inner class FingerprintHandler : FingerprintManagerCompat.AuthenticationCallback() {
		fun startAuth() {
			fingerprintCancellationSignal = CancellationSignal()
			fingerprintManager!!.authenticate(cryptoObject, 0, fingerprintCancellationSignal,
					this, null)
		}
		
		override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult) {
			onPinCodeEntered(null)
		}
		
		override fun onAuthenticationFailed() {
			hint_label?.text = getString(R.string.fingerprint_auth_failed)
		}
		
		override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
			hint_label?.text = errString
		}
		
		override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
			hint_label?.text = helpString
		}
	}
	
	companion object {
		private const val ARG_ACTION = "action"
		
		private const val STATE_CURRENT = "current_state"
		private const val STATE_NEW_PIN_HASH = "new_pin_hash"
		
		private const val ERROR_DELAY = 1000L
		private const val AUTH_DELAY = 250L
		
		private const val KEY_NAME = "keyForFingerprintAuth"
		
		fun newInstance(action: Action): PinPadDialogFragment =
				PinPadDialogFragment().apply {
					arguments = Bundle().apply {
						putInt(ARG_ACTION, action.ordinal)
					}
				}
		
		fun pinEnabled(sharedPreferences: SharedPreferences): Boolean =
				sharedPreferences.getString("pin_hash", "")?.isNotEmpty() == true
		
		fun hashPin(pin: String): String {
			val digest = MessageDigest.getInstance("SHA-512")
			digest.update(pin.toByteArray())
			return digest.digest().joinToString {
				it.toString(16).padStart(2, '0')
			}
		}
	}
}
