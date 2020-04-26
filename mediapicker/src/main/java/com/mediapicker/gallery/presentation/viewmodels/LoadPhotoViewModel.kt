package com.mediapicker.gallery.presentation.viewmodels

import android.database.Cursor
import android.database.DataSetObserver
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.lifecycle.MutableLiveData
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.mediapicker.gallery.GalleryConfig
import com.mediapicker.gallery.domain.entity.CameraItem
import com.mediapicker.gallery.domain.entity.IGalleryItem
import com.mediapicker.gallery.domain.entity.PhotoAlbum
import com.mediapicker.gallery.domain.entity.PostingDraftPhoto
import com.mediapicker.gallery.presentation.viewmodels.factory.BaseLoadMediaViewModel
import java.util.*

class LoadPhotoViewModel constructor(val galleryConfig: GalleryConfig) : BaseLoadMediaViewModel(galleryConfig){

    companion object {
        private const val COL_FULL_PHOTO_URL = "fullPhotoUrl"
    }

    private var isObserverRegistered = false

    private var lastLoadedCursor: Cursor? = null

    private var currentSelectedPhotos: LinkedHashSet<PostingDraftPhoto> = LinkedHashSet()

    private val galleryItemsLiveData = MutableLiveData<List<IGalleryItem>>()

    fun getGalleryItems() = galleryItemsLiveData

    override fun getCursorLoader(): Loader<Cursor> {
        val selection = MediaStore.Images.Media.MIME_TYPE + "!=?"
        val mimeTypeGif = MimeTypeMap.getSingleton().getMimeTypeFromExtension("gif")
        val selectionTypeGifArgs = arrayOf(mimeTypeGif)
        return CursorLoader(
            getApplication(),
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, selection,
            selectionTypeGifArgs, MediaStore.Images.Media.DATE_TAKEN + " DESC"
        )
    }

    override fun getUniqueLoaderId() = 1

    override fun prepareDataForAdapterAndPost(cursor: Cursor) {
        val listOfGalleryItems: MutableList<IGalleryItem> = ArrayList()
        if (cursor.moveToFirst()) {
            val photos = ArrayList<IGalleryItem>()
            do {
                val photo = getPhoto(cursor)
                photos.add(photo)
            } while (cursor.moveToNext())
            listOfGalleryItems.clear()
            listOfGalleryItems.add(CameraItem())
            if (needToAddFolderView())
                listOfGalleryItems.add(PhotoAlbum.dummyInstance)
            listOfGalleryItems.addAll(getFinalListOfGalleryItems(photos))
        }
        galleryItemsLiveData.postValue(listOfGalleryItems)
    }

    private fun unregisterDataSetObserver() {
        if (lastLoadedCursor != null && isObserverRegistered) {
            lastLoadedCursor?.unregisterDataSetObserver(dataObserver)
            isObserverRegistered = false
        }
    }

    private fun registerDataSetObserver() {
        if (!isObserverRegistered) {
            if (lastLoadedCursor != null) {
                lastLoadedCursor?.registerDataSetObserver(dataObserver)
                isObserverRegistered = true
            }
        }
    }

    private val dataObserver = object : DataSetObserver() {
        override fun onChanged() {
            super.onChanged()
            getNewlyAddedMedia()
        }
    }

    private fun getNewlyAddedMedia() {

    }

    private fun needToAddFolderView(): Boolean {
        return (galleryConfig.typeOfMediaSupported == GalleryConfig.MediaType.PhotoWithFolderOnly
                || galleryConfig.typeOfMediaSupported == GalleryConfig.MediaType.PhotoWithFolderAndVideo)
    }


    private fun getFinalListOfGalleryItems(photos: List<IGalleryItem>): Collection<IGalleryItem> {
        val finalSetOfPhotos = ArrayList<IGalleryItem>()
        if (currentSelectedPhotos.isNotEmpty()) {
            for (currentSelectedPhoto in currentSelectedPhotos) {
                if (!photos.contains(currentSelectedPhoto)) {
                    val photoBackendId = currentSelectedPhoto.photoBackendId
                    if (photoBackendId != null) {
                        currentSelectedPhoto.path = "" + photoBackendId!!
                    }
                    finalSetOfPhotos.add(currentSelectedPhoto)
                }
            }
        }
        finalSetOfPhotos.addAll(photos)
        return finalSetOfPhotos
    }


    private fun getPhoto(cursor: Cursor): PostingDraftPhoto {
        val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID))
        val path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
        val col = cursor.getColumnIndex(COL_FULL_PHOTO_URL)
        var fullPhotoUrl = ""
        if (col != -1) {
            fullPhotoUrl = cursor.getString(col)
        }
        return PostingDraftPhoto.Builder()
            .imageId(id)
            .path(path)
            .smallPhotoUrl("")
            .fullPhotoUrl(fullPhotoUrl)
            .photoBackendId(0L)
            .build()
    }

    override fun onCleared() {
        super.onCleared()
        // unregisterDataSetObserver()
    }
}
