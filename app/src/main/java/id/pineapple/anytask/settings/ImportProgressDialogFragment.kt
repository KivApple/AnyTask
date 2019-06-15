package id.pineapple.anytask.settings

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.work.WorkManager
import androidx.work.WorkStatus
import id.pineapple.anytask.R
import id.pineapple.anytask.fixLandscapeHeight
import java.util.*

class ImportProgressDialogFragment: BottomSheetDialogFragment(), Observer<WorkStatus> {
	private lateinit var workerId: UUID
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		arguments!!.let {
			workerId = it.getSerializable(ARG_WORKER_ID) as UUID
		}
		isCancelable = false
		WorkManager.getInstance().getStatusByIdLiveData(workerId).observe(this, this)
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
			inflater.inflate(R.layout.dialog_import_progress, container, false)
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		fixLandscapeHeight()
	}
	
	override fun onChanged(status: WorkStatus?) {
		if (status != null && status.state.isFinished) {
			dismiss()
		}
	}
	
	companion object {
		private const val ARG_WORKER_ID = "worker_id"
		
		@JvmStatic
		fun newInstance(workerId: UUID) = ImportProgressDialogFragment().apply {
			arguments = Bundle().apply {
				putSerializable(ARG_WORKER_ID, workerId)
			}
		}
	}
}
