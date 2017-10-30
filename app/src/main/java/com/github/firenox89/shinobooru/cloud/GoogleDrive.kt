package com.github.firenox89.shinobooru.cloud

import android.os.Bundle
import android.util.Log
import com.github.firenox89.shinobooru.utility.FileManager
import com.github.firenox89.shinobooru.model.DownloadedPost
import com.github.firenox89.shinobooru.model.Post
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.drive.*
import com.google.android.gms.drive.query.Filters
import com.google.android.gms.drive.query.Query
import com.google.android.gms.drive.query.SearchableField
import java.io.File
import java.lang.IllegalStateException
import java.nio.channels.Channels
import java.util.concurrent.TimeUnit


/**
 * Created by firenox on 02.11.16.
 */
class GoogleDrive : GoogleApiClient.ConnectionCallbacks {

    companion object {
        var googleApiClient: GoogleApiClient? = null
        val TAG = "GoogleDrive"
        val appRootDir = "Shinobooru"
        var rootDirDriveID: DriveId? = null
        var appRootContent: Map<Metadata, List<DownloadedPost>>? = null
    }

    init {
        googleApiClient?.registerConnectionCallbacks(this)
    }

    override fun onConnected(p0: Bundle?) {
        Log.e(TAG, "CONNECTED $p0")
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.e(TAG, "SUSPENDED $p0")
    }

    fun sync(updateUI: (Map<Metadata, List<DownloadedPost>>?) -> Unit) {
        Log.e(TAG, "sync")
        val driveContent = scanAppRootDirectoryContent()
        driveContent.forEach { Log.e(TAG, it.key.title) }

        FileManager.boards.forEach {
            val name = it.key
            val postList = driveContent.filter { it.key.title == name }.toList()
            if (postList.isNotEmpty()) {
                val fileList = it.value
                val driveList = postList[0].second.map(DownloadedPost::id)
                val unsyncedFiles = fileList.filter { !driveList.contains(it.id) }
                unsyncedFiles.forEach { uploadFile(it.file, postList[0].first.driveId.asDriveFolder()) }
            } else {
                val changeSet = MetadataChangeSet.Builder().setTitle(it.key).build()
                val result = findOrCreateAppRootFolder().asDriveFolder().createFolder(googleApiClient, changeSet).await(3000, TimeUnit.MILLISECONDS)
                it.value.forEach {
                    val file = it.file
                    uploadFile(file, result.driveFolder)
                    updateUI(scanAppRootDirectoryContent(true))
                }
            }
        }
    }

    fun findDoubleID() {
        FileManager.boards.forEach {
            val set: MutableSet<Long> = mutableSetOf()
            val list: MutableList<Post> = mutableListOf()
            it.value.forEach {
                if (!set.add(it.id)) {
                    list.add(it)
                }
            }
            val board = it.key
            list.forEach {
                val id = it.id
                val dlist = FileManager.boards[board]?.filter { it.id == id }!!
                Log.e(TAG, "delete")
                if (dlist[0].file_size != dlist[1].file_size) {
                    if (dlist[0].file_size > dlist[1].file_size) {
                        FileManager.deleteDownloadedPost(dlist[1])
                        Log.e(TAG, "${dlist[1]}")
                    } else {
                        FileManager.deleteDownloadedPost(dlist[0])
                        Log.e(TAG, "${dlist[0]}")
                    }
                } else if (dlist[0].tags != dlist[1].tags) {
                    if (dlist[0].file.lastModified() > dlist[1].file.lastModified()) {
                        FileManager.deleteDownloadedPost(dlist[1])
                        Log.e(TAG, "${dlist[1]}")
                    } else {
                        FileManager.deleteDownloadedPost(dlist[0])
                        Log.e(TAG, "${dlist[0]}")
                    }
                } else {
                    Log.e(TAG, "Wut?")
                }
            }
        }
    }

    fun getPostOnlyOnDevice(board: String): List<DownloadedPost>? {
        val driveContent = scanAppRootDirectoryContent()
        val driveList = driveContent.filter { it.key.title == board }.map { it.value }.first()
        val deviceList = FileManager.boards[board]

        val drivePostIdList = driveList.map(DownloadedPost::id)

        return deviceList?.filter { !drivePostIdList.contains(it.id) }
    }

    fun getPostOnlyOnDrive(board: String): List<DownloadedPost>? {
        val driveContent = scanAppRootDirectoryContent()
        val driveList = driveContent.filter { it.key.title == board }.map { it.value }.first()
        val deviceList = FileManager.boards[board]

        val devicePostIdList = deviceList?.map(DownloadedPost::id) ?: emptyList()

        return driveList.filter { !devicePostIdList.contains(it.id) }
    }

    fun collectData(updateUI: (Map<Metadata, List<DownloadedPost>>?) -> Unit) {
        val res = scanAppRootDirectoryContent()
        updateUI(res)
    }

    private fun uploadFile(file: File, driveFolder: DriveFolder) {
        Log.e(TAG, "upload ${file.name}")
        val contentResult = Drive.DriveApi.newDriveContents(googleApiClient).await(3000, TimeUnit.MILLISECONDS)
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
        connectGoogleAPI()
        val res = driveFolder.createFile(googleApiClient, createFile, contentResult.driveContents).await(5000, TimeUnit.MILLISECONDS)
        if (!res.status.isSuccess) {
            throw IllegalStateException("File upload failed for ${file.absoluteFile}")
        }
    }

    private fun scanAppRootDirectoryContent(reload: Boolean = false): Map<Metadata, List<DownloadedPost>> {
        val content = appRootContent
        if (content != null && !reload) {
            return content
        }
        connectGoogleAPI()
        val driveResult = execWithRetries { findOrCreateAppRootFolder().asDriveFolder().listChildren(googleApiClient).await(3000, TimeUnit.MILLISECONDS) }
        val result = driveResult.metadataBuffer.associateBy({ it }, { listDownloadedPostFromDriveId(it.driveId) })
        appRootContent = result
        return result
    }

    private fun listDownloadedPostFromDriveId(driveId: DriveId): List<DownloadedPost> {
        connectGoogleAPI()
        val result = execWithRetries { driveId.asDriveFolder().listChildren(googleApiClient).await(3000, TimeUnit.MILLISECONDS) }
        //create a non existing file is nicer then passing null
        return result.metadataBuffer.map { FileManager.postFromName(it.title, File(it.title)) }
    }

    private fun findOrCreateAppRootFolder(reload: Boolean = false): DriveId {
        val id = rootDirDriveID
        if (id != null && !reload) {
            return id
        }
        val appDirQuery = Query.Builder().addFilter(Filters.eq(SearchableField.TITLE, appRootDir)).build()
        connectGoogleAPI()
        val metadataBufferResult = execWithRetries { Drive.DriveApi.getRootFolder(googleApiClient).queryChildren(googleApiClient, appDirQuery).await(3000, TimeUnit.MILLISECONDS) }
        if (metadataBufferResult.status.isSuccess) {
            when {
                metadataBufferResult.metadataBuffer.count == 0 -> {
                    //dir not found, create it
                    val changeSet = MetadataChangeSet.Builder().setTitle(appRootDir).build()
                    val createdFolder = Drive.DriveApi.getRootFolder(googleApiClient)
                            .createFolder(googleApiClient, changeSet).await()
                    rootDirDriveID = createdFolder.driveFolder.driveId
                    return createdFolder.driveFolder.driveId
                }
                metadataBufferResult.metadataBuffer.count == 1 -> {
                    rootDirDriveID = metadataBufferResult.metadataBuffer[0].driveId
                    return metadataBufferResult.metadataBuffer[0].driveId
                }
                metadataBufferResult.metadataBuffer.count > 2 -> {
                    //TODO let the user decide which one is correct

                    //for now just hope that the first one is correct
                    rootDirDriveID = metadataBufferResult.metadataBuffer[0].driveId
                    return metadataBufferResult.metadataBuffer[0].driveId
                }
            }
        } else {
            Log.e(TAG, "get app root was not successful")
            Log.e(TAG, "status ${metadataBufferResult.status}")
            Log.e(TAG, "code ${metadataBufferResult.status.statusCode}")
            Log.e(TAG, "msg ${metadataBufferResult.status.statusMessage}")
            Log.e(TAG, "has res ${metadataBufferResult.status.hasResolution()}")
        }
        throw IllegalStateException("Getting root dir failed")
    }

    private fun connectGoogleAPI() {
        val api = googleApiClient
        if (api != null) {
            if (!api.isConnected) {
                api.connect()
            }
        }
    }

    private fun execWithRetries(retries: Int = 5, task: () -> DriveApi.MetadataBufferResult): DriveApi.MetadataBufferResult {
        var count = 0
        var result: DriveApi.MetadataBufferResult? = null
        while (result == null || (count < retries && !result.status.isSuccess)) {
            try {
                connectGoogleAPI()
                Log.e(TAG, "failed ${result?.status} retry. is connected ${googleApiClient?.isConnected}")
                result = task()
                count++
            } catch (ise: IllegalStateException) {
                ise.printStackTrace()
            }
        }
        return result
    }
}