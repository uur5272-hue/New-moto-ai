package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MotoApplication
import com.example.control.AppInfo
import com.example.control.DeviceAction
import com.example.control.DeviceControlManager
import com.example.data.db.MotoDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.GeneratedMediaEntity
import com.example.data.repository.UserPreferencesRepository
import com.example.media.MediaStorageManager
import com.example.network.ApiResult
import com.example.network.GeminiApiClient
import com.example.network.GeneratedImageResult
import com.example.network.GeneratedVideoResult
import com.example.service.MotoFloatingService
import com.example.service.MotoWakeWordService
import com.example.voice.MotoVoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MotoViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MotoApplication
    private val db: MotoDatabase = app.database
    private val prefsRepo: UserPreferencesRepository = app.preferencesRepository
    private val geminiClient: GeminiApiClient = app.geminiApiClient
    private val mediaStorage: MediaStorageManager = app.mediaStorageManager
    private val deviceControl: DeviceControlManager = app.deviceControlManager
    val voiceManager: MotoVoiceManager = app.voiceManager

    // Preferences
    val preferences: StateFlow<UserPreferencesRepository.PreferencesState> = prefsRepo.state

    // Chat
    val chatMessages: StateFlow<List<ChatMessageEntity>> = db.chatMessageDao().getAllMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _attachedImage = MutableStateFlow<Bitmap?>(null)
    val attachedImage: StateFlow<Bitmap?> = _attachedImage.asStateFlow()

    // Voice UI State
    val isVoiceListening: StateFlow<Boolean> = voiceManager.isVoiceListening
    val isSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking
    val rmsLevel: StateFlow<Float> = voiceManager.rmsLevel

    // Pending Device Dialogs
    private val _pendingCallTarget = MutableStateFlow<String?>(null)
    val pendingCallTarget: StateFlow<String?> = _pendingCallTarget.asStateFlow()

    private val _pendingMessage = MutableStateFlow<Pair<String, String>?>(null)
    val pendingMessage: StateFlow<Pair<String, String>?> = _pendingMessage.asStateFlow()

    // Download/Save Status Toast or Banner
    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice: StateFlow<String?> = _statusNotice.asStateFlow()

    // Image Studio State
    private val _imagePrompt = MutableStateFlow("")
    val imagePrompt: StateFlow<String> = _imagePrompt.asStateFlow()

    private val _selectedAspectRatio = MutableStateFlow("1:1")
    val selectedAspectRatio: StateFlow<String> = _selectedAspectRatio.asStateFlow()

    private val _selectedStyle = MutableStateFlow("Cyberpunk Neon")
    val selectedStyle: StateFlow<String> = _selectedStyle.asStateFlow()

    private val _generatedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedImageBitmap: StateFlow<Bitmap?> = _generatedImageBitmap.asStateFlow()

    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    private val _imageGenError = MutableStateFlow<String?>(null)
    val imageGenError: StateFlow<String?> = _imageGenError.asStateFlow()

    // Photo Editor State
    private val _editSourceBitmap = MutableStateFlow<Bitmap?>(null)
    val editSourceBitmap: StateFlow<Bitmap?> = _editSourceBitmap.asStateFlow()

    private val _editInstructions = MutableStateFlow("")
    val editInstructions: StateFlow<String> = _editInstructions.asStateFlow()

    private val _editedResultBitmap = MutableStateFlow<Bitmap?>(null)
    val editedResultBitmap: StateFlow<Bitmap?> = _editedResultBitmap.asStateFlow()

    private val _isEditingPhoto = MutableStateFlow(false)
    val isEditingPhoto: StateFlow<Boolean> = _isEditingPhoto.asStateFlow()

    private val _photoEditError = MutableStateFlow<String?>(null)
    val photoEditError: StateFlow<String?> = _photoEditError.asStateFlow()

    // Video Studio State
    private val _videoPrompt = MutableStateFlow("")
    val videoPrompt: StateFlow<String> = _videoPrompt.asStateFlow()

    private val _videoResolution = MutableStateFlow("720p")
    val videoResolution: StateFlow<String> = _videoResolution.asStateFlow()

    private val _videoStatusMessage = MutableStateFlow<String?>(null)
    val videoStatusMessage: StateFlow<String?> = _videoStatusMessage.asStateFlow()

    private val _isGeneratingVideo = MutableStateFlow(false)
    val isGeneratingVideo: StateFlow<Boolean> = _isGeneratingVideo.asStateFlow()

    private val _generatedVideoResult = MutableStateFlow<GeneratedVideoResult?>(null)
    val generatedVideoResult: StateFlow<GeneratedVideoResult?> = _generatedVideoResult.asStateFlow()

    private val _videoError = MutableStateFlow<String?>(null)
    val videoError: StateFlow<String?> = _videoError.asStateFlow()

    // Device Apps
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    init {
        _videoResolution.value = preferences.value.videoResolution
        loadInstalledApps()
        updateTtsSettings()
    }

    fun setAttachedImage(bitmap: Bitmap?) {
        _attachedImage.value = bitmap
    }

    fun setImagePrompt(prompt: String) {
        _imagePrompt.value = prompt
    }

    fun setSelectedAspectRatio(ratio: String) {
        _selectedAspectRatio.value = ratio
    }

    fun setSelectedStyle(style: String) {
        _selectedStyle.value = style
    }

    fun setEditSourceBitmap(bitmap: Bitmap?) {
        _editSourceBitmap.value = bitmap
    }

    fun setEditInstructions(instructions: String) {
        _editInstructions.value = instructions
    }

    fun setVideoPrompt(prompt: String) {
        _videoPrompt.value = prompt
    }

    fun setVideoResolutionChoice(res: String) {
        _videoResolution.value = res
        prefsRepo.setVideoResolution(res)
    }

    fun clearStatusNotice() {
        _statusNotice.value = null
    }

    fun showNotice(text: String) {
        _statusNotice.value = text
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = deviceControl.getLaunchableApps()
        }
    }

    fun updateTtsSettings() {
        val p = preferences.value
        voiceManager.setSpeechParameters(p.speechSpeed, p.speechPitch, p.language)
    }

    fun sendChatMessage(userText: String, isVoiceInput: Boolean = false) {
        val text = userText.trim()
        if (text.isEmpty() && _attachedImage.value == null) return

        _chatError.value = null
        val attach = _attachedImage.value
        _attachedImage.value = null

        viewModelScope.launch {
            var localCachedImagePath: String? = null
            if (attach != null) {
                val cachedFile = mediaStorage.cacheBitmap(attach, "chat_in")
                localCachedImagePath = cachedFile.absolutePath
            }

            db.chatMessageDao().insertMessage(
                ChatMessageEntity(
                    role = "user",
                    content = text,
                    attachedImagePath = localCachedImagePath,
                    isVoice = isVoiceInput,
                    languageCode = preferences.value.language
                )
            )

            val ownerAnswer = geminiClient.checkOwnerQuestion(text)
            if (ownerAnswer != null) {
                insertAssistantMessage(ownerAnswer)
                return@launch
            }

            val deviceAction = deviceControl.parseDeviceCommand(text)
            if (deviceAction != null) {
                handleDeviceAction(deviceAction)
                return@launch
            }

            _isGenerating.value = true
            val conversation = chatMessages.value.takeLast(8).map {
                (if (it.role == "user") "user" else "model") to it.content
            } + listOf("user" to text)

            val result = geminiClient.chat(conversation)
            _isGenerating.value = false

            when (result) {
                is ApiResult.Success -> {
                    insertAssistantMessage(result.data)
                }
                is ApiResult.Error -> {
                    _chatError.value = result.message
                    insertAssistantMessage("⚠️ ${result.message}", isError = true)
                }
            }
        }
    }

    private suspend fun insertAssistantMessage(content: String, isError: Boolean = false) {
        db.chatMessageDao().insertMessage(
            ChatMessageEntity(
                role = "assistant",
                content = content,
                isVoice = false,
                languageCode = preferences.value.language
            )
        )
        if (!isError && preferences.value.autoSpeak) {
            voiceManager.speak(content)
        }
    }

    private suspend fun handleDeviceAction(action: DeviceAction) {
        when (action) {
            is DeviceAction.OpenApp -> {
                val opened = deviceControl.launchAppByPackage(action.packageName)
                if (opened) {
                    insertAssistantMessage("Launching ${action.appName} for you now.")
                } else {
                    insertAssistantMessage("Unable to launch ${action.appName}. Please check if the app is installed.")
                }
            }
            is DeviceAction.OpenSetting -> {
                deviceControl.openSettingScreen(action.intentAction)
                insertAssistantMessage("Opening ${action.name}...")
            }
            is DeviceAction.OpenUrl -> {
                deviceControl.openWebUrl(action.url)
                insertAssistantMessage("Opening ${action.url} in your browser...")
            }
            is DeviceAction.RequestCall -> {
                _pendingCallTarget.value = action.target
                insertAssistantMessage("Preparing phone call to ${action.target}. Please confirm below to place the call.")
            }
            is DeviceAction.RequestMessage -> {
                _pendingMessage.value = action.target to action.body
                insertAssistantMessage("Drafting message to ${action.target}: \"${action.body}\". Please confirm to open your messaging app.")
            }
        }
    }

    fun confirmCall(phoneNumber: String) {
        _pendingCallTarget.value = null
        val ok = deviceControl.initiateCallDirect(phoneNumber)
        if (ok) {
            showNotice("Calling $phoneNumber...")
        } else {
            showNotice("Could not initiate call. Opening dialer.")
            deviceControl.openDialer(phoneNumber)
        }
    }

    fun dismissCallDialog() {
        _pendingCallTarget.value = null
    }

    fun confirmMessage(target: String, body: String) {
        _pendingMessage.value = null
        deviceControl.draftSms(target, body)
        showNotice("Message drafted to $target.")
    }

    fun dismissMessageDialog() {
        _pendingMessage.value = null
    }

    fun startVoiceRecognition() {
        voiceManager.stopSpeaking()
        val lang = preferences.value.language
        voiceManager.startListening(
            languageCode = lang,
            onResult = { recognizedText: String ->
                sendChatMessage(recognizedText, isVoiceInput = true)
            },
            onError = { errMsg: String ->
                _chatError.value = errMsg
            }
        )
    }

    fun stopVoiceRecognition() {
        voiceManager.stopListening()
    }

    fun speakText(text: String) {
        updateTtsSettings()
        voiceManager.speak(text)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun clearChat() {
        viewModelScope.launch {
            db.chatMessageDao().clearAllMessages()
            showNotice("Chat history cleared.")
        }
    }

    fun generateImage() {
        val prompt = _imagePrompt.value.trim()
        if (prompt.isEmpty()) return

        _isGeneratingImage.value = true
        _imageGenError.value = null
        _generatedImageBitmap.value = null

        viewModelScope.launch {
            val result = geminiClient.generateImage(
                prompt = prompt,
                aspectRatio = _selectedAspectRatio.value,
                style = _selectedStyle.value
            )
            _isGeneratingImage.value = false
            when (result) {
                is ApiResult.Success -> {
                    val imgData = result.data
                    _generatedImageBitmap.value = imgData.bitmap
                    showNotice("Image generated successfully!")
                }
                is ApiResult.Error -> {
                    _imageGenError.value = result.message
                }
            }
        }
    }

    fun saveGeneratedImageToGallery() {
        val bitmap = _generatedImageBitmap.value ?: return
        viewModelScope.launch {
            val res = mediaStorage.saveImageToGallery(bitmap, "MOTO_AI_GEN")
            if (res.success) {
                showNotice("Saved to Gallery: ${res.filePath}")
                db.generatedMediaDao().insertMedia(
                    GeneratedMediaEntity(
                        mediaType = "image",
                        prompt = _imagePrompt.value,
                        localUri = res.contentUri.toString(),
                        mediaStoreUri = res.contentUri.toString(),
                        resolution = _selectedAspectRatio.value,
                        fileName = res.fileName
                    )
                )
            } else {
                showNotice("Save failed: ${res.errorMessage}")
            }
        }
    }

    fun shareGeneratedImage() {
        val bitmap = _generatedImageBitmap.value ?: return
        viewModelScope.launch {
            val res = mediaStorage.saveImageToGallery(bitmap, "MOTO_AI_SHARE")
            if (res.success && res.contentUri != null) {
                mediaStorage.shareMedia(res.contentUri, "image/jpeg", "Image generated with MOTO AI")
            } else {
                showNotice("Failed to prepare image for sharing.")
            }
        }
    }

    fun executePhotoEdit() {
        val original = _editSourceBitmap.value ?: return
        val instructions = _editInstructions.value.trim()
        if (instructions.isEmpty()) return

        _isEditingPhoto.value = true
        _photoEditError.value = null
        _editedResultBitmap.value = null

        viewModelScope.launch {
            val result = geminiClient.editImage(original, instructions)
            _isEditingPhoto.value = false
            when (result) {
                is ApiResult.Success -> {
                    val editData = result.data
                    _editedResultBitmap.value = editData.bitmap
                    showNotice("Photo edited successfully!")
                }
                is ApiResult.Error -> {
                    _photoEditError.value = result.message
                }
            }
        }
    }

    fun saveEditedPhotoToGallery() {
        val bitmap = _editedResultBitmap.value ?: return
        viewModelScope.launch {
            val res = mediaStorage.saveImageToGallery(bitmap, "MOTO_AI_EDITED")
            if (res.success) {
                showNotice("Saved edited photo to Gallery: ${res.filePath}")
                db.generatedMediaDao().insertMedia(
                    GeneratedMediaEntity(
                        mediaType = "edited_photo",
                        prompt = _editInstructions.value,
                        localUri = res.contentUri.toString(),
                        mediaStoreUri = res.contentUri.toString(),
                        resolution = "original",
                        fileName = res.fileName
                    )
                )
            } else {
                showNotice("Save failed: ${res.errorMessage}")
            }
        }
    }

    fun shareEditedPhoto() {
        val bitmap = _editedResultBitmap.value ?: return
        viewModelScope.launch {
            val res = mediaStorage.saveImageToGallery(bitmap, "MOTO_AI_EDITED_SHARE")
            if (res.success && res.contentUri != null) {
                mediaStorage.shareMedia(res.contentUri, "image/jpeg", "Photo edited with MOTO AI")
            } else {
                showNotice("Failed to share photo.")
            }
        }
    }

    fun generateVideo() {
        val prompt = _videoPrompt.value.trim()
        if (prompt.isEmpty()) return

        _isGeneratingVideo.value = true
        _videoError.value = null
        _generatedVideoResult.value = null
        _videoStatusMessage.value = "Initializing Veo video model..."

        viewModelScope.launch {
            val result = geminiClient.generateVideo(
                prompt = prompt,
                resolution = _videoResolution.value,
                aspectRatio = "16:9",
                onProgress = { status: String ->
                    _videoStatusMessage.value = status
                }
            )
            _isGeneratingVideo.value = false
            when (result) {
                is ApiResult.Success -> {
                    val vid = result.data
                    _generatedVideoResult.value = vid
                    _videoStatusMessage.value = "Video ready!"
                    showNotice("Video generated successfully (${vid.resolution})!")
                }
                is ApiResult.Error -> {
                    _videoError.value = result.message
                    _videoStatusMessage.value = null
                }
            }
        }
    }

    fun saveGeneratedVideoToGallery() {
        val vid = _generatedVideoResult.value ?: return
        viewModelScope.launch {
            val res = mediaStorage.saveVideoToGallery(
                videoUriOrUrl = vid.videoUri,
                base64Bytes = vid.videoBase64,
                baseName = "MOTO_AI_VEO"
            )
            if (res.success) {
                showNotice("Video saved to Gallery: ${res.filePath}")
                db.generatedMediaDao().insertMedia(
                    GeneratedMediaEntity(
                        mediaType = "video",
                        prompt = vid.prompt,
                        localUri = res.contentUri.toString(),
                        mediaStoreUri = res.contentUri.toString(),
                        resolution = vid.resolution,
                        fileName = res.fileName
                    )
                )
            } else {
                showNotice("Video save failed: ${res.errorMessage}")
            }
        }
    }

    fun setLanguage(lang: String) {
        prefsRepo.setLanguage(lang)
        updateTtsSettings()
        showNotice("Language set to ${getLanguageDisplayName(lang)}")
    }

    fun setSpeechSpeed(speed: Float) {
        prefsRepo.setSpeechSpeed(speed)
        updateTtsSettings()
    }

    fun setSpeechPitch(pitch: Float) {
        prefsRepo.setSpeechPitch(pitch)
        updateTtsSettings()
    }

    fun setAutoSpeak(enabled: Boolean) {
        prefsRepo.setAutoSpeak(enabled)
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        prefsRepo.setWakeWordEnabled(enabled)
        if (enabled) {
            MotoWakeWordService.startService(app)
            showNotice("MOTO AI wake-word service started in foreground.")
        } else {
            MotoWakeWordService.stopService(app)
            showNotice("Wake-word service stopped.")
        }
    }

    fun setFloatingAssistantEnabled(enabled: Boolean) {
        prefsRepo.setFloatingEnabled(enabled)
        if (enabled) {
            if (android.provider.Settings.canDrawOverlays(app)) {
                MotoFloatingService.start(app)
                showNotice("Floating MOTO AI button activated!")
            } else {
                showNotice("Overlay permission required to display floating button.")
            }
        } else {
            MotoFloatingService.stop(app)
            showNotice("Floating button turned off.")
        }
    }

    fun setCustomApiKey(key: String) {
        prefsRepo.setCustomApiKey(key)
        showNotice("API Key updated.")
    }

    fun isApiKeyReady(): Boolean {
        return geminiClient.isKeyConfigured()
    }

    fun getApiKeyDisplayStatus(): String {
        val effective = geminiClient.getEffectiveApiKey()
        return if (effective.isNotEmpty()) {
            if (effective.length > 8) {
                "Active (•••" + effective.takeLast(4) + ")"
            } else "Active"
        } else {
            "Not Configured"
        }
    }

    private fun getLanguageDisplayName(code: String): String {
        return when (code) {
            "hi" -> "हिन्दी (Hindi)"
            "ur" -> "اردو (Urdu)"
            "es" -> "Español"
            "fr" -> "Français"
            "ar" -> "العربية (Arabic)"
            "en" -> "English"
            else -> "Auto-detect"
        }
    }
}
