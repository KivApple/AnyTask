package id.pineapple.anytask.notes

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.support.v7.widget.StaggeredGridLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import id.pineapple.anytask.R
import id.pineapple.recyclerviewutil.BaseViewHolder
import id.pineapple.recyclerviewutil.UniqueEntity
import id.pineapple.recyclerviewutil.ViewHolderFactory

@SuppressLint("ClickableViewAccessibility")
class TextEditViewHolder(
		private val context: Context,
		container: ViewGroup,
		resId: Int
): BaseViewHolder<TextEditViewHolder.Marker>(context, resId, container) {
	val editText: EditText = itemView.findViewById(R.id.edit_text)
	
	init {
		(itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.isFullSpan = true
		editText.isFocusable = false
		var lastTitleClickX = 0.0f
		var lastTitleClickY = 0.0f
		editText.setOnTouchListener { _, event ->
			if (event.action == MotionEvent.ACTION_DOWN) {
				lastTitleClickX = event.x
				lastTitleClickY = event.y
			}
			false
		}
		editText.setOnClickListener {
			startEdit(lastTitleClickX, lastTitleClickY)
		}
		editText.setOnFocusChangeListener { _, hasFocus ->
			if (!hasFocus) {
				finishEdit()
			}
		}
		editText.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
			}
			
			override fun afterTextChanged(s: Editable) {
			}
			
			override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
				item?.text = s.toString()
			}
		})
	}
	
	override fun doBind(item: Marker, oldItem: Marker?) {
		if (editText.text.toString() != item.text) {
			editText.setText(item.text)
		}
	}
	
	fun startEdit(x: Float = 0.0f, y: Float = 0.0f) {
		if (editText.isFocusable) return
		editText.isFocusable = true
		editText.isFocusableInTouchMode = true
		editText.requestFocus()
		editText.post {
			(context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
					.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
		}
		editText.postDelayed({
			if (!editText.isFocused) return@postDelayed
			val eventTime = System.currentTimeMillis()
			val event1 = MotionEvent.obtain(eventTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
			val event2 = MotionEvent.obtain(eventTime, eventTime, MotionEvent.ACTION_UP, x, y, 0)
			editText.onTouchEvent(event1)
			editText.onTouchEvent(event2)
			event1.recycle()
			event2.recycle()
		}, 400)
	}
	
	fun finishEdit() {
		if (!editText.isFocusable) return
		editText.isFocusable = false
		editText.post {
			(context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
					.hideSoftInputFromWindow(editText.windowToken, 0)
		}
	}
	
	data class Marker(
			override val id: Long?,
			var text: String,
			val resId: Int
	): UniqueEntity {
		override fun describeContents(): Int = 0
		
		override fun writeToParcel(parcel: Parcel, flags: Int) {
			parcel.writeLong(id ?: 0L)
			parcel.writeString(text)
			parcel.writeInt(resId)
		}
		
		companion object CREATOR : Parcelable.Creator<Marker> {
			override fun createFromParcel(parcel: Parcel): Marker = Marker(
					parcel.readLong().let { if (it != 0L) it else 0L },
					parcel.readString()!!,
					parcel.readInt()
			)
			
			override fun newArray(size: Int): Array<Marker?> = arrayOfNulls(size)
		}
	}
	
	class Factory(private val resId: Int):
			ViewHolderFactory<Marker>(Marker::class.java) {
		override fun createViewHolder(context: Context, container: ViewGroup) =
				TextEditViewHolder(context, container, resId)
		
		override fun isItemSupported(item: Marker): Boolean = item.resId == resId
	}
}
