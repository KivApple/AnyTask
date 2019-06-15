package id.pineapple.recyclerviewutil

import android.os.Parcelable
import android.support.annotation.Keep

@Keep
interface UniqueEntity : Parcelable {
	val id: Long?
}
