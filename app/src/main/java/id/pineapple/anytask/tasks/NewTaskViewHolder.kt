package id.pineapple.anytask.tasks

import android.view.View
import android.view.ViewGroup
import id.pineapple.anytask.R
import id.pineapple.recyclerviewutil.BaseViewHolder

class NewTaskViewHolder(
		private val adapter: TaskListAdapter,
		parent: ViewGroup
) : BaseViewHolder<TaskListAdapter.NewTaskMarker>(adapter.context, R.layout.item_new_task, parent),
		View.OnClickListener {
	init {
		itemView.setOnClickListener(this)
	}
	
	override fun onClick(view: View) {
		adapter.controller.add("")
	}
}
