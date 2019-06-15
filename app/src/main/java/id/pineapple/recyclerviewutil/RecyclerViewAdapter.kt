package id.pineapple.recyclerviewutil

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import java.util.*

abstract class RecyclerViewAdapter(
		private val context: Context
): RecyclerView.Adapter<BaseViewHolder<in UniqueEntity>>(), RecyclerViewAdapterInterface {
	override var items = mutableListOf<UniqueEntity>()
	private val viewHolderFactories = mutableListOf<ViewHolderFactory<in UniqueEntity>>()
	private val viewHolderFactoriesByItemClass = mutableMapOf<Class<*>, MutableList<ViewHolderFactory<in UniqueEntity>>>()
	private val viewHolders = mutableMapOf<Long, BaseViewHolder<in UniqueEntity>>()
	val bindedViewHolders: Sequence<BaseViewHolder<in UniqueEntity>> get() = viewHolders.values.asSequence()
	var recyclerView: RecyclerView? = null
		private set
	
	init {
		this.setHasStableIds(true)
	}
	
	override fun restoreInstanceState(savedState: Bundle?, callNotifyDataSetChanged: Boolean) {
		if (savedState == null) return
		items = savedState.getParcelableArrayList("items")!!
		if (callNotifyDataSetChanged) {
			notifyDataSetChanged()
		}
	}
	
	fun saveInstanceState() = Bundle().apply {
		saveInstanceState(this)
	}
	
	override fun saveInstanceState(outState: Bundle) {
		outState.putParcelableArrayList("items", items as ArrayList<out Parcelable>)
	}
	
	protected fun registerViewType(factory: ViewHolderFactory<out UniqueEntity>): Int {
		factory.viewType = viewHolderFactories.size
		viewHolderFactories.add(factory as ViewHolderFactory<in UniqueEntity>)
		viewHolderFactoriesByItemClass.getOrPut(factory.itemClass) {
			mutableListOf()
		}.add(factory)
		return factory.viewType
	}
	
	override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
		this.recyclerView = recyclerView
	}
	
	override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
		if (recyclerView === this.recyclerView) {
			this.recyclerView = null
		}
	}
	
	final override fun getItemCount(): Int = items.size
	
	override fun getItemId(position: Int): Long = items[position].id!!
	
	final override fun getItemViewType(position: Int): Int {
		val item = items[position]
		val factories = viewHolderFactoriesByItemClass[item::class.java] ?:
				throw IllegalStateException("No suitable view holder factory for item of class ${item::class.java}")
		factories.forEach { factory ->
			if (factory.isItemSupported(item)) {
				return factory.viewType
			}
		}
		throw IllegalStateException("Unable to find suitable view holder factory for item $item")
	}
	
	final override fun onCreateViewHolder(container: ViewGroup, viewType: Int): BaseViewHolder<in UniqueEntity> =
			if (viewType in 0..viewHolderFactories.lastIndex)
				viewHolderFactories[viewType].createViewHolder(context, container)
			else
				throw IllegalStateException("Invalid viewType=$viewType")
	
	override fun onBindViewHolder(viewHolder: BaseViewHolder<in UniqueEntity>, position: Int) {
		viewHolder.bind(items[position])
		viewHolders[items[position].id!!] = viewHolder
	}
	
	override fun onViewRecycled(viewHolder: BaseViewHolder<in UniqueEntity>) {
		val id = (viewHolder.item as? UniqueEntity)?.id
		if (viewHolder === viewHolders[id]) {
			viewHolders.remove(id)
		}
		viewHolder.onRecycled()
	}
	
	fun getGenericViewHolder(id: Long): BaseViewHolder<in UniqueEntity>? = viewHolders[id]
	
	inline fun <reified T>getViewHolder(id: Long): T? = getGenericViewHolder(id) as? T
	
	fun ensureItemVisible(id: Long, callback: (viewHolder: BaseViewHolder<*>) -> Unit) {
		val runnable = object : Runnable {
			private var delay = 10L
			
			override fun run() {
				val position = items.indexOfFirst { it.id == id }
				if (position >= 0) {
					val viewHolder = viewHolders[id]
					val visible = viewHolder != null && recyclerView?.layoutManager
							?.isViewPartiallyVisible(
									viewHolder.itemView,
									true,
									true
							) != false
					if (visible) {
						callback(viewHolder!!)
						return
					} else {
						recyclerView?.scrollToPosition(position)
					}
				}
				delay *= 2
				if (delay < 5000L) {
					recyclerView?.postDelayed(this, delay)
				}
			}
		}
		runnable.run()
	}
}
