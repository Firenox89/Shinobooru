package com.github.firenox89.shinobooru.cloud

import android.os.Bundle
import android.util.Log
import com.github.firenox89.shinobooru.repo.FileManager
import com.github.firenox89.shinobooru.repo.model.DownloadedPost
import com.github.firenox89.shinobooru.repo.model.Post
import com.github.firenox89.shinobooru.ui.SyncActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.drive.*
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import timber.log.Timber
import java.io.File
import java.nio.channels.Channels


/**
 * Created by firenox on 02.11.16.
 */
class GoogleDrive(val activity: SyncActivity): GoogleApiClient.ConnectionCallbacks {

    companion object {
        var googleApiClient: GoogleApiClient? = null
        val TAG = "GoogleDrive"
        val appRootDir = "Shinobooru"
        fun delete(downloadedPost: DownloadedPost) {

        }
    }

    //TODO should not be mutable from outside this class
    val boards = mutableListOf<Pair<String, MutableList<DownloadedPost>>>()

    init {
        googleApiClient?.registerConnectionCallbacks(this)
    }

    override fun onConnected(p0: Bundle?) {
        Timber.e("CONNECTED $p0")
    }

    override fun onConnectionSuspended(p0: Int) {
        Timber.e("SUSPENDED $p0")
    }

    fun syncDevice2Drive() {
        Timber.e("sync")

        fetchData {
            FileManager.boards.forEach {
                val boardName = it.key
                val boardOnDrive = boards.find { it.first == boardName }
                if (boardOnDrive != null) {
                    val unsyncedFiles = getPostOnlyOnDevice(boardName, boardOnDrive.second)
                    if (unsyncedFiles != null) {
                        Timber.e("unsynced $boardName posts ${unsyncedFiles.count()}")
                        upload(unsyncedFiles, boardName)
                    }
                } else {
                    //TODO create new drive folder
                }
            }
        }
    }

    private fun upload(posts: List<DownloadedPost>, parent: String) {
        Timber.e("upload $parent $posts")
        val mDriveResourceClient = Drive.getDriveResourceClient(activity, GoogleSignIn.getLastSignedInAccount(activity)!!)
        val fetchBoardsTask = fetchBoardsFromDrive(mDriveResourceClient)
        Tasks.whenAll(fetchBoardsTask)
                .continueWith {
                    val boardDriveId = fetchBoardsTask.result?.find { it.title == parent }?.driveId?.asDriveFolder()
                    if (boardDriveId != null) {
                        posts.forEach { post ->
                            val createContentsTask = mDriveResourceClient.createContents()
                            Tasks.whenAll(createContentsTask)
                                    .continueWith { task ->
                                        Timber.e("upload ${post.file.name} start")
                                        val contents = createContentsTask.result
                                        val outputStream = contents?.outputStream

                                        val channelOut = Channels.newChannel(outputStream)
                                        val channelIn = post.file.inputStream().channel
                                        channelIn.transferTo(0, channelIn.size(), channelOut)
                                        channelIn.close()
                                        channelOut.close()
                                        val changeSet = MetadataChangeSet.Builder()
                                                .setTitle(post.file.name)
                                                .setMimeType("image/jpeg")
                                                .build()

                                        mDriveResourceClient.createFile(boardDriveId, changeSet, contents);
                                        Timber.e("upload ${post.file.name} done")
                                        boards.find { it.first == parent }?.second?.add(post)
                                        activity.updateTable()
                                    }
                        }
                    }
                }
    }

    fun getPostOnlyOnDevice(board: String, posts: List<DownloadedPost>): List<DownloadedPost>? {
        val deviceList = FileManager.boards[board]

        val drivePostIdList = posts.map(DownloadedPost::id)

        return deviceList?.filter { !drivePostIdList.contains(it.id) }
    }

    fun getPostOnlyOnDrive(board: String, posts: List<DownloadedPost>): List<DownloadedPost> {
        val deviceList = FileManager.boards[board]

        val devicePostIdList = deviceList?.map(DownloadedPost::id) ?: emptyList()

        return posts.filter { !devicePostIdList.contains(it.id) }
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
                Timber.e("delete")
                if (dlist[0].file_size != dlist[1].file_size) {
                    if (dlist[0].file_size > dlist[1].file_size) {
                        FileManager.deleteDownloadedPost(dlist[1])
                        Timber.e("${dlist[1]}")
                    } else {
                        FileManager.deleteDownloadedPost(dlist[0])
                        Timber.e("${dlist[0]}")
                    }
                } else if (dlist[0].tags != dlist[1].tags) {
                    if (dlist[0].file.lastModified() > dlist[1].file.lastModified()) {
                        FileManager.deleteDownloadedPost(dlist[1])
                        Timber.e("${dlist[1]}")
                    } else {
                        FileManager.deleteDownloadedPost(dlist[0])
                        Timber.e("${dlist[0]}")
                    }
                } else {
                    Timber.e("Wut?")
                }
            }
        }
    }

    fun fetchBoardsFromDrive(mDriveResourceClient: DriveResourceClient): Task<MetadataBuffer> {
        return mDriveResourceClient.rootFolder
                .continueWithTask { mDriveResourceClient.listChildren(it.result!!) }
                .continueWithTask { mDriveResourceClient.listChildren(it.result!![0].driveId.asDriveFolder()) }//Shinobooru dir
    }

    fun fetchData(callback: () -> Unit) {
        Timber.i("fetch data")
        val mDriveResourceClient = Drive.getDriveResourceClient(activity, GoogleSignIn.getLastSignedInAccount(activity)!!)
        fetchBoardsFromDrive(mDriveResourceClient)
                .addOnCompleteListener {
                    Timber.i("app dir")
                    val boardTasks = it.result?.map { Pair(it.title, mDriveResourceClient.listChildren(it.driveId.asDriveFolder())) }
                    Tasks.whenAll(boardTasks?.map { it.second }).continueWith {
                        Timber.i("fetch boards")
                        Timber.i("${boardTasks} boards")
                        boardTasks?.forEach {
                            val board = it.first
                            Timber.i("fetch boards")
                            val posts = it.second.result!!.map { FileManager.postFromName(it.title, File(it.title)) }.toMutableList()
                            Timber.i("fetch boards")
                            boards.add(Pair(board, posts))
                        }
                        callback.invoke()
                    }
                }
    }
}