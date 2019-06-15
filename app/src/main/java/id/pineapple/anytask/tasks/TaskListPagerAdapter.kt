package id.pineapple.anytask.tasks

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.view.ViewGroup
import id.pineapple.anytask.R
import id.pineapple.anytask.daysToLocalDate
import org.joda.time.Days
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

class TaskListPagerAdapter(
		private val context: Context,
		fragmentManager: FragmentManager
) : FragmentStatePagerAdapter(fragmentManager) {
	private val dateFormatter = DateTimeFormat.shortDate()
	private val weekDayFormatter = DateTimeFormat.forPattern("EEEE")
	private val instances = mutableMapOf<Int, TaskListFragment>()
	
	override fun getCount(): Int = Int.MAX_VALUE
	
	override fun getPageTitle(position: Int): CharSequence? {
		val date = daysToLocalDate(position)
		val delta = Days.daysBetween(LocalDate(), date).days
		return dateFormatter.print(date) + "\n" +
				weekDayFormatter.print(date) + ", " + when {
			delta == 0 -> context.getString(R.string.today)
			delta == 1 -> context.getString(R.string.tomorrow)
			delta == -1 -> context.getString(R.string.yesterday)
			delta < 0 -> context.resources.getQuantityString(R.plurals.days_ago, -delta, -delta)
			else -> context.resources.getQuantityString(R.plurals.days_later, delta, delta)
		}
	}
	
	override fun getItem(position: Int): Fragment =
			TaskListFragment.newInstance(daysToLocalDate(position))
	
	override fun instantiateItem(container: ViewGroup, position: Int): Any =
			instances.getOrPut(position) {
				super.instantiateItem(container, position) as TaskListFragment
			}
	
	override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
		instances.remove(position)
		super.destroyItem(container, position, `object`)
	}
	
	fun getInstance(position: Int) = instances[position]
}
