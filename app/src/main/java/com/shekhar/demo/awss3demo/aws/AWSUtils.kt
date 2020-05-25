package com.shekhar.demo.awss3demo.aws

import android.content.Context
import android.net.ParseException
import android.net.Uri
import android.text.TextUtils
import com.amazonaws.HttpMethod
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.ResponseHeaderOverrides
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID.randomUUID


class AWSUtils(private val context: Context, private val filePath: String, val onAwsImageUploadListener: OnAwsImageUploadListener, val filePathKey: String) {

    private var image: File? = null
    private var mTransferUtility: TransferUtility? = null

    private var sS3Client: AmazonS3Client? = null
    private var sCredProvider: CognitoCachingCredentialsProvider? = null

    private fun getCredProvider(context: Context): CognitoCachingCredentialsProvider? {
        if (sCredProvider == null) {
            sCredProvider = CognitoCachingCredentialsProvider(context.applicationContext, AwsConstants.COGNITO_IDENTITY_ID, AwsConstants.COGNITO_REGION)
        }
        return sCredProvider
    }

    private fun getS3Client(context: Context?): AmazonS3Client? {
        if (sS3Client == null) {
            sS3Client = AmazonS3Client(getCredProvider(context!!))
            sS3Client!!.setRegion(Region.getRegion(Regions.AP_SOUTH_1))
        }
        return sS3Client
    }

    private fun getTransferUtility(context: Context): TransferUtility? {
        if (mTransferUtility == null) {
            mTransferUtility = TransferUtility(
                getS3Client(context.applicationContext),
                context.applicationContext
            )
        }
        return mTransferUtility
    }

    fun beginUpload() {

        if (TextUtils.isEmpty(filePath)) {
            onAwsImageUploadListener.onError("Could not find the filepath of the selected file")
            return
        }

        onAwsImageUploadListener.showProgressDialog()
        val file = File(filePath)
        image = file

        try {
            val observer = getTransferUtility(context)?.upload(
                AwsConstants.BUCKET_NAME, //Bucket name
                filePathKey + file.name, image //File name with folder path
            )
            observer?.setTransferListener(UploadListener())
        } catch (e: Exception) {
            e.printStackTrace()
            onAwsImageUploadListener.hideProgressDialog()
        }
    }

    private fun generateS3SignedUrl(path: String?): String? {

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = calendar.time

        val dateFormat: DateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val tomorrowAsString = dateFormat.format(tomorrow)

        val EXPIRY_DATE = tomorrowAsString // maximum 7 days allowed
        val mFile = File(path)

        val s3client: AmazonS3Client? = getS3Client(context)

        val expiration = Date()
        var msec: Long = expiration.time
        msec += 1000 * 6000 * 6000.toLong() // 1 hour.

        val format: DateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val date: Date?
        try {
            date = format.parse(EXPIRY_DATE)
            expiration.time = date.time
        } catch (e: ParseException) {
            e.printStackTrace()
            expiration.time = msec
        }

        val overrideHeader = ResponseHeaderOverrides()
        overrideHeader.contentType = "image/jpeg"
        val mediaUrl: String = mFile.name

        val generatePreSignedUrlRequest = GeneratePresignedUrlRequest(AwsConstants.BUCKET_NAME, filePathKey + mediaUrl)
        generatePreSignedUrlRequest.method = HttpMethod.GET
        generatePreSignedUrlRequest.expiration = expiration
        generatePreSignedUrlRequest.responseHeaders = overrideHeader

        return s3client!!.generatePresignedUrl(generatePreSignedUrlRequest).toString()
    }

    private inner class UploadListener : TransferListener {

        override fun onError(id: Int, e: Exception) {
            onAwsImageUploadListener.hideProgressDialog()
        }

        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
        }

        override fun onStateChanged(id: Int, newState: TransferState) {

            if (newState == TransferState.COMPLETED) {

                onAwsImageUploadListener.hideProgressDialog()

                val finalImageUrl = AwsConstants.S3_URL + filePathKey + image!!.name
                onAwsImageUploadListener.onSuccess(generateS3SignedUrl(finalImageUrl)!!)

//                image!!.delete() //Todo: uncomment this line to delete original file after upload

            } else if (newState == TransferState.CANCELED || newState == TransferState.FAILED) {
                onAwsImageUploadListener.hideProgressDialog()
                onAwsImageUploadListener.onError("Error in uploading file.")
            }
        }
    }

    interface OnAwsImageUploadListener {
        fun showProgressDialog()
        fun hideProgressDialog()
        fun onSuccess(imgUrl: String)
        fun onError(errorMsg: String)
    }
}