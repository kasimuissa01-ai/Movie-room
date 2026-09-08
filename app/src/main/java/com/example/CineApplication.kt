package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * CineApplication initializes high-performance Coil image caching and sets up
 * background resources with zero main-thread blocking for instant app startup.
 */
class CineApplication : Application(), ImageLoaderFactory {

    companion object {
        var instance: CineApplication? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Ensure private download directory with .nomedia is initialized early in background
        com.example.data.MovieDownloadManager.ensurePrivateStorage(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("cine_image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}
