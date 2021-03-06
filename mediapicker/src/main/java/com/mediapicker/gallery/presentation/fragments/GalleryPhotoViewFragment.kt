package com.mediapicker.gallery.presentation.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mediapicker.gallery.R
import com.mediapicker.gallery.domain.entity.PhotoAlbum
import com.mediapicker.gallery.domain.entity.PostingDraftPhoto
import com.mediapicker.gallery.presentation.adapters.IGalleryItemClickListener
import com.mediapicker.gallery.presentation.adapters.SelectPhotoImageAdapter
import com.mediapicker.gallery.presentation.utils.Constants.EXTRA_SELECTED_ALBUM
import com.mediapicker.gallery.presentation.utils.Constants.EXTRA_SELECTED_PHOTO
import com.mediapicker.gallery.presentation.utils.ItemDecorationAlbumColumns
import com.mediapicker.gallery.presentation.utils.ValidatePhotos
import com.mediapicker.gallery.presentation.utils.ValidationResult
import com.mediapicker.gallery.utils.SnackbarUtils
import kotlinx.android.synthetic.main.fragment_folder_view.*

const val COLUMNS_COUNT = 3

class GalleryPhotoViewFragment : BaseGalleryViewFragment() {

    lateinit var adapter: SelectPhotoImageAdapter

    private var photoValidationAction: ValidatePhotos = ValidatePhotos()

    private val photoAlbum: PhotoAlbum by lazy {
        arguments?.getSerializable(EXTRA_SELECTED_ALBUM) as PhotoAlbum
    }

    private val currentSelectedPhotos: LinkedHashSet<PostingDraftPhoto> by lazy {
        arguments?.getSerializable(EXTRA_SELECTED_PHOTO) as LinkedHashSet<PostingDraftPhoto>
    }

    private fun removePhotoFromSelection(photo: PostingDraftPhoto, position: Int) {
        currentSelectedPhotos.remove(photo)
        adapter.listCurrentPhotos = currentSelectedPhotos.toList()
        adapter.notifyDataSetChanged()
    }

    private fun showError(msg: String?) {
        SnackbarUtils.show(view, msg, Snackbar.LENGTH_SHORT)
    }

    override fun getScreenTitle() = photoAlbum.name ?: ""

    override fun getLayoutId() = R.layout.fragment_folder_view

    override fun setUpViews() {
        super.setUpViews()
        photoAlbum.let { album ->
            adapter = SelectPhotoImageAdapter(album.getAlbumEntries(), currentSelectedPhotos.toList(), galleryItemClickListener, fromGallery = false)
        }

        folderRV.apply {
            this.addItemDecoration(ItemDecorationAlbumColumns(resources.getDimensionPixelSize(R.dimen.module_base), COLUMNS_COUNT))
            this.layoutManager = GridLayoutManager(activity, COLUMNS_COUNT)
            this.adapter = this@GalleryPhotoViewFragment.adapter
        }
    }

    @SuppressLint("CheckResult")
    private fun handleItemClickListener(photo: PostingDraftPhoto, position: Int) {
        if (currentSelectedPhotos.contains(photo)) {
            removePhotoFromSelection(photo, position)
        } else {
            validateNewPhoto(photo, position)
        }
    }

    private fun validateNewPhoto(photo: PostingDraftPhoto, position: Int) {
        when(val validationResult = photoValidationAction.canAddThisToList(currentSelectedPhotos.size, photo)){
            is ValidationResult.Success -> {
                galleryActionListener?.onPhotoSelected(photo)
                adapter.listCurrentPhotos = currentSelectedPhotos.toList()
                adapter.notifyDataSetChanged()
            }
            is ValidationResult.Failure -> {
                var msg = validationResult.msg
                showError(msg)
            }
        }
    }

    override fun onActionButtonClick() {
        super.onActionButtonClick()
        galleryActionListener?.onActionClicked(false)
    }

    override fun setHomeAsUp() = true

    private val galleryItemClickListener = object : IGalleryItemClickListener {

        override fun onPhotoItemClick(photo: PostingDraftPhoto, position: Int) {
            handleItemClickListener(photo, position)
        }

        override fun onFolderItemClick() {

        }

        override fun onCameraIconClick() {

        }
    }

    companion object {
        fun getInstance(photoAlbum: PhotoAlbum, currentSelectedPhotos: java.util.LinkedHashSet<PostingDraftPhoto>) = GalleryPhotoViewFragment().apply {
            arguments = Bundle().apply {
                this.putSerializable(EXTRA_SELECTED_ALBUM, photoAlbum)
                this.putSerializable(EXTRA_SELECTED_PHOTO, currentSelectedPhotos)
            }
        }
    }
}