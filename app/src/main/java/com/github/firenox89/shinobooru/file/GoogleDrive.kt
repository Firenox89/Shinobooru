package com.github.firenox89.shinobooru.file

import android.app.Activity
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import com.github.firenox89.shinobooru.model.FileManager
import com.github.firenox89.shinobooru.model.Post
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.drive.*
import com.google.android.gms.drive.query.Filters
import com.google.android.gms.drive.query.Query
import com.google.android.gms.drive.query.SearchableField
import org.jetbrains.anko.doAsync
import java.io.File
import java.nio.channels.Channels


/**
 * Created by firenox on 02.11.16.
 */
class GoogleDrive(val activity: Activity, val uicallback: (Map<Metadata, List<Post>>?) -> Unit) :
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    val TAG = "GoogleDrive"
    val appRootDir = "Shinobooru"
    val REQUEST_CODE_RESOLUTION = 1

    var appRootDriveId: DriveId? = null
    var connected = false
    var driveContent: Map<Metadata, List<Post>>? = null

    //TODO check if that instance expires
    val googleApiClient: GoogleApiClient = GoogleApiClient.Builder(activity)
            .addApi(Drive.API)
            .addScope(Drive.SCOPE_FILE)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()

    init {
        googleApiClient.connect()
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        Log.e("GD", "GoogleApiClient connection failed: " + result.toString())
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(activity, result.errorCode, 0).show()
            return
        }
        try {
            result.startResolutionForResult(activity, REQUEST_CODE_RESOLUTION)
        } catch (e: IntentSender.SendIntentException) {
            Log.e("GD", "Exception while starting resolution activity", e)
        }
    }

    override fun onConnected(p0: Bundle?) {
        findOrCreateAppRootFolder()
        connected = true
    }

    fun sync() {
        val driveContent = this@GoogleDrive.driveContent ?: return
        doAsync(Throwable::printStackTrace) {

            driveContent.forEach { Log.e(TAG, it.key.title) }

            FileManager.boards.forEach {
                val name = it.key
                val postList = driveContent.filter { it.key.title.equals(name) }.toList()
                if (postList.isNotEmpty()) {
                    val fileList = it.value
                    val driveList = postList[0].second.map(Post::id)
                    val unsyncedFiles = fileList.filter { !driveList.contains(it.id) }
                    unsyncedFiles.forEach { uploadFile(it.file!!, postList[0].first.driveId.asDriveFolder()) }
                } else {
                    val changeSet = MetadataChangeSet.Builder().setTitle(it.key).build()
                    val result = appRootDriveId?.asDriveFolder()?.createFolder(googleApiClient, changeSet)?.await()
                    it.value.forEach {
                        val file = it.file
                        if (file != null && result != null)
                            uploadFile(file, result.driveFolder)
                    }
                }
            }
        }
    }

    //TODO DataBuffer: Internal data leak within a DataBuffer object detected!  Be sure to explicitly call release() on all DataBuffer extending objects when you are done with them. (internal object: com.google.android.gms.common.data.DataHolder@2beee47a)
    fun uploadFile(file: File, driveFolder: DriveFolder) {
        val contentResult = Drive.DriveApi.newDriveContents(googleApiClient).await()
        val contentOutputStream = contentResult.driveContents.outputStream
        val channelOut = Channels.newChannel(contentOutputStream)
        val channelIn = file.inputStream().channel
        channelIn.transferTo(0, channelIn.size(), channelOut)
        channelIn.close()
        channelOut.close()
        val createFile = MetadataChangeSet.Builder()
                .setTitle(file.name)
                .setMimeType("image/jpeg")
                .build()
        //TODO: handle responses
        driveFolder.createFile(googleApiClient, createFile, contentResult.driveContents).await()

        driveContent = scanAppRootDirectoryContent()
        uicallback(driveContent)
    }

    fun scanAppRootDirectoryContent(): Map<Metadata, List<Post>> {
        val driveId = appRootDriveId
        if (driveId != null) {
            val result = driveId.asDriveFolder().listChildren(googleApiClient).await()
            return result.metadataBuffer.associateBy({ it }, { listPostFromDriveId(it.driveId) })
        }
        return mutableMapOf()
    }

    fun listPostFromDriveId(driveId: DriveId): List<Post> {
        val result = driveId.asDriveFolder().listChildren(googleApiClient).await()
        return result.metadataBuffer.map { FileManager.postFromName(it.title, null) }
    }

    override fun onConnectionSuspended(p0: Int) {
        throw UnsupportedOperationException("not implemented") //TODO handle this
    }


    fun findOrCreateAppRootFolder() {
        val appDirQuery = Query.Builder().addFilter(Filters.eq(SearchableField.TITLE, appRootDir)).build()

        Drive.DriveApi.getRootFolder(googleApiClient).queryChildren(googleApiClient, appDirQuery).setResultCallback {
            if (it.status.isSuccess) {
                doAsync {
                    if (it.metadataBuffer.count == 1) {
                        appRootDriveId = it.metadataBuffer[0].driveId
                        driveContent = scanAppRootDirectoryContent()
                        uicallback(driveContent)
                    } else if (it.metadataBuffer.count == 0) {
                        //dir not found, create it
                        val changeSet = MetadataChangeSet.Builder().setTitle(appRootDir).build()
                        Drive.DriveApi.getRootFolder(googleApiClient).createFolder(
                                googleApiClient, changeSet).setResultCallback {
                            appRootDriveId = it.driveFolder.driveId
                            driveContent = scanAppRootDirectoryContent()
                            uicallback(driveContent)
                        }
                    }
                }
            } else {
                Log.e("GD", "get app root was not successful")
            }
        }
    }
}