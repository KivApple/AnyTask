package id.pineapple.anytask

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.dialog_confirm.*

class ConfirmDialogFragment: BottomSheetDialogFragment() {
	private lateinit var message: String
	private lateinit var positiveButtonTitle: String
	private lateinit var negativeButtonTitle: String
	private var positiveIntent: Intent? = null
	
	override fun onCreate(savedInstanceState: Bundle?) {
		arguments!!.let {
			message = it.getString(ARG_MESSAGE)!!
			positiveButtonTitle = it.getString(ARG_POSITIVE_BUTTON_TITLE)!!
			negativeButtonTitle = it.getString(ARG_NEGATIVE_BUTTON_TITLE)!!
			positiveIntent = it.getParcelable(ARG_INTENT)
		}
		super.onCreate(savedInstanceState)
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
			inflater.inflate(R.layout.dialog_confirm, container, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		fixLandscapeHeight()
		confirm_message.text = message
		confirm_positive_button.text = positiveButtonTitle
		confirm_negative_button.text = negativeButtonTitle
		confirm_positive_button.setOnClickListener {
			dismiss()
			targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK,
					positiveIntent)
		}
		confirm_negative_button.setOnClickListener {
			dismiss()
		}
	}
	
	companion object {
		private const val ARG_MESSAGE = "message"
		private const val ARG_POSITIVE_BUTTON_TITLE = "positive_button_title"
		private const val ARG_NEGATIVE_BUTTON_TITLE = "negative_button_title"
		private const val ARG_INTENT = "intent"
		
		@JvmStatic
		fun newInstance(message: String, positiveButtonTitle: String, negativeButtonTitle: String,
						intent: Intent? = null) =
				ConfirmDialogFragment().apply {
					arguments = Bundle().apply {
						putString(ARG_MESSAGE, message)
						putString(ARG_POSITIVE_BUTTON_TITLE, positiveButtonTitle)
						putString(ARG_NEGATIVE_BUTTON_TITLE, negativeButtonTitle)
						putParcelable(ARG_INTENT, intent)
					}
				}
	}
}
