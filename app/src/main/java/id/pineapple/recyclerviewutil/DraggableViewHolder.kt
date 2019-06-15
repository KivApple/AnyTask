package id.pineapple.recyclerviewutil

interface DraggableViewHolder {
	fun canMove(): Boolean = false
	
	fun canSwipeLeft(): Boolean = false
	
	fun canSwipeRight(): Boolean = false
	
	fun swipedLeft() {
	}
	
	fun swipedRight() {
	}
	
	fun canDrop(targetViewHolder: DraggableViewHolder) = false
	
	fun dragFinished(moved: Boolean) {
	}
}
