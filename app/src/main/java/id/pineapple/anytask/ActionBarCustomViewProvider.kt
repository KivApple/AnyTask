package id.pineapple.anytask

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

interface ActionBarCustomViewProvider {
	fun onCreateActionBarCustomView(inflater: LayoutInflater, parent: ViewGroup): View
}
