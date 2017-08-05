package com.github.firenox89.shinobooru.openGL

import android.app.Activity
import android.os.Bundle
import android.opengl.GLSurfaceView
import android.view.MotionEvent


/**
 * Created by firenox on 3/15/17.
 */
class OpenGLViewer: Activity() {
    lateinit private var mGLSurfaceView: GLSurfaceView
    lateinit private var renderer: LessonFourRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGLSurfaceView = GLSurfaceView(this)
        // Request an OpenGL ES 2.0 compatible context.
        mGLSurfaceView.setEGLContextClientVersion(2)

        renderer = LessonFourRenderer(this)
        // Set the renderer to our demo renderer, defined below.
        mGLSurfaceView.setRenderer(renderer)
        setContentView(mGLSurfaceView)
    }

    override fun onResume() {
        // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume()
        mGLSurfaceView.onResume()
    }

    override fun onPause() {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause()
        mGLSurfaceView.onPause()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        renderer.onTouchEvent(event, mGLSurfaceView)
        return super.onTouchEvent(event)
    }
}