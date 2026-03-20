package com.civicfix.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLEncoder

/**
 * TwitterShareManager - Handles sharing civic issue reports to Twitter/X.
 */
class TwitterShareManager {

    companion object {

        private const val TWITTER_PACKAGE = "com.twitter.android"

        /**
         * Build the tweet text in the required format.
         */
        @JvmStatic
        fun buildTweetText(
            issueType: String,
            description: String,
            locationAddress: String,
            latitude: Double?,
            longitude: Double?,
            dateTime: String
        ): String {
            val sb = StringBuilder()
            sb.appendLine("\uD83D\uDEA8 Civic Issue Reported")
            sb.appendLine()
            sb.appendLine("Issue Type: $issueType")
            sb.appendLine()
            sb.appendLine("Description:")
            sb.appendLine(description)
            sb.appendLine()
            sb.appendLine("\uD83D\uDCCD Location:")
            sb.appendLine(locationAddress)
            sb.appendLine()
            if (latitude != null && longitude != null) {
                sb.appendLine("\uD83D\uDDFA\uFE0F Coordinates: $latitude,$longitude")
                sb.appendLine("\uD83D\uDD17 Map: https://maps.google.com/?q=$latitude,$longitude")
                sb.appendLine()
            }
            sb.appendLine("\uD83D\uDD52 Date & Time:")
            sb.appendLine(dateTime)
            sb.appendLine()
            sb.append("#FixCivics #PublicIssue #CityReport")
            return sb.toString()
        }

        /**
         * Copy image to cache and return a FileProvider content:// URI.
         */
        @JvmStatic
        fun getCacheUri(context: Context, originalUri: Uri): Uri {
            val cacheFile = File(context.cacheDir, "twitter_share_${System.currentTimeMillis()}.jpg")
            try {
                context.contentResolver.openInputStream(originalUri)?.use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                return originalUri
            }
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
        }

        /**
         * Share a report to Twitter/X.
         *
         * If the Twitter app is installed, opens the Twitter composer with text + optional image.
         * If the Twitter app is NOT installed, opens the browser-based tweet composer (text only).
         */
        @JvmStatic
        fun shareReportToTwitter(
            context: Context,
            issueType: String,
            description: String,
            locationAddress: String,
            latitude: Double?,
            longitude: Double?,
            dateTime: String,
            imageUri: Uri?
        ) {
            val tweetText = buildTweetText(issueType, description, locationAddress, latitude, longitude, dateTime)

            // Check if Twitter app is installed
            val twitterInstalled: Boolean = try {
                context.packageManager.getPackageInfo(TWITTER_PACKAGE, 0)
                true
            } catch (e: Exception) {
                false
            }

            if (twitterInstalled) {
                // Share via Twitter app
                try {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.putExtra(Intent.EXTRA_TEXT, tweetText)
                    intent.setPackage(TWITTER_PACKAGE)

                    if (imageUri != null) {
                        val contentUri = getCacheUri(context, imageUri)
                        intent.type = "image/*"
                        intent.putExtra(Intent.EXTRA_STREAM, contentUri)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.grantUriPermission(TWITTER_PACKAGE, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        intent.type = "text/plain"
                    }

                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to browser
                    Toast.makeText(context, "Could not open Twitter app, trying browser...", Toast.LENGTH_SHORT).show()
                    try {
                        val encodedText = URLEncoder.encode(tweetText, "UTF-8")
                        val url = "https://twitter.com/intent/tweet?text=$encodedText"
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(browserIntent)
                    } catch (ex: Exception) {
                        Toast.makeText(context, "Could not open browser to share tweet", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                // Open browser-based tweet composer (text only, no image attachment possible)
                try {
                    val encodedText = URLEncoder.encode(tweetText, "UTF-8")
                    val url = "https://twitter.com/intent/tweet?text=$encodedText"
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(browserIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open browser to share tweet", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
