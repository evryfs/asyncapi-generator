package com.example.account.model


/**
 * Identifies an account within an institution.
 *
 * @param [institutionId] Institution that owns the account.
 *
 * @param [accountId] Account identifier within the institution.
 *
 */
data class MyAccountKey(

    val institutionId: String,

    val accountId: String
) {
}
