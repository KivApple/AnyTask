package id.pineapple.anytask

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.ViewGroup
import android.widget.TextView
import id.pineapple.recyclerviewutil.BaseViewHolder
import id.pineapple.recyclerviewutil.UniqueEntity
import id.pineapple.recyclerviewutil.ViewHolderFactory

class SimpleTextViewHolder(
		context: Context,
		container: ViewGroup
): BaseViewHolder<SimpleTextViewHolder.Marker>(
		context, R.layout.item_simple_text, container
) {
	private val textView: TextView = itemView.findViewById(R.id.text_view)
	
	init {
		(itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.isFullSpan = true
	}
	
	override fun doBind(item: Marker, oldItem: Marker?) {
		textView.text = item.text
	}
	
	data class Marker(
			override val id: Long?,
			var text: String
	): UniqueEntity {
		override fun describeContents(): Int = 0
		
		override fun writeToParcel(parcel: Parcel, flags: Int) {
			parcel.writeLong(id ?: 0L)
			parcel.writeString(text)
		}
		
		companion object CREATOR : Parcelable.Creator<Marker> {
			override fun createFromParcel(parcel: Parcel): Marker = Marker(
					parcel.readLong().let { if (it != 0L) it else 0L },
					parcel.readString()!!
			)
			
			override fun newArray(size: Int): Array<Marker?> = arrayOfNulls(size)
		}
	}
	
	class Factory: ViewHolderFactory<Marker>(Marker::class.java) {
		override fun createViewHolder(context: Context, container: ViewGroup) =
				SimpleTextViewHolder(context, container)
	}
}
