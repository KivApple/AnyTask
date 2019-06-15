package id.pineapple.recyclerviewutil

import android.content.Context
import android.view.ViewGroup

abstract class ViewHolderFactory<T: UniqueEntity>(val itemClass: Class<T>) {
	var viewType: Int = -1
		internal set
	
	abstract fun createViewHolder(context: Context, container: ViewGroup): BaseViewHolder<T>
	
	open fun isItemSupported(item: T) = true
	
	companion object {
		@JvmStatic
		fun <T: UniqueEntity>create(
				itemClass: Class<T>,
				creator: (context: Context, container: ViewGroup) -> BaseViewHolder<T>
		) = object : ViewHolderFactory<T>(itemClass) {
			override fun createViewHolder(context: Context, container: ViewGroup): BaseViewHolder<T> =
					creator(context, container)
		}
		
		@JvmStatic
		fun <T: UniqueEntity>create(
				itemClass: Class<T>,
				creator: (context: Context, container: ViewGroup) -> BaseViewHolder<T>,
				filter: (item: T) -> Boolean
		) = object : ViewHolderFactory<T>(itemClass) {
			override fun createViewHolder(context: Context, container: ViewGroup): BaseViewHolder<T> =
					creator(context, container)
			
			override fun isItemSupported(item: T): Boolean = filter(item)
		}
	}
}
