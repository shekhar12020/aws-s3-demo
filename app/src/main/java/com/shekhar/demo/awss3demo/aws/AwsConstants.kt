package com.shekhar.demo.awss3demo.aws

import com.amazonaws.regions.Regions

object AwsConstants {

    val COGNITO_IDENTITY_ID: String = "Identity pool ID"
    val COGNITO_REGION: Regions = Regions.AP_SOUTH_1 // Region
    val BUCKET_NAME: String = "Bucket-Name"

    val S3_URL: String = "https://$BUCKET_NAME.s3.ap-south-1.amazonaws.com/"
    val folderPath = "images/"

}