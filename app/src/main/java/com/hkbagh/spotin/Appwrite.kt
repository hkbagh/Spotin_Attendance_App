package com.hkbagh.spotin

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.models.User

const val APPWRITE_PROJECT_ID = "684c2359000d2bf66544"

/**
 * Appwrite project name.
 */
const val APPWRITE_PROJECT_NAME = "Spotin"

/**
 * Appwrite Employer Team ID (from your Appwrite Console).
 * IMPORTANT: Replace with your actual Employer Team ID for granular employer permissions.
 */
const val APPWRITE_EMPLOYER_TEAM_ID = "684d4c84000f5ee1deff"

object Appwrite {
    // Database and Collection IDs for user profiles
    const val APPWRITE_DATABASE_ID = "684d43c6001eb3b4547b"
    const val APPWRITE_COLLECTION_ID = "684ed2330036cd68e666"

    lateinit var client: Client
    lateinit var account: Account
    lateinit var databases: Databases
    lateinit var currentUser: User<Map<String, Any>>

    fun init(context: Context) {
        client = Client(context)
            .setEndpoint("https://cloud.appwrite.io/v1") // Your Appwrite Endpoint
            .setProject(APPWRITE_PROJECT_ID) // Your project ID
            .setSelfSigned(true) // For self-signed certificates, only in development

        account = Account(client)
        databases = Databases(client)
    }
} 