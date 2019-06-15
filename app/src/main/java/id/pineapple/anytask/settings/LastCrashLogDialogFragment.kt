package id.pineapple.anytask.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import id.pineapple.anytask.App
import id.pineapple.anytask.R
import id.pineapple.anytask.fixLandscapeHeight
import kotlinx.android.synthetic.main.dialog_last_crash_log.*
import java.io.File
import java.io.IOException

class LastCrashLogDialogFragment: BottomSheetDialogFragment() {
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
			inflater.inflate(R.layout.dialog_last_crash_log, container, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		fixLandscapeHeight()
		crash_log_text_view.text = try {
			File(App.appFilesDir, App.LAST_EXCEPTION_LOG_NAME).readText()
		} catch (e: IOException) {
			""
		}
		close_button.setOnClickListener {
			dismiss()
		}
		copy_button.setOnClickListener {
			dismiss()
			val clipboard = activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as
					ClipboardManager
			clipboard.primaryClip = ClipData.newPlainText("text", crash_log_text_view.text)
			Toast.makeText(activity, R.string.copied, Toast.LENGTH_SHORT).show()
		}
	}
	
	companion object {
		@JvmStatic
		fun newInstance() = LastCrashLogDialogFragment()
	}
}
