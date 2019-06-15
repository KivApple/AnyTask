package id.pineapple.anytask.notes

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.transition.TransitionInflater
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.*
import id.pineapple.anytask.ActionBarTitleProvider
import id.pineapple.anytask.OnSoftKeyboardVisibilityChangedListener
import id.pineapple.anytask.R
import id.pineapple.anytask.UiHelper
import id.pineapple.recyclerviewutil.RecyclerViewAdapter
import id.pineapple.recyclerviewutil.SelectableItemsAdapterDelegate
import id.pineapple.recyclerviewutil.UniqueEntity
import id.pineapple.anytask.tasks.TaskListAdapter
import id.pineapple.anytask.tasks.TaskListArrayModel
import id.pineapple.anytask.tasks.TaskListInterface
import kotlinx.android.synthetic.main.fragment_note.*

class NoteFragment : Fragment(), SelectableItemsAdapterDelegate.OnSelectionChangedListener,
		ActionMode.Callback, ActionBarTitleProvider, OnSoftKeyboardVisibilityChangedListener,
		TaskListInterface {
	private lateinit var sharedPreferences: SharedPreferences
	var notePath: Array<Note>? = null
		get() = field ?: arguments!!.getParcelableArrayList<Note>(ARG_NOTE_PATH)!!.toTypedArray()
		private set
	private var note: Note? = null
	private val isFolder: Boolean
		get() = note?.type.let { it == null || it == Note.Type.FOLDER }
	private lateinit var model: NoteListModel
	private lateinit var controller: NoteListController
	private var adapter: RecyclerViewAdapter? = null
	private var savedAdapterState: Bundle? = null
	private var actionMode: ActionMode? = null
	override val actionBarTitle: String?
		get() = note.let { folder ->
			when {
				folder == null -> getString(R.string.home_folder)
				folder.type == Note.Type.FOLDER -> getString(R.string.folder)
				else -> getString(R.string.note)
			}
		}
	override val actionBarSubtitle: String?
		get() = when {
			notePath!!.isNotEmpty() ->
				getString(R.string.in_folder,
						"/${notePath!!.dropLast(1).joinToString("/") {
							it.title
						}}")
			else -> null
		}
	private lateinit var titleEditMarker: TextEditViewHolder.Marker
	private lateinit var textEditMarker: TextEditViewHolder.Marker
	private var instanceJustSaved = false
	private var isDialog = false
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		isDialog = arguments?.getBoolean(ARG_IS_DIALOG) ?: false
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
		notePath = notePath
		savedAdapterState = savedInstanceState?.getBundle(STATE_ADAPTER_DATA)
		note = savedInstanceState?.getParcelable(STATE_NOTE) ?: notePath!!.lastOrNull()
		model = NoteListPersistentModel()
		controller = NoteListController(model, context as UiHelper)
		if (!isDialog) {
			setHasOptionsMenu(true)
			if (note != null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					sharedElementEnterTransition =
							TransitionInflater.from(context).inflateTransition(android.R.transition.move)
				}
				postponeEnterTransition()
			}
		}
	}
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		adapter?.let {
			savedAdapterState = it.saveInstanceState()
			outState.putBundle(STATE_ADAPTER_DATA, savedAdapterState)
		}
		saveChanges()
		outState.putParcelable(STATE_NOTE, note)
		instanceJustSaved = true
	}
	
	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(if (isFolder) R.menu.menu_note_list else R.menu.menu_note, menu)
	}
	
	override fun onPrepareOptionsMenu(menu: Menu) {
		if (isFolder) {
			menu.findItem(R.id.action_toggle_note_list_mode).setIcon(
					if (sharedPreferences.getString("note_list_mode",
									"compact") != "compact")
						R.drawable.ic_view_stream_white_24dp
					else
						R.drawable.ic_view_compact_white_24dp
			)
		}
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
							  savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_note, container, false)
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		notes_recycler_view.isNestedScrollingEnabled = true
		notes_recycler_view.layoutManager = if (isFolder)
			StaggeredGridLayoutManager(
					if (sharedPreferences.getString("note_list_mode", "compact") == "compact")
						when (context!!.resources.configuration.orientation) {
							Configuration.ORIENTATION_LANDSCAPE -> 3
							else -> 2
						}
					else
						1,
					StaggeredGridLayoutManager.VERTICAL
			)
		else
			LinearLayoutManager(context)
		adapter = when {
			isFolder -> createNoteListAdapter()
			note?.type == Note.Type.LIST -> createTaskListAdapter()
			else -> createSimpleAdapter()
		}
		adapter!!.restoreInstanceState(savedAdapterState)
		notes_recycler_view.adapter = adapter
		(adapter as? SelectableItemsAdapterDelegate)?.addOnSelectionChangedListener(this)
		if (isFolder) {
			ViewCompat.setTransitionName(new_note_zone, "note:null")
			new_note_zone.setOnClickListener {
				controller.newNote(Note.Type.TEXT, notePath!!, new_note_zone)
			}
			new_list_note_image_button.setOnClickListener {
				controller.newNote(Note.Type.LIST, notePath!!, new_note_zone)
			}
		} else {
			new_note_zone.visibility = View.GONE
		}
		notes_recycler_view.setOnTouchListener(object : View.OnTouchListener {
			private var touchX = 0.0f
			private var touchY = 0.0f
			
			@SuppressLint("ClickableViewAccessibility")
			override fun onTouch(view: View, event: MotionEvent): Boolean {
				when (event.action) {
					MotionEvent.ACTION_DOWN -> {
						touchX = event.x
						touchY = event.y
						val child = notes_recycler_view.findChildViewUnder(event.x, event.y)
						if (child == null) {
							if (note != null) {
								adapter!!.getViewHolder<TextEditViewHolder>(Long.MIN_VALUE)?.finishEdit()
								(adapter as? TaskListAdapter)?.finishTitleEdit()
							}
						}
					}
					MotionEvent.ACTION_UP -> {
						val deltaX = touchX - event.x
						val deltaY = touchY - event.y
						val delta = deltaX * deltaX + deltaY * deltaY
						if (delta < 10.0f * 10.0f) {
							val child = notes_recycler_view.findChildViewUnder(event.x, event.y)
							if (child == null) {
								if (note != null) {
									adapter!!.getViewHolder<TextEditViewHolder>(0)?.editText?.let { editText ->
										val sourceLocation = IntArray(2)
										view.getLocationOnScreen(sourceLocation)
										val targetLocation = IntArray(2)
										editText.getLocationOnScreen(targetLocation)
										val x = touchX - (targetLocation[0] - sourceLocation[0])
										val y = touchY - (targetLocation[1] - sourceLocation[1])
										val eventTime = System.currentTimeMillis()
										val event1 = MotionEvent.obtain(eventTime, eventTime,
												MotionEvent.ACTION_DOWN, x, y, 0)
										val event2 = MotionEvent.obtain(eventTime, eventTime,
												MotionEvent.ACTION_UP, x, y, 0)
										editText.dispatchTouchEvent(event1)
										editText.dispatchTouchEvent(event2)
										event1.recycle()
										event2.recycle()
									}
								}
							}
						}
					}
				}
				return false
			}
		})
		if (note != null && !isDialog) {
			note!!.let { note ->
				ViewCompat.setTransitionName(view, "note:${note.id}")
				startPostponedEnterTransition()
				if (note.id == null && note.type == Note.Type.TEXT) {
					notes_recycler_view.post {
						adapter!!.getViewHolder<TextEditViewHolder>(0)?.startEdit()
					}
				}
			}
		}
	}
	
	private fun createNoteListAdapter() =
			object : NoteListAdapter(
					model,
					notePath!!,
					context!!,
					this
			) {
				init {
					registerViewType(TextEditViewHolder.Factory(R.layout.item_note_title_edit_view))
				}
				
				override fun addHeaders(items: MutableList<UniqueEntity>) {
					if (note != null) {
						items.add(titleEditMarker)
					}
				}
				
				override fun restoreInstanceState(savedState: Bundle?, callNotifyDataSetChanged: Boolean) {
					if (savedState == null && note != null) {
						titleEditMarker =
								TextEditViewHolder.Marker(Long.MIN_VALUE, note?.title ?: "",
										R.layout.item_note_title_edit_view)
					}
					super.restoreInstanceState(savedState, callNotifyDataSetChanged)
					if (savedState != null && note != null) {
						titleEditMarker = items.first {
							it is TextEditViewHolder.Marker
						} as TextEditViewHolder.Marker
					}
				}
			}
	
	private fun createTaskListAdapter() =
			object : TaskListAdapter(
					TaskListArrayModel(note!!.getList(), view),
					false,
					sharedPreferences.getBoolean("hide_completed_tasks", false),
					context!!,
					this@NoteFragment
			) {
				init {
					registerViewType(TextEditViewHolder.Factory(R.layout.item_note_title_edit_view))
				}
				
				override fun addHeaders(items: MutableList<UniqueEntity>) {
					items.add(titleEditMarker)
				}
				
				override fun restoreInstanceState(savedState: Bundle?, callNotifyDataSetChanged: Boolean) {
					if (savedState == null) {
						titleEditMarker =
								TextEditViewHolder.Marker(Long.MIN_VALUE, note?.title ?: "",
										R.layout.item_note_title_edit_view)
					}
					super.restoreInstanceState(savedState, callNotifyDataSetChanged)
					if (savedState != null) {
						titleEditMarker = items.first {
							it is TextEditViewHolder.Marker
						} as TextEditViewHolder.Marker
					}
				}
			}
	
	private fun createSimpleAdapter() =
			object : RecyclerViewAdapter(context!!) {
				init {
					registerViewType(TextEditViewHolder.Factory(
							R.layout.item_note_title_borderless_edit_view
					))
					registerViewType(TextEditViewHolder.Factory(R.layout.item_note_text_edit_view))
				}
				
				override fun restoreInstanceState(savedState: Bundle?, callNotifyDataSetChanged: Boolean) {
					if (savedState == null) {
						titleEditMarker =
								TextEditViewHolder.Marker(Long.MIN_VALUE, note?.title ?: "",
										R.layout.item_note_title_borderless_edit_view)
						textEditMarker =
								TextEditViewHolder.Marker(0, note?.text ?: "",
										R.layout.item_note_text_edit_view)
					}
					super.restoreInstanceState(savedState, callNotifyDataSetChanged)
					if (savedState != null && note != null) {
						titleEditMarker = items[0] as TextEditViewHolder.Marker
						textEditMarker = items[1] as TextEditViewHolder.Marker
					} else {
						items.add(titleEditMarker)
						items.add(textEditMarker)
					}
				}
			}
	
	override fun onDestroyView() {
		adapter = null
		super.onDestroyView()
	}
	
	override fun onStart() {
		instanceJustSaved = false
		super.onStart()
	}
	
	override fun onStop() {
		if (!instanceJustSaved) {
			saveChanges()
		}
		finishActionMode()
		super.onStop()
	}
	
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_note_delete -> note?.let { note ->
				controller.delete(note)
				activity?.onBackPressed()
			}
			R.id.action_new_note_folder -> {
				controller.newNoteFolder(note?.id)
			}
			R.id.action_toggle_note_list_mode -> {
				(notes_recycler_view.layoutManager as StaggeredGridLayoutManager).spanCount =
						if (sharedPreferences.getString("note_list_mode", "compact") != "compact") {
							sharedPreferences.edit().putString("note_list_mode", "compact").apply()
							when (context!!.resources.configuration.orientation) {
								Configuration.ORIENTATION_LANDSCAPE -> 3
								else -> 2
							}
						} else {
							sharedPreferences.edit().putString("note_list_mode", "stream").apply()
							1
						}
				activity?.invalidateOptionsMenu()
			}
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}
	
	override fun onSoftKeyboardVisibilityChangedListener(isVisible: Boolean) {
		if (isFolder) {
			new_note_zone.visibility = if (isVisible) View.GONE else View.VISIBLE
		}
	}
	
	override fun onSelectionChanged(selectedItems: Collection<UniqueEntity>) {
		if (isDialog && selectedItems.isNotEmpty()) {
			(adapter as? SelectableItemsAdapterDelegate)?.clearSelection()
			return
		}
		if (selectedItems.isNotEmpty()) {
			if (actionMode != null) {
				actionMode?.invalidate()
			} else {
				actionMode = (context!! as AppCompatActivity).startSupportActionMode(this)
			}
		} else {
			actionMode?.finish()
		}
	}
	
	override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
		if (isFolder) {
			mode.menuInflater.inflate(R.menu.menu_note_actions, menu)
		} else {
			mode.menuInflater.inflate(R.menu.menu_task_actions, menu)
			menu.findItem(R.id.action_task_reschedule_to).isVisible = false
			menu.findItem(R.id.action_task_postpone_to_tomorrow).isVisible = false
		}
		return true
	}
	
	override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
		if (isFolder) {
			menu.findItem(R.id.action_note_copy).isVisible =
					(adapter as? NoteListAdapter)?.canCopySelection == true
		}
		return false
	}
	
	override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_note_move -> (adapter as? NoteListAdapter)?.moveSelection()
			R.id.action_note_copy -> (adapter as? NoteListAdapter)?.copySelection()
			R.id.action_note_delete -> (adapter as? NoteListAdapter)?.deleteSelection()
			R.id.action_task_copy -> (adapter as? TaskListAdapter)?.copySelection()
			R.id.action_task_delete -> (adapter as? TaskListAdapter)?.deleteSelection()
			else -> return false
		}
		return true
	}
	
	override fun onDestroyActionMode(mode: ActionMode) {
		actionMode = null
		(adapter as? SelectableItemsAdapterDelegate)?.clearSelection()
	}
	
	private fun finishActionMode() {
		actionMode?.finish()
	}
	
	private fun saveChanges() {
		note?.let { note ->
			var newNote = note
			if (newNote.title != titleEditMarker.text) {
				newNote = note.updateTitle(titleEditMarker.text)
			}
			when (note.type) {
				Note.Type.FOLDER -> {}
				Note.Type.TEXT -> if (textEditMarker.text != newNote.text) {
					newNote = newNote.updateText(textEditMarker.text)
				}
				Note.Type.LIST -> (adapter as TaskListAdapter).let { adapter ->
					adapter.saveTitleEdit()
					val tmp = newNote.updateList((adapter.model as TaskListArrayModel).items)
					if (tmp.text != newNote.text) {
						newNote = tmp
					}
				}
			}
			if (newNote !== note) {
				this.note = newNote
				controller.saveNote(newNote)
			}
		}
	}
	
	override fun editTaskTitle(id: Long) {
		(adapter as? TaskListAdapter)?.editTitle(id)
	}
	
	override fun finishTaskEdit() {
		(adapter as? TaskListAdapter)?.finishTitleEdit()
	}
	
	companion object {
		private const val ARG_NOTE_PATH = "path"
		private const val ARG_IS_DIALOG = "is_dialog"
		
		private const val STATE_ADAPTER_DATA = "adapter_data"
		private const val STATE_NOTE = "note"
		
		@JvmStatic
		fun newInstance(vararg notePath: Note, isDialog: Boolean = false) =
				NoteFragment().apply {
					arguments = Bundle().apply {
						putParcelableArrayList(ARG_NOTE_PATH, notePath.toMutableList() as ArrayList)
						putBoolean(ARG_IS_DIALOG, isDialog)
					}
				}
	}
}
