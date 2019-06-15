package id.pineapple.anytask.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import id.pineapple.anytask.R
import id.pineapple.anytask.fixLandscapeHeight
import kotlinx.android.synthetic.main.dialog_file_picker.*
import java.io.File

class FilePickerDialogFragment: BottomSheetDialogFragment(),
		FileTreeAdapter.OnFileSelectedListener {
	private var adapter: FileTreeAdapter? = null
	private var savedAdapterState: Bundle? = null
	private var currentDir: File? = null
	private var requireExisting = false
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments!!.let {
			requireExisting = it.getBoolean(ARG_REQUIRE_EXISTING)
		}
		savedAdapterState = savedInstanceState?.getBundle(STATE_ADAPTER_DATA)
	}
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		adapter?.let { adapter ->
			savedAdapterState = adapter.saveInstanceState()
			outState.putBundle(STATE_ADAPTER_DATA, savedAdapterState)
		}
		outState.putString(STATE_FILE_NAME, file_name_edit_text?.text?.toString())
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
			inflater.inflate(R.layout.dialog_file_picker, container, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		fixLandscapeHeight()
		files_recycler_view.isNestedScrollingEnabled = true
		files_recycler_view.setHasFixedSize(true)
		(files_recycler_view.layoutManager as LinearLayoutManager).isAutoMeasureEnabled = true
		select_button.isEnabled = false
		file_name_edit_text.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
			}
			
			override fun afterTextChanged(s: Editable) {
			}
			
			override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
				select_button.isEnabled = s.isNotBlank()
			}
		})
		if (savedInstanceState != null) {
			file_name_edit_text.setText(savedInstanceState.getString(STATE_FILE_NAME) ?: "")
		}
		file_name_edit_text.isFocusable = !requireExisting
		select_button.setOnClickListener {
			dismiss()
			val selectedFile = (currentDir ?: Environment.getExternalStorageDirectory())
					.resolve(file_name_edit_text.text.toString())
			targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, Intent().apply {
				putExtra("selectedFile", selectedFile.absolutePath)
			})
		}
		cancel_button.setOnClickListener {
			dismiss()
		}
	}
	
	override fun onDestroyView() {
		super.onDestroyView()
		adapter = null
	}
	
	override fun onStart() {
		super.onStart()
		if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
				PackageManager.PERMISSION_GRANTED) {
			adapter = null
			files_recycler_view?.adapter = null
			requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
					PERMISSION_REQUEST)
		} else if (adapter == null) {
			createFileTreeAdapter()
		}
	}
	
	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		when (requestCode) {
			PERMISSION_REQUEST -> {
				if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
					if (adapter == null) {
						createFileTreeAdapter()
					}
				} else {
					dismiss()
				}
			}
		}
	}
	
	private fun createFileTreeAdapter() {
		if (view == null) return
		adapter = FileTreeAdapter(
				context!!,
				Environment.getExternalStorageDirectory(),
				savedAdapterState
		).apply {
			onFileSelectedListener = this@FilePickerDialogFragment
			currentDir = selectedFile.let {
				if (it != null)
					if (it.isDirectory)
						it
					else
						it.parentFile
				else
					null
			}
			current_dir_text_view?.text =
					getString(R.string.in_folder,
							(currentDir ?: Environment.getExternalStorageDirectory()).absolutePath)
		}
		files_recycler_view.adapter = adapter
	}
	
	override fun onFileSelected(file: File) {
		if (file.isFile) {
			currentDir = file.parentFile
			file_name_edit_text.setText(file.name)
			file_name_edit_text.setSelection(file.name.length)
		} else {
			currentDir = file
			if (requireExisting) {
				file_name_edit_text.setText("")
			}
		}
		current_dir_text_view?.text = getString(R.string.in_folder, currentDir!!.absolutePath)
	}
	
	companion object {
		private const val ARG_REQUIRE_EXISTING = "require_existing"
		
		private const val STATE_ADAPTER_DATA = "adapter_data"
		private const val STATE_FILE_NAME = "file_name"
		
		private const val PERMISSION_REQUEST = 1
		
		@JvmStatic
		fun newInstance(requireExisting: Boolean): FilePickerDialogFragment =
				FilePickerDialogFragment().apply {
					arguments = Bundle().apply {
						putBoolean(ARG_REQUIRE_EXISTING, requireExisting)
					}
				}
	}
}
