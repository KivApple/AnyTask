package id.pineapple.anytask.tasks

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import id.pineapple.anytask.R
import id.pineapple.recyclerviewutil.BaseViewHolder

class SeparatorViewHolder(
		private val adapter: TaskListAdapter,
		parent: ViewGroup
) : BaseViewHolder<TaskListAdapter.SeparatorMarker>(
		adapter.context, R.layout.item_task_separator, parent
), View.OnClickListener {
	private val completedTaskCountTextView: TextView = itemView.findViewById(R.id.completed_task_count_text_view)
	private val separatorImageView: ImageView = itemView.findViewById(R.id.task_separator_image_view)
	private var firstBind = true
	
	init {
		itemView.setOnClickListener(this)
	}
	
	override fun onClick(view: View) {
		adapter.completedTasksShown = !adapter.completedTasksShown
	}
	
	override fun doBind(item: TaskListAdapter.SeparatorMarker, oldItem: TaskListAdapter.SeparatorMarker?) {
		completedTaskCountTextView.text = adapter.context.resources.getQuantityString(
				R.plurals.completed_task_count, adapter.completedTaskCount,
				adapter.completedTaskCount
		)
		val targetRotation = if (adapter.completedTasksShown) 90.0f else 0.0f
		if (firstBind) {
			firstBind = false
			separatorImageView.rotation = targetRotation
		} else if (targetRotation != separatorImageView.rotation) {
			separatorImageView.animate()
					.rotation(targetRotation)
					.start()
		}
	}
}
