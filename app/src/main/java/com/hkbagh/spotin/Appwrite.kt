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
const val APPWRITE_EMPLOYER_TEAM_ID = "YOUR_EMPLOYER_TEAM_ID"

object Appwrite {
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