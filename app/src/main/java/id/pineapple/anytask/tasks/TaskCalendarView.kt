package id.pineapple.anytask.tasks

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import id.pineapple.anytask.R
import id.pineapple.anytask.localDateToMonths
import id.pineapple.anytask.monthsToLocalDate
import id.pineapple.anytask.resolveColor
import id.pineapple.calendarview.MonthView
import org.joda.time.LocalDate
import java.util.*

class TaskCalendarView(context: Context, attrs: AttributeSet?):
		ViewPager(context, attrs), MonthView.OnDayClickedListener {
	private val months = context.resources.getStringArray(R.array.months)
	var selectedDate: LocalDate = LocalDate()
		set(value) {
			field = value
			val index = localDateToMonths(value)
			(adapter as Adapter).getMonthView(index)?.setSelectedDate(value.toDate())
			currentItem = index
		}
	private val model = TaskListPersistentModel(LocalDate())
	var onDateChangeListener: ((date: LocalDate) -> Unit)? = null
	private val priorityColorMap = mapOf(
			-1 to R.attr.completedPriorityDateBackground,
			0 to R.attr.normalPriorityDateBackground,
			1 to R.attr.mediumPriorityDateBackground,
			2 to R.attr.highPriorityDateBackground,
			3 to R.attr.highestPriorityDateBackground
	).mapValues {
		resolveColor(context, it.value)
	}
	
	constructor(context: Context): this(context, null)
	
	init {
		LayoutInflater.from(context).inflate(R.layout.fragment_calendar_view, this)
		adapter = Adapter()
	}
	
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		val childWidthSpec = MeasureSpec.makeMeasureSpec(
				View.getDefaultSize(0, widthMeasureSpec),
				MeasureSpec.EXACTLY
		)
		var maxPageHeight = 0
		for (i in 0 until childCount) {
			val child = getChildAt(i)
			child.measure(childWidthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
			if (child.measuredHeight > maxPageHeight) {
				maxPageHeight = child.measuredHeight
			}
		}
		val newHeightSpec = if (maxPageHeight > 0)
			MeasureSpec.makeMeasureSpec(maxPageHeight, MeasureSpec.EXACTLY)
		else
			heightMeasureSpec
		super.onMeasure(widthMeasureSpec, newHeightSpec)
	}
	
	override fun onDayClicked(day: Date) {
		selectedDate = LocalDate(day)
		onDateChangeListener?.invoke(selectedDate)
	}
	
	inner class Adapter: PagerAdapter() {
		private val views = mutableMapOf<Int, ViewGroup>()
		
		override fun getCount(): Int = Int.MAX_VALUE
		
		override fun getPageTitle(position: Int): CharSequence =
				monthsToLocalDate(position).let {
					if (it.year == LocalDate().year) {
						months[it.monthOfYear - 1]
					} else {
						"${months[it.monthOfYear - 1]} ${it.year}"
					}
				}
		
		override fun instantiateItem(container: ViewGroup, position: Int): Any {
			val view = LayoutInflater.from(context).inflate(
					R.layout.fragment_month_view, container, false
			) as ViewGroup
			view.findViewById<TextView>(R.id.month_title).text = getPageTitle(position)
			view.findViewById<MonthView>(R.id.month_view).let {
				it.date = monthsToLocalDate(position).toDate()
				it.setSelectedDate(selectedDate.toDate())
				it.setOnDayClickedListener(this@TaskCalendarView)
				model.fetchByDateRange(LocalDate(it.startDate), LocalDate(it.stopDate)) { r ->
					val maxPriorities = r.mapValues { pair ->
						pair.value.map { task ->
							if (task.completed) -1 else task.priority
						}.max()
					}
					it.dateColorAdapter = object : MonthView.DateColorAdapter() {
						override fun getDateColor(date: Date): Int =
								maxPriorities[LocalDate(date)].let { maxPriority ->
									if (maxPriority == null)
										0
									else
										priorityColorMap[maxPriority] ?: 0
								}
					}
				}
			}
			views[position] = view
			container.addView(view)
			return view
		}
		
		override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
			container.removeView(`object` as View)
			if (views[position] == `object`) {
				views.remove(position)
			}
		}
		
		fun getMonthView(position: Int): MonthView? = views[position]?.findViewById(R.id.month_view)
		
		override fun isViewFromObject(view: View, `object`: Any): Boolean = view === `object`
	}
}
