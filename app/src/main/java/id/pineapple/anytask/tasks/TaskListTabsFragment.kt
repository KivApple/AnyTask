package id.pineapple.anytask.tasks

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import id.pineapple.anytask.ActionBarCustomViewProvider
import id.pineapple.anytask.R
import id.pineapple.anytask.daysToLocalDate
import id.pineapple.anytask.localDateToDays
import kotlinx.android.synthetic.main.fragment_task_list_tabs.*
import org.joda.time.LocalDate

class TaskListTabsFragment : Fragment(), ActionBarCustomViewProvider,
		ViewPager.OnPageChangeListener {
	private var initialDate: LocalDate? = null
		get() =
			if (field == null && arguments != null)
				daysToLocalDate(arguments!!.getInt(ARG_INITIAL_DATE))
			else
				field
	val currentDate: LocalDate?
		get() =
			if (task_list_view_pager != null)
				daysToLocalDate(task_list_view_pager!!.currentItem)
			else
				initialDate
	private var pagerAdapter: TaskListPagerAdapter? = null
	val taskListFragment: TaskListFragment?
		get() = if (task_list_view_pager != null)
			pagerAdapter?.getInstance(task_list_view_pager.currentItem)
		else
			null
	private var curPageIndex: Int = -1
	private var pageScrollDelta: Int? = null
	private var curPageTitleIndex: Int = -1
	private var nextPageTitleIndex: Int = -1
	private var actionBarCustomView: ViewGroup? = null
	private var actionBarNextButton: ImageButton? = null
	private var actionBarPrevButton: ImageButton? = null
	private var actionBarTitleTextView: TextView? = null
	private var actionBarSubtitleTextView: TextView? = null
	private var actionBarTitleContainer: ViewGroup? = null
	private var actionBarNextTitleTextView: TextView? = null
	private var actionBarNextSubtitleTextView: TextView? = null
	private var actionBarNextTitleContainer: ViewGroup? = null
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		initialDate = initialDate
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
							  savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_task_list_tabs, container, false)
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		pagerAdapter = TaskListPagerAdapter(view.context, childFragmentManager)
		task_list_view_pager.adapter = pagerAdapter
		task_list_view_pager.currentItem = localDateToDays(initialDate!!)
		curPageIndex = task_list_view_pager.currentItem
		task_list_view_pager.addOnPageChangeListener(this)
		updateActionBar()
	}
	
	override fun onDestroyView() {
		super.onDestroyView()
		pagerAdapter = null
	}
	
	override fun onDetach() {
		actionBarNextButton = null
		actionBarPrevButton = null
		actionBarTitleTextView = null
		actionBarSubtitleTextView = null
		actionBarTitleContainer = null
		actionBarCustomView = null
		actionBarNextTitleTextView = null
		actionBarNextSubtitleTextView = null
		actionBarNextTitleContainer = null
		super.onDetach()
	}
	
	override fun onPageScrollStateChanged(state: Int) {
		when (state) {
			ViewPager.SCROLL_STATE_DRAGGING -> {
				actionBarNextButton?.animate()?.alpha(0.4f)?.start()
				actionBarPrevButton?.animate()?.alpha(0.4f)?.start()
				pagerAdapter?.getInstance(curPageIndex)?.let {
					it.finishTaskEdit()
					it.finishActionMode()
				}
				curPageIndex = -1
				pageScrollDelta = 0
			}
			ViewPager.SCROLL_STATE_SETTLING -> {
				actionBarNextButton?.animate()?.alpha(1.0f)?.start()
				actionBarPrevButton?.animate()?.alpha(1.0f)?.start()
				curPageIndex = task_list_view_pager?.currentItem ?: -1
			}
			ViewPager.SCROLL_STATE_IDLE -> {
				actionBarNextTitleContainer?.visibility = View.GONE
				curPageTitleIndex = -1
				nextPageTitleIndex = -1
			}
		}
	}
	
	override fun onPageScrolled(position: Int, offset: Float, positionOffsetPixels: Int) {
		updateTitles(if (offset < 0.9f) position else position + 1, -offset)
		updateNextTitles(position + 1, 1.0f - offset)
		if (pageScrollDelta == 0) {
			pageScrollDelta = if (offset > 0.5f) -1 else 1
			actionBarNextTitleContainer?.visibility = View.VISIBLE
		}
	}
	
	override fun onPageSelected(position: Int) {
	}
	
	override fun onCreateActionBarCustomView(inflater: LayoutInflater, parent: ViewGroup): View {
		actionBarCustomView = inflater.inflate(
				R.layout.toolbar_task_list, parent, false
		) as ViewGroup
		actionBarCustomView!!.let { view ->
			actionBarNextButton = view.findViewById(R.id.task_list_go_forward_button)
			actionBarNextButton!!.setOnClickListener {
				if (task_list_view_pager.currentItem < Int.MAX_VALUE) {
					task_list_view_pager.currentItem = task_list_view_pager.currentItem + 1
				}
			}
			actionBarPrevButton = view.findViewById(R.id.task_list_go_backward_button)
			actionBarPrevButton!!.setOnClickListener {
				if (task_list_view_pager.currentItem > 0) {
					task_list_view_pager.currentItem = task_list_view_pager.currentItem - 1
				}
			}
			actionBarTitleTextView = view.findViewById(R.id.task_list_title)
			actionBarSubtitleTextView = view.findViewById(R.id.task_list_subtitle)
			actionBarTitleContainer = view.findViewById(R.id.task_list_title_container)
			actionBarNextTitleTextView = view.findViewById(R.id.task_list_next_title)
			actionBarNextSubtitleTextView = view.findViewById(R.id.task_list_next_subtitle)
			actionBarNextTitleContainer = view.findViewById(R.id.task_list_next_title_container)
			curPageTitleIndex = -1
			nextPageTitleIndex = -1
			updateActionBar()
			actionBarTitleContainer!!.setOnClickListener {
				TaskCalendarDialogFragment.newInstance(currentDate!!)
						.show((context as FragmentActivity).supportFragmentManager, null)
			}
			return view
		}
	}
	
	private fun updateActionBar() {
		if (pagerAdapter != null && actionBarCustomView != null) {
			updateTitles(task_list_view_pager.currentItem, 0.0f)
		}
	}
	
	private fun updateTitles(position: Int, offset: Float) {
		if (curPageTitleIndex != position) {
			val titles = pagerAdapter!!.getPageTitle(position)!!.split('\n', limit = 2)
			actionBarTitleTextView!!.text = titles.first()
			actionBarSubtitleTextView!!.text = titles.lastOrNull()
			actionBarSubtitleTextView!!.visibility =
					if (titles.size > 1) View.VISIBLE else View.GONE
			curPageTitleIndex = position
		}
		actionBarTitleContainer?.translationX = offset * actionBarCustomView!!.width
	}
	
	private fun updateNextTitles(position: Int, offset: Float) {
		if (nextPageTitleIndex != position) {
			val titles = pagerAdapter!!.getPageTitle(position)!!
					.split('\n', limit = 2)
			actionBarNextTitleTextView!!.text = titles.first()
			actionBarNextSubtitleTextView!!.text = titles.lastOrNull()
			actionBarNextSubtitleTextView!!.visibility =
					if (titles.size > 1) View.VISIBLE else View.GONE
			nextPageTitleIndex = position
		}
		actionBarNextTitleContainer?.translationX = offset * actionBarCustomView!!.width
	}
	
	companion object {
		private const val ARG_INITIAL_DATE = "initial_date"
		
		@JvmStatic
		fun newInstance(initialDate: LocalDate) =
				TaskListTabsFragment().apply {
					arguments = Bundle().apply {
						putInt(ARG_INITIAL_DATE, localDateToDays(initialDate))
					}
				}
	}
}
