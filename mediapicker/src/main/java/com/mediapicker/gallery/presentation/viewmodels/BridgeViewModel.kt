package com.mediapicker.gallery.presentation.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mediapicker.gallery.GalleryConfig
import com.mediapicker.gallery.domain.action.RuleAction
import com.mediapicker.gallery.domain.entity.PostingDraftPhoto

class BridgeViewModel(
    private var listOfSelectedPhotos: List<PostingDraftPhoto>,
    private var listOfSelectedVideos: List<VideoFile>,
    private val galleryConfig: GalleryConfig
) : ViewModel() {

    private val ruleAction: RuleAction = RuleAction(galleryConfig.validation)

    private val reloadMediaLiveData = MutableLiveData<Unit>()

    private val recordVideoLiveData = MutableLiveData<Unit>()

    private val actionButtonStateLiveData = MutableLiveData<Boolean>()

    private val errorStateLiveData = MutableLiveData<String>()

    fun recordVideoWithNativeCamera() = recordVideoLiveData

    fun getActionState() = actionButtonStateLiveData

    fun getMediaStateLiveData() = reloadMediaLiveData

    fun getError() = errorStateLiveData

    fun setCurrentSelectedPhotos(listOfSelectedPhotos: List<PostingDraftPhoto>) {
        this.listOfSelectedPhotos = listOfSelectedPhotos
        shouldEnableActionButton()
    }

    fun setCurrentSelectedVideos(listOfSelectedVideos: List<VideoFile>) {
        this.listOfSelectedVideos = listOfSelectedVideos
        shouldEnableActionButton()
    }

    private fun shouldEnableActionButton() {
        val status = ruleAction.shouldEnableActionButton(Pair(listOfSelectedPhotos.size, listOfSelectedVideos.size))
        actionButtonStateLiveData.postValue(status)
    }

    private fun onActionButtonClick() {
        galleryConfig.galleryCommunicator.actionButtonClick(listOfSelectedPhotos, listOfSelectedVideos)
    }


    fun shouldRecordVideo() {
        if (galleryConfig.shouldUseVideoCamera) {
            recordVideoLiveData.postValue(Unit)
        } else {
            galleryConfig.galleryCommunicator.recordVideo()
        }
    }

    fun onBackPressed() {
        galleryConfig.galleryCommunicator.onCloseMainScreen()
    }

    fun getMaxSelectionLimit() = galleryConfig.validation.getMaxPhotoSelectionRule().maxSelectionLimit

    fun getMaxVideoSelectionLimit() = galleryConfig.validation.getMaxVideoSelectionRule().maxSelectionLimit

    fun getMaxLimitErrorResponse() = galleryConfig.validation.getMaxPhotoSelectionRule().message

    fun reloadMedia() {
        reloadMediaLiveData.postValue(Unit)
    }

    fun shouldUseMyCamera(): Boolean {
        galleryConfig.galleryCommunicator.captureImage()
        return galleryConfig.shouldUsePhotoCamera
    }

    fun getMaxVideoLimitErrorResponse() = galleryConfig.validation.getMaxVideoSelectionRule().message

    fun complyRules() {
        val error = ruleAction.getFirstFailingMessage(Pair(listOfSelectedPhotos.size, listOfSelectedVideos.size))
        if (error.isEmpty()) {
            onActionButtonClick()
        } else {
            errorStateLiveData.postValue(error)
        }
    }

}